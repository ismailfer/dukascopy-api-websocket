package com.ismail.dukascopy.controller;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import com.dukascopy.api.Instrument;
import com.ismail.dukascopy.service.DukasService;
import com.ismail.dukascopy.service.DukasStrategy;
import com.ismail.dukascopy.service.DukasSubscriber;
import com.ismail.dukascopy.util.DukasUtil;
import com.ismail.dukascopy.util.ObjectQueue;

import lombok.extern.slf4j.Slf4j;

/**
 * Market data websocket
 * 
 * servers either TopOfBook or full OrderBook
 * 
 * @author ismail
 * @since 20220617
 */
@Slf4j
public class MarketDataWebsocket extends WebSocketAdapter implements DukasSubscriber
{
    private DukasService dukasService;

    public DukasStrategy strategy;

    private ObjectQueue<EventJob> mEventQueue = null;

    private EventProcessor eventProcessor = null;

    private Session sess = null;

    private boolean tob = true;

    @Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);

        this.sess = sess;

        mEventQueue = new ObjectQueue<>(true);

        // start the event processor
        eventProcessor = new EventProcessor();
        eventProcessor.setName(getClass().getSimpleName() + "_" + eventProcessor.getClass().getSimpleName() + "_" + sess.hashCode());
        eventProcessor.setDaemon(true);
        eventProcessor.setPriority(Thread.MIN_PRIORITY);
        eventProcessor.start();

        // app = DukascopyApplication.getInstance();
        dukasService = DukasService.getInstance();
        strategy = dukasService.strategy;

        // --------------------------------------------------------------------------------------------------
        // Optional: subscribe to a list of instrumentIDs; otherwise return default list
        // --------------------------------------------------------------------------------------------------
        String instIDs = DukasUtil.get(sess.getUpgradeRequest(), "instIDs", null);

        if (DukasUtil.isDefined(instIDs))
        {
            List<String> instIDList = DukasUtil.splitToArrayList(instIDs, ',');
            
            Set<Instrument> instruments = new TreeSet<>();

            for (String instrumentID : instIDList)
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
            
            if (instruments.size() > 0)
            {
                strategy.adjustSubscription("new-sub-"+System.currentTimeMillis(), instruments);
            }
        }

        // --------------------------------------------------------------------------------------------------
        // true => TopOfBook
        // false => OrderBook
        // --------------------------------------------------------------------------------------------------

        tob = DukasUtil.getb(sess.getUpgradeRequest(), "topOfBook", true);

        if (tob)
        {
            strategy.subscribeToTOB(this);
        }
        else
        {
            strategy.subscribeToOrderBook(this);
        }

        log.info("onWebSocketConnect() << " + strategy);

    }

    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);

        log.info("onWebSocketText() << " + message);

    }

    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode, reason);

        log.info("onWebSocketClose() << " + statusCode + ":" + reason);

        if (tob)
        {
            strategy.unsubscribeFromTOB(this);
        }
        else
        {
            strategy.unsubscribeFromOrderBook(this);
        }
    }

    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);

        log.info("onWebSocketError() << " + cause.getMessage(), cause);

    }

    public void awaitClosure() throws InterruptedException
    {
        log.info("onWebSocketClose() ");

    }

    private void sendMessage_(String text) throws IOException
    {
        log.info("sendMessage() >> " + text);

        getSession().getRemote().sendString(text);
    }

    @Override
    public void sendMessage(String text)
    {
        EventJob job = new EventJob();
        job.text = text;

        mEventQueue.put(job);
    }

    public class EventJob
    {
        public String text;
    }

    public class EventProcessor extends Thread
    {

        public void run()
        {
            log.info(getName(), ".run() STARTED");

            while (sess.isOpen())
            {
                try
                {
                    EventJob job = mEventQueue.receiveWithWait(1000);

                    if (job != null)
                    {
                        sendMessage_(job.text);
                    }

                }
                catch (Exception e)
                {
                    log.warn("Non-fatal Error: ", e.getMessage(), e);
                }

            }

            log.warn(getName(), ".run() EXITED");
        }
    }
}