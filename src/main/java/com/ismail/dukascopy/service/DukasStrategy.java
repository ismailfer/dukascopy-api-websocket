package com.ismail.dukascopy.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConnectionStatusMessage;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IInstrumentStatusMessage;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ismail.dukascopy.config.DukasConfig;
import com.ismail.dukascopy.model.Candle;
import com.ismail.dukascopy.model.DukasSubscription;
import com.ismail.dukascopy.model.OrderBook;
import com.ismail.dukascopy.model.OrderBookEntry;
import com.ismail.dukascopy.model.TopOfBook;
import com.ismail.dukascopy.util.DukasUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Dukascopy Strategy implementation that sends data to websocket clients
 * 
 * @author ismail
 * @since 20220617
 */
@Service
@Slf4j
public class DukasStrategy implements IStrategy
{

    private final Clock clock;

    private final DukasConfig config;

    private final AtomicReference<IContext> reference = new AtomicReference<>();

    private ReentrantLock lock = new ReentrantLock();

    private ArrayList<DukasSubscriber> tobSubscribers = new ArrayList<>();

    private int tobSubscribersSize = 0;

    private ArrayList<DukasSubscriber> orderBookSubscribers = new ArrayList<>();

    private int orderBookSubscribersSize = 0;

    private ObjectMapper jsonMapper = new ObjectMapper();
    // jsonGeminiEventReader = jsonMapper.readerFor(GeminiEvent.class);

    private DukasSubscription dukasSubscription = null;

    @Autowired
    public DukasStrategy(Clock clock, DukasConfig config)
    {

        this.clock = Objects.requireNonNull(clock, "Clock is required.");

        this.config = Objects.requireNonNull(config, "Configuration is required.");

        // this.template = Objects.requireNonNull(template, "SimpMessageSendingOperations is required.");

    }

    @Override
    public void onStart(IContext context)
    {
        reference.set(context);

        log.info("onStart() server time = {}", Instant.ofEpochMilli(context.getTime()));

        // DukasSubscription subscription = adjustSubscription(null, persistInstruments(null, null));

        //template.convertAndSend(TOPIC_SUBSCRIPTION, subscription);

        // Pre-subscribe to configured instrument list
        List<String> instrumentIDList = DukasUtil.splitToArrayList(config.getSubscriptionInstruments(), ',');

        Set<Instrument> instruments = new TreeSet<>();

        for (String instrumentID : instrumentIDList)
        {
            Instrument instrument = Instrument.valueOf(instrumentID);

            if (instrument != null)
            {
                instruments.add(instrument);
            }
            else
            {
                log.warn("Invalid instrument: " + instrumentID);
            }
        }

        dukasSubscription = adjustSubscription("pre-defined-subscriptions", instruments);

    }

    @Override
    public void onStop()
    {

        IContext context = reference.getAndSet(null);

        if (context == null)
        {
            return;
        }

        log.info("Context stopped : server time = {}", Instant.ofEpochMilli(context.getTime()));

    }

    @Override
    public void onMessage(IMessage message)
    {

        if (message instanceof IConnectionStatusMessage)
        {
            IConnectionStatusMessage m = (IConnectionStatusMessage) message;
        }

        if (message instanceof IInstrumentStatusMessage)
        {
            IInstrumentStatusMessage m = (IInstrumentStatusMessage) message;

        }

        log.info("onMessage() " + message);

        //  template.convertAndSend(TOPIC_MESSAGE, map);

    }

    @Override
    public void onAccount(IAccount account)
    {
        log.info("onAccount() " + account);

        // Map<String, Object> map = convertAccount(account);

        // LOGGER.trace("ACC|{}", map);

        // template.convertAndSend(TOPIC_ACCOUNT, map);

    }

