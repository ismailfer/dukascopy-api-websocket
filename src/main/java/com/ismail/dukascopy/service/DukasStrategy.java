package com.ismail.dukascopy.service;

import com.dukascopy.api.Filter;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConnectionStatusMessage;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IInstrumentStatusMessage;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.instrument.IFinancialInstrument.Type;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ismail.dukascopy.DukasConstants;
import com.ismail.dukascopy.config.DukasConfig;
import com.ismail.dukascopy.model.DukasSubscription;
import com.ismail.dukascopy.model.OrderBook;
import com.ismail.dukascopy.model.OrderBookEntry;
import com.ismail.dukascopy.model.OrderSide;
import com.ismail.dukascopy.model.OrderType;
import com.ismail.dukascopy.model.TopOfBook;
import com.ismail.dukascopy.util.DukasUtil;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Dukascopy Strategy implementation that sends data to websocket clients
 *
 * @author ismail
 * @since 20220617
 */
@Service
@Slf4j
public class DukasStrategy implements IStrategy {

    private final DukasConfig config;

    private final AtomicReference<IContext> reference = new AtomicReference<>();

    private IContext context;

    private IEngine engine = null;

    private ReentrantLock lock = new ReentrantLock();

    private ArrayList<DukasSubscriber> tobSubscribers = new ArrayList<>();

    private int tobSubscribersSize = 0;

    private ArrayList<DukasSubscriber> orderBookSubscribers = new ArrayList<>();

    private int orderBookSubscribersSize = 0;

    private ObjectMapper jsonMapper = new ObjectMapper();
    // jsonGeminiEventReader = jsonMapper.readerFor(GeminiEvent.class);

    private DukasSubscription dukasSubscription = null;

    private ArrayList<IOrder> orderList = new ArrayList<>();

    private HashMap<String, IOrder> orderByIDMap = new HashMap<>();

    private HashMap<String, IOrder> orderByClientOrderIDMap = new HashMap<>();

    @Autowired
    public DukasStrategy(Clock clock, DukasConfig config) {
        this.config = Objects.requireNonNull(config, "Configuration is required.");
        // this.template = Objects.requireNonNull(template,
        // "SimpMessageSendingOperations is required.");

    }

