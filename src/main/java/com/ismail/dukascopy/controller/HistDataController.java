package com.ismail.dukascopy.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dukascopy.api.IBar;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.instrument.IFinancialInstrument.Type;
import com.ismail.dukascopy.model.ApiException;
import com.ismail.dukascopy.model.Candle;
import com.ismail.dukascopy.service.DukasStrategy;
import com.ismail.dukascopy.util.DukasUtil;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class HistDataController
{
    @Autowired
    private DukasStrategy strategy;

    @RequestMapping(value = "/api/v1/histdata", method = RequestMethod.GET)
    public List<Candle> getHistData(@RequestParam String instID, @RequestParam int period, @RequestParam long timeFrom, @RequestParam long timeTo)
    {
        Instrument instrument = Instrument.valueOf(instID);

        if (instrument == null)
            throw new ApiException("Invalid instrument: " + instID);

        Period per = null;
        switch (period)
        {
        case 60:
            per = Period.ONE_MIN;
            break;

        case 60 * 5:
            per = Period.FIVE_MINS;
            break;

        case 60 * 10:
            per = Period.TEN_MINS;
            break;

        case 60 * 15:
            per = Period.FIFTEEN_MINS;
            break;

        case 60 * 60:
            per = Period.ONE_HOUR;
            break;

        case 60 * 60 * 24:
            per = Period.DAILY;
            break;

        default:
            throw new ApiException("Invalid period: " + period);
        }

        
        // timeFrom
        if (timeFrom == 0L)
        {
            timeFrom = System.currentTimeMillis() - DukasUtil.DAY * 5;
            timeTo = System.currentTimeMillis();
            
            // normalize time
            long periodInMillis = period * 1000L;
            
            timeFrom = timeFrom - timeFrom % periodInMillis;
            timeTo = timeTo - timeTo % periodInMillis;
            
        }
        
        
        try
        {
            
            List<IBar> bars =   strategy.getHistData(instrument, per, OfferSide.BID, timeFrom, timeTo);
            

             if (bars != null && bars.size() > 0)
             {
                 ArrayList<Candle> list = new ArrayList<>(bars.size());

                 for (IBar bar : bars)
                 {
                     Candle st = new Candle();
                     st.symbol = instID.replace("/", "");
                     st.open = bar.getOpen();
                     st.high = bar.getHigh();
                     st.low = bar.getLow();
                     st.close = bar.getClose();
                     st.volume = bar.getVolume();
                     st.time = bar.getTime();
                     st.period = period;
                     
                     if (instrument.getType() == Type.FOREX)
                     {
                         st.volume *= 100000.0;
                     }
                     
                     list.add(st);
                 }
                 
                 return list;
             }
             else
             {
                 return new ArrayList<>();
             }
             
        }
        catch (Exception e)
        {
            log.error("getHistData() error: ", e.getMessage(), e);

            throw new ApiException("Server error: " + e.getMessage());
        }
    }
}