    @Override
    public void onTick(Instrument instrument, ITick tick)
    {
        // log.info("onTick << " + instrument.getName() + ": " + tick.toString());

        // --------------------------------------------------------------------------------------
        // Send TOB
        // --------------------------------------------------------------------------------------
        if (tobSubscribersSize > 0)
        {
            TopOfBook st = new TopOfBook();
            st.symbol = instrument.getName();
            st.bid = tick.getBid();
            st.bidQty = tick.getBidVolume();
            st.ask = tick.getAsk();
            st.askQty = tick.getAskVolume();

            int depthLevels = (tick.getBids() == null || tick.getAsks() == null) ? 0 : Math.min(tick.getBids().length, tick.getAsks().length);

            st.depthLevels = depthLevels;

            st.updateTime = System.currentTimeMillis();

            st.spread = st.ask - st.bid;
            st.spreadBps = 10000.0 * (st.spread) / st.ask;

            st.last = (st.ask + st.bid) / 2.0;

            st.live = true;

            try
            {
                String json = jsonMapper.writeValueAsString(st);

                lock.lock();
                try
                {
                    for (DukasSubscriber ws : tobSubscribers)
                    {
                        ws.sendMessage(json);
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }
            catch (Exception e)
            {
                log.error("onTick() error: " + e.getMessage(), e);
            }
        }

        // --------------------------------------------------------------------------------------
        // Send OrderBook
        // --------------------------------------------------------------------------------------
        if (orderBookSubscribersSize > 0)
        {
            OrderBook st = new OrderBook();
            st.symbol = instrument.getName();
            st.bid = tick.getBid();
            st.bidQty = tick.getBidVolume();
            st.ask = tick.getAsk();
            st.askQty = tick.getAskVolume();

            int depthLevels = (tick.getBids() == null || tick.getAsks() == null) ? 0 : Math.min(tick.getBids().length, tick.getAsks().length);

            st.depthLevels = depthLevels;

            st.updateTime = System.currentTimeMillis();

            st.spread = st.ask - st.bid;
            st.spreadBps = 10000.0 * (st.spread) / st.ask;

            st.last = (st.ask + st.bid) / 2.0;

            st.live = true;

            if (depthLevels > 0)
            {
                st.bids = new ArrayList<>();
                st.asks = new ArrayList<>();

                for (int i = 0; i < depthLevels; i++)
                {
                    OrderBookEntry bidEntry = new OrderBookEntry(tick.getBidVolumes()[i], tick.getBids()[i]);
                    st.bids.add(bidEntry);

                    OrderBookEntry askEntry = new OrderBookEntry(tick.getAskVolumes()[i], tick.getAsks()[i]);
                    st.asks.add(askEntry);
                }
            }

            try
            {
                String json = jsonMapper.writeValueAsString(st);

                lock.lock();
                try
                {
                    for (DukasSubscriber ws : orderBookSubscribers)
                    {
                        ws.sendMessage(json);
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }
            catch (Exception e)
            {
                log.error("onTick() error: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar)
    {
        log.trace("onBar() " + instrument.getName() + ": " + askBar);
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
    public List<IBar> getHistData(Instrument instrument, Period period, OfferSide pOfferSide, long timeFrom, long timeTo) throws Exception
    {
      
        if (instrument == null || period == null)
        {
            return null;
        }

        IContext context = reference.get();

        if (context == null)
        {
            return null;
        }

        List<IBar> bars = context.getHistory().getBars(instrument, period, pOfferSide, timeFrom, timeTo);

        return bars;
        
    }

    public DukasSubscription adjustSubscription(String id, Set<Instrument> instruments)
    {
        DukasSubscription subscription = new DukasSubscription();
        subscription.id = id;
        subscription.time = System.currentTimeMillis();
        subscription.instruments = instruments;

        IContext context = reference.get();

        if (context != null)
        {
            Set<Instrument> current = context.getSubscribedInstruments();

            Set<Instrument> excessive = new TreeSet<>();
            Set<Instrument> lacking = new TreeSet<>();

            for (Instrument inst : current)
            {
                if (instruments.contains(inst) == false)
                    excessive.add(inst);
            }

            for (Instrument inst : instruments)
            {
                if (current.contains(inst) == false)
                    lacking.add(inst);
            }

            if (CollectionUtils.isNotEmpty(excessive))
            {
                excessive.forEach(i -> log.info("Unsubscribing : {} - {}", id, i));

                context.unsubscribeInstruments(new HashSet<>(excessive));
            }

            if (CollectionUtils.isNotEmpty(lacking))
            {
                lacking.forEach(i -> log.info("Subscribing : {} - {}", id, i));

                context.setSubscribedInstruments(new HashSet<>(lacking), false);
            }

            subscription.success = true;

            log.info("Adjusted subscription : {}", subscription);

        }
        else
        {

            subscription.success = false;

            log.info("Skipped subscription : {}", subscription);

        }

        return subscription;

    }

    public void subscribeToTOB(DukasSubscriber ws)
    {
        lock.lock();
        try
        {
            tobSubscribers.add(ws);
            tobSubscribersSize = tobSubscribers.size();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void unsubscribeFromTOB(DukasSubscriber ws)
    {
        lock.lock();
        try
        {
            tobSubscribers.remove(ws);
            tobSubscribersSize = tobSubscribers.size();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void subscribeToOrderBook(DukasSubscriber ws)
    {
        lock.lock();
        try
        {
            orderBookSubscribers.add(ws);
            orderBookSubscribersSize = orderBookSubscribers.size();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void unsubscribeFromOrderBook(DukasSubscriber ws)
    {
        lock.lock();
        try
        {
            orderBookSubscribers.remove(ws);
            orderBookSubscribersSize = orderBookSubscribers.size();
        }
        finally
        {
            lock.unlock();
        }
    }

}