    @Override
    public void onStart(IContext context) {
        reference.set(context);
        this.context = context;
        engine = context.getEngine();

        log.info(
                "onStart() server time = {}",
                Instant.ofEpochMilli(context.getTime()));

        // DukasSubscription subscription = adjustSubscription(null,
        // persistInstruments(null, null));

        // template.convertAndSend(TOPIC_SUBSCRIPTION, subscription);

        // Pre-subscribe to configured instrument list
        List<String> instrumentIDList = DukasUtil.splitToArrayList(
                config.getSubscriptionInstruments(),
                ',');

        Set<Instrument> instruments = new TreeSet<>();

        for (String instrumentID : instrumentIDList) {
            Instrument instrument = Instrument.valueOf(instrumentID);

            if (instrument != null) {
                instruments.add(instrument);
            } else {
                log.warn("Invalid instrument: " + instrumentID);
            }
        }

        dukasSubscription = adjustSubscription("pre-defined-subscriptions", instruments);

        try {
            // Instrument eurusd = Instrument.EURUSD;
            // IOrder order = submitOrder("ORDER_" + System.currentTimeMillis(), eurusd,
            // OrderSide.Buy, OrderType.Market, 100000.0, 0.0);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO get list of orders for my strategy
        try {
            List<IOrder> myOrderList = engine.getOrders();

            if (myOrderList != null) {
                for (IOrder order : myOrderList) {
                    orderList.add(order);
                    orderByClientOrderIDMap.put(order.getLabel(), order);
                    orderByIDMap.put(order.getId(), order);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        IContext context = reference.getAndSet(null);

        if (context == null) {
            return;
        }

        log.info(
                "Context stopped : server time = {}",
                Instant.ofEpochMilli(context.getTime()));
    }

    @Override
    public void onMessage(IMessage message) {
        if (message instanceof IConnectionStatusMessage) {
            IConnectionStatusMessage m = (IConnectionStatusMessage) message;
        }

        if (message instanceof IInstrumentStatusMessage) {
            IInstrumentStatusMessage m = (IInstrumentStatusMessage) message;
        }
        // log.info("onMessage() " + message);

        // template.convertAndSend(TOPIC_MESSAGE, map);

    }

    @Override
    public void onAccount(IAccount account) {
        // log.info("onAccount() " + account);

        // Map<String, Object> map = convertAccount(account);

        // LOGGER.trace("ACC|{}", map);

        // template.convertAndSend(TOPIC_ACCOUNT, map);

    }

    @Override
    public void onTick(Instrument instrument, ITick tick) {
        // log.info("onTick << " + instrument.getName() + ": " + tick.toString());

        // --------------------------------------------------------------------------------------
        // Send TOB
        // --------------------------------------------------------------------------------------
        if (tobSubscribersSize > 0) {
            TopOfBook st = new TopOfBook();
            st.symbol = instrument.getName().replace("/", "").replace(".", "");
            st.bid = tick.getBid();
            st.ask = tick.getAsk();
            st.askQty = tick.getAskVolume();
            st.bidQty = tick.getBidVolume();

            if (instrument.getType() == Type.FOREX) {
                st.bidQty *= 100000.0;
                st.askQty *= 100000.0;
            }

            int depthLevels = (tick.getBids() == null || tick.getAsks() == null)
                    ? 0
                    : Math.min(tick.getBids().length, tick.getAsks().length);

            st.depthLevels = depthLevels;

            st.updateTime = System.currentTimeMillis();

            st.spread = st.ask - st.bid;
            st.spreadBps = 10000.0 * (st.spread) / st.ask;

            st.last = (st.ask + st.bid) / 2.0;

            st.live = true;

            try {
                String json = jsonMapper.writeValueAsString(st);

                lock.lock();
                try {
                    for (DukasSubscriber ws : tobSubscribers) {
                        ws.sendMessage(json);
                    }
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                log.error("onTick() error: " + e.getMessage(), e);
            }
        }

        // --------------------------------------------------------------------------------------
        // Send OrderBook
        // --------------------------------------------------------------------------------------
        if (orderBookSubscribersSize > 0) {
            OrderBook st = new OrderBook();
            st.symbol = instrument.getName().replace("/", "").replace(".", "");
            st.bid = tick.getBid();
            st.bidQty = tick.getBidVolume();
            st.ask = tick.getAsk();
            st.askQty = tick.getAskVolume();

            if (instrument.getType() == Type.FOREX) {
                st.bidQty *= 100000.0;
                st.askQty *= 100000.0;
            }

            int depthLevels = (tick.getBids() == null || tick.getAsks() == null)
                    ? 0
                    : Math.min(tick.getBids().length, tick.getAsks().length);

            st.depthLevels = depthLevels;

            st.updateTime = System.currentTimeMillis();

            st.spread = st.ask - st.bid;
            st.spreadBps = 10000.0 * (st.spread) / st.ask;

            st.last = (st.ask + st.bid) / 2.0;

            st.live = true;

            if (depthLevels > 0) {
                st.bids = new ArrayList<>();
                st.asks = new ArrayList<>();

                for (int i = 0; i < depthLevels; i++) {
                    OrderBookEntry bidEntry = new OrderBookEntry(
                            tick.getBidVolumes()[i],
                            tick.getBids()[i]);
                    st.bids.add(bidEntry);

                    OrderBookEntry askEntry = new OrderBookEntry(
                            tick.getAskVolumes()[i],
                            tick.getAsks()[i]);
                    st.asks.add(askEntry);

                    if (instrument.getType() == Type.FOREX) {
                        bidEntry.quantity *= 100000.0;
                        askEntry.quantity *= 100000.0;
                    }
                }
            }

            try {
                String json = jsonMapper.writeValueAsString(st);

                lock.lock();
                try {
                    for (DukasSubscriber ws : orderBookSubscribers) {
                        ws.sendMessage(json);
                    }
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                log.error("onTick() error: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onBar(
            Instrument instrument,
            Period period,
            IBar askBar,
            IBar bidBar) {
        log.trace("onBar() " + instrument.getName() + ": " + askBar);
    }

    /**
     * @return
     * @throws Exception
     */
    public List<IOrder> getPositions()
            throws Exception {
        if (context == null)
            throw new RuntimeException(
                    "Strategy context not initialized yet");
        return engine.getOrders();
    }

    /**
     * @param clientOrderID
     * @param dukasOrderID
     * @return
     * @throws Exception
     */
    public IOrder getPosition(String clientOrderID, String dukasOrderID)
            throws Exception {
        if (context == null)
            throw new RuntimeException(
                    "Strategy context not initialized yet");

        // Lookup the order

        IOrder order = null;

        // Give priority to DukasOrderID

        if (DukasUtil.isDefined(dukasOrderID)) {
            order = engine.getOrderById(dukasOrderID);

            if (order == null)
                throw new RuntimeException(
                        "Invalid DukasOrderID " + dukasOrderID);
        }
        // Then ClientOrderID
        else if (DukasUtil.isDefined(clientOrderID)) {
            order = engine.getOrder(clientOrderID);

            if (order == null)
                throw new RuntimeException(
                        "Invalid ClientOrderID " + clientOrderID);
        } else {
            throw new RuntimeException(
                    "Either DukasOrderID or ClientOrderID are required");
        }

        return order;
    }

    /**
     * https://www.dukascopy.com/wiki/en/development/strategy-api/orders-and-positions/overview-orders-and-positions
     *
     * @param clientOrderID
     * @param instrument
     * @param side
     * @param orderType
     * @param quantity
     * @param price
     * @return
     * @throws Exception
     */
    public IOrder openPosition(
            String clientOrderID,
            Instrument instrument,
            OrderSide orderSide,
            OrderType orderType,
            double quantity,
            double price,
            double slippage,
            long timeout)
            throws Exception {
        if (context == null)
            throw new RuntimeException(
                    "Strategy context not initialized yet");

        // convert OrderSide and orderType to OrderCommand
        OrderCommand cmd = null;

        if (orderSide == OrderSide.Buy) {
            if (orderType == OrderType.Market) {
                cmd = OrderCommand.BUY;
            } else if (orderType == OrderType.Limit) {
                cmd = OrderCommand.BUY;
            } else if (orderType == OrderType.Stop) {
                cmd = OrderCommand.BUYSTOP;
            } else {
                throw new IllegalArgumentException("Invalid OrderType: " + orderType);
            }
        } else {
            if (orderType == OrderType.Market) {
                cmd = OrderCommand.SELL;
            } else if (orderType == OrderType.Limit) {
                cmd = OrderCommand.SELL;
            } else if (orderType == OrderType.Stop) {
                cmd = OrderCommand.SELLSTOP;
            } else {
                throw new IllegalArgumentException("Invalid OrderType: " + orderType);
            }
        }

        // Default slippage; 10 pips
        if (slippage <= 0.0)
            slippage = 10;

        // Normalize quantity to Dukascopy quantity (in lots of 100,000)
        double quantityNormalized = quantity / DukasConstants.lotSize;

        // We have to submit the order in the same thread as the Context; using Reactive
        // Programming
        // So we submit a task; then wait for it to get done
        OpenPositionTask task = new OpenPositionTask(
                clientOrderID,
                instrument,
                cmd,
                quantityNormalized,
                price,
                slippage);
        context.executeTask(task);

        // we have to wait for a given timeout
        long sleepTime = 100;
        int iterations = (int) (timeout / sleepTime);

        for (int i = 0; i < iterations; i++) {
            // sleep for a minimal amount at a time; so we can get the result quick when the
            // task is done
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
            }

            if (task.taskDone) {
                if (task.rejectReason != null) {
                    throw new RuntimeException(task.rejectReason);
                } else {
                    return task.order;
                }
            }
        }

        throw new RuntimeException("Timeout submitting order");
        // return null;
    }

    public class OpenPositionTask implements Callable<IOrder> {

        private String clientOrderID;

        private final Instrument instrument;

        private OrderCommand cmd;

        private double quantity;

        private double price;

        private double slippage = 5;

        // result

        public IOrder order = null;

        public Throwable error = null;

        public String rejectReason = null;

        public boolean taskDone = false;

        public OpenPositionTask(
                String clientOrderID,
                Instrument instrument,
                OrderCommand cmd,
                double quantity,
                double price,
                double slippage) {
            this.clientOrderID = clientOrderID;
            this.instrument = instrument;
            this.cmd = cmd;
            this.quantity = quantity;
            this.price = price;
            this.slippage = slippage;
        }

        public IOrder call() throws Exception {
            try {

                order = engine.submitOrder(
                        clientOrderID,
                        instrument,
                        cmd,
                        quantity,
                        price,
                        slippage);

                log.info("order submitted " + order);

                taskDone = true;

                return order;
            } catch (Throwable e) {

                if (e.getMessage().contains("Label not unique")) {
                    rejectReason = "Duplicate ClientOrderID: " + clientOrderID;
                } else {
                    rejectReason = e.getMessage();
                }

                taskDone = true;

                e.printStackTrace();

                return null;
            }
        }
    }

    /**
     * https://www.dukascopy.com/wiki/en/development/strategy-api/orders-and-positions/take-profit
     *
     * @param clientOrderID
     * @param takeProfitPips
     * @param stopLossPips
     * @return
     * @throws Exception
     */
    public IOrder editPosition(
            Optional<String> clientOrderID,
            Optional<String> dukasOrderID,
            double takeProfitPips,
            double stopLossPips,
            long timeout)
            throws Exception {
        if (context == null)
            throw new RuntimeException(
                    "Strategy context not initialized yet");

        IOrder order = null;

        // Give priority to DukasOrderID
        String orderId = null;
        if (dukasOrderID.isPresent()) {
            orderId = dukasOrderID.get();
            order = engine.getOrderById(orderId);

            if (order == null)
                throw new RuntimeException(
                        "Invalid DukasOrderID " + dukasOrderID);
        }
        // Then ClientOrderID
        else if (clientOrderID.isPresent()) {
            orderId = clientOrderID.get();

            order = engine.getOrder(orderId);

            if (order == null)
                throw new RuntimeException(
                        "Invalid ClientOrderID " + clientOrderID);
        } else {
            throw new RuntimeException(
                    "Either DukasOrderID or ClientOrderID are required");
        }

        // We have to submit the order in the same thread as the Context; using Reactive
        // Programming
        // So we submit a task; then wait for it to get done
        EditPositionTask task = new EditPositionTask(order, takeProfitPips, stopLossPips);
        context.executeTask(task);

        // we have to wait for a given timeout
        long sleepTime = 100;
        int iterations = (int) (timeout / sleepTime);

        for (int i = 0; i < iterations; i++) {
            // sleep for a minimal amount at a time; so we can get the result quick when the
            // task is done
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
            }

            if (task.taskDone) {
                if (task.rejectReason != null) {
                    log.info("rejectReason: " + task.rejectReason);
                    throw new RuntimeException(task.rejectReason);
                } else {
                    return task.order;
                }
            }
        }

        // throw new RuntimeException("Timeout editing order");
        return null;
    }

    public class EditPositionTask implements Callable<IOrder> {

        public IOrder order = null;

        private double takeProfitPips;

        private double stopLossPips;

        // result

        public Throwable error = null;

        public String rejectReason = null;

        public boolean taskDone = false;

        public EditPositionTask(
                IOrder order,
                double takeProfitPips,
                double stopLossPips) {
            this.order = order;
            this.takeProfitPips = takeProfitPips;
            this.stopLossPips = stopLossPips;
        }

        public IOrder call() throws Exception {

            try {
                Instrument instrument = order.getInstrument();
                if (order.isLong()) {
                    if (takeProfitPips > 0L) {

                        log.info("takeProfitPips: " + takeProfitPips);
                        double entryPrice = order.getOpenPrice();
                        double takeProfitPrice = entryPrice + (takeProfitPips * instrument.getPipValue());
                        log.info("takeProfitPrice: " + takeProfitPrice);
                        order.setTakeProfitPrice(takeProfitPrice);
                    }

                    if (stopLossPips > 0L) {
                        log.info("stopLossPips: " + stopLossPips);
                        double entryPrice = order.getOpenPrice();
                        double stopLossPrice = entryPrice - (stopLossPips * instrument.getPipValue());
                        log.info("stopLossPrice: " + stopLossPrice);
                        order.setStopLossPrice(stopLossPrice);
                    }
                }

                if (order.isLong() == false) {
                    if (takeProfitPips > 0L) {

                        log.info("takeProfitPips: " + takeProfitPips);
                        double entryPrice = order.getOpenPrice();
                        double takeProfitPrice = entryPrice - (takeProfitPips * instrument.getPipValue());
                        log.info("takeProfitPrice: " + takeProfitPrice);
                        order.setTakeProfitPrice(takeProfitPrice);
                    }

                    if (stopLossPips > 0L) {
                        log.info("stopLossPips: " + stopLossPips);
                        double entryPrice = order.getOpenPrice();
                        double stopLossPrice = entryPrice + (stopLossPips * instrument.getPipValue());
                        log.info("stopLossPrice: " + stopLossPrice);
                        order.setStopLossPrice(stopLossPrice);
                    }
                }

                log.info("order edited " + order);

                taskDone = true;

                return order;
            } catch (Throwable e) {

                rejectReason = e.getMessage();
                log.info("rejectReason" + rejectReason);
                taskDone = true;

                e.printStackTrace();

                return null;
            }
        }
    }

    /**
     * https://www.dukascopy.com/wiki/en/development/strategy-api/orders-and-positions/close-orders
     *
     * @param clientOrderID
     * @return
     * @throws Exception
     */
    public IOrder closePosition(
            Optional<String> clientOrderID,
            Optional<String> dukasOrderID,
            double quantity,
            double price,
            double slippage,
            long timeout)
            throws Exception {
        if (context == null)
            throw new RuntimeException(
                    "Strategy context not initialized yet");

        // Lookup the order

        IOrder order = null;

        // Give priority to DukasOrderID
        String orderId = null;
        if (dukasOrderID.isPresent()) {
            orderId = dukasOrderID.get();
            order = engine.getOrderById(orderId);

            if (order == null)
                throw new RuntimeException(
                        "Invalid DukasOrderID " + dukasOrderID);
        }
        // Then ClientOrderID
        else if (clientOrderID.isPresent()) {
            orderId = clientOrderID.get();

            order = engine.getOrder(orderId);

            if (order == null)
                throw new RuntimeException(
                        "Invalid ClientOrderID " + clientOrderID);
        } else {
            throw new RuntimeException(
                    "Either DukasOrderID or ClientOrderID are required");
        }

        if (order.getState() != IOrder.State.FILLED)
            throw new RuntimeException(
                    "Cannot close order in current state: " + order.getState());

        // Default slippage; 10 pips
        if (slippage < 0.0)
            slippage = 10;

        // Normalize quantity to Dukascopy quantity (in lots of 100,000)
        double quantityNormalized = quantity / DukasConstants.lotSize;

        // We have to submit the order in the same thread as the Context; using Reactive
        // Programming
        // So we submit a task; then wait for it to get done
        ClosePositionTask task = new ClosePositionTask(
                order,
                quantityNormalized,
                price,
                slippage);
        context.executeTask(task);

        // we have to wait for a given timeout
        long sleepTime = 100;
        int iterations = (int) (timeout / sleepTime);

        for (int i = 0; i < iterations; i++) {
            // sleep for a minimal amount at a time; so we can get the result quick when the
            // task is done
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
            }

            if (task.taskDone) {
                if (task.rejectReason != null) {
                    throw new RuntimeException(task.rejectReason);
                } else {
                    return task.order;
                }
            }
        }

        return null;
    }

    public class ClosePositionTask implements Callable<IOrder> {

        private IOrder order;

        private double quantity;

        private double price;

        private double slippage;

        public boolean taskDone = false;

        public Throwable error = null;

        public String rejectReason = null;

        public ClosePositionTask(
                IOrder order,
                double quantity,
                double price,
                double slippage) {
            this.order = order;
            this.quantity = quantity;
            this.price = price;
            this.slippage = slippage;
        }

        @Override
        public IOrder call() throws Exception {
            try {
                if (order.getState() == IOrder.State.FILLED) {
                    log.info("quantity: " + quantity);
                    log.info("price: " + price);
                    log.info("slippage: " + slippage);
                    if (quantity == 0.0 && price == 0.0 && slippage == 0.0) {
                        order.close();
                    } else if (quantity > 0.0 && price == 0.0 && slippage == 0.0) {
                        order.close(quantity);
                    } else if (quantity > 0.0 && price > 0.0 && slippage == 0.0) {
                        order.close(quantity, price);
                    } else if (quantity > 0.0 && price > 0.0 && slippage > 0.0) {
                        order.close(quantity, price, slippage);
                    }
                } else {
                    rejectReason = "Invalid order state: " + order.getState();
                }

                taskDone = true;
            } catch (Throwable e) {
                error = e;
                rejectReason = e.getMessage();

                taskDone = true;

                e.printStackTrace();
            }

            return order;
        }
    }

    /**
     * Gets historical data
     *
     * @param instrument
     * @param period
     * @param pOfferSide
     * @param timeFrom
     * @param timeTo
     * @return
     * @throws Exception
     */
    public List<IBar> getHistData(
            Instrument instrument,
            Period period,
            OfferSide pOfferSide,
            long timeFrom,
            long timeTo)
            throws Exception {
        if (instrument == null || period == null) {
            return null;
        }

        IContext context = reference.get();

        if (context == null) {
            return null;
        }

        List<IBar> bars = context
                .getHistory()
                .getBars(instrument, period, pOfferSide, Filter.WEEKENDS, timeFrom, timeTo);

        return bars;
    }

    public DukasSubscription adjustSubscription(
            String id,
            Set<Instrument> instruments) {
        DukasSubscription subscription = new DukasSubscription();
        subscription.id = id;
        subscription.time = System.currentTimeMillis();
        subscription.instruments = instruments;

        IContext context = reference.get();

        if (context != null) {
            Set<Instrument> current = context.getSubscribedInstruments();

            Set<Instrument> excessive = new TreeSet<>();
            Set<Instrument> lacking = new TreeSet<>();

            for (Instrument inst : current) {
                if (instruments.contains(inst) == false)
                    excessive.add(inst);
            }

            for (Instrument inst : instruments) {
                if (current.contains(inst) == false)
                    lacking.add(inst);
            }

            if (CollectionUtils.isNotEmpty(excessive)) {
                excessive.forEach(i -> log.info("Unsubscribing : {} - {}", id, i));

                context.unsubscribeInstruments(new HashSet<>(excessive));
            }

            if (CollectionUtils.isNotEmpty(lacking)) {
                lacking.forEach(i -> log.info("Subscribing : {} - {}", id, i));

                context.setSubscribedInstruments(new HashSet<>(lacking), false);
            }

            subscription.success = true;

            log.info("Adjusted subscription : {}", subscription);
        } else {
            subscription.success = false;

            log.info("Skipped subscription : {}", subscription);
        }

        return subscription;
    }

    public void subscribeToTOB(DukasSubscriber ws) {
        lock.lock();
        try {
            tobSubscribers.add(ws);
            tobSubscribersSize = tobSubscribers.size();
        } finally {
            lock.unlock();
        }
    }

    public void unsubscribeFromTOB(DukasSubscriber ws) {
        lock.lock();
        try {
            tobSubscribers.remove(ws);
            tobSubscribersSize = tobSubscribers.size();
        } finally {
            lock.unlock();
        }
    }

    public void subscribeToOrderBook(DukasSubscriber ws) {
        lock.lock();
        try {
            orderBookSubscribers.add(ws);
            orderBookSubscribersSize = orderBookSubscribers.size();
        } finally {
            lock.unlock();
        }
    }

    public void unsubscribeFromOrderBook(DukasSubscriber ws) {
        lock.lock();
        try {
            orderBookSubscribers.remove(ws);
            orderBookSubscribersSize = orderBookSubscribers.size();
        } finally {
            lock.unlock();
        }
    }
}
