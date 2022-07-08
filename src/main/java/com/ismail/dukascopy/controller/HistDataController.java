package com.ismail.dukascopy.controller;

import com.dukascopy.api.IBar;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.instrument.IFinancialInstrument.Type;
import com.ismail.dukascopy.model.ApiException;
import com.ismail.dukascopy.model.Candle;
import com.ismail.dukascopy.service.DukasStrategy;
import com.ismail.dukascopy.util.DukasUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class HistDataController {

    @Autowired
    private DukasStrategy strategy;

    @RequestMapping(value = "/api/v1/history", method = RequestMethod.GET)
    public List<Candle> getHistData(
            @RequestParam String instID,
            @RequestParam String timeFrame,
            @RequestParam(required = false) OptionalLong from,
            @RequestParam(required = false) OptionalLong to) {

        Instrument instrument = Instrument.valueOf(instID);
        long timeFrom = from == null ? 0 : from.getAsLong();
        long timeTo = to == null ? 0 : to.getAsLong();

        if (instrument == null)
            throw new ApiException(
                    "Invalid instrument: " + instID);

        Period period = null;

        switch (timeFrame) {
            case "1SEC":
                period = Period.ONE_SEC;
                break;
            case "10SEC":
                period = Period.TEN_SECS;
                break;
            case "1MIN":
                period = Period.ONE_MIN;
                break;
            case "5MIN":
                period = Period.FIVE_MINS;
                break;
            case "10MIN":
                period = Period.TEN_MINS;
                break;
            case "15MIN":
                period = Period.FIFTEEN_MINS;
                break;
            case "1HOUR":
                period = Period.ONE_HOUR;
                break;
            case "DAILY":
                period = Period.DAILY;
                break;
            default:
                period = Period.DAILY;
                break;
        }

        // timeFrom
        if (timeFrom == 0L) {
            timeFrom = System.currentTimeMillis() - DukasUtil.DAY * 5;
            timeTo = System.currentTimeMillis();

            // normalize time
            long periodInMillis = period.getInterval();

            timeFrom = timeFrom - timeFrom % periodInMillis;
            timeTo = timeTo - timeTo % periodInMillis;
        } else {
            timeTo = System.currentTimeMillis();
            // normalize time
            long periodInMillis = period.getInterval();
            timeTo = timeTo - timeTo % periodInMillis;
        }

        try {
            List<IBar> bars = strategy.getHistData(
                    instrument,
                    period,
                    OfferSide.BID,
                    timeFrom,
                    timeTo);

            if (bars != null && bars.size() > 0) {
                ArrayList<Candle> list = new ArrayList<>(bars.size());

                for (IBar bar : bars) {
                    Candle st = new Candle();
                    st.symbol = Optional.of(instID.replace("/", ""));
                    st.open = bar.getOpen();
                    st.high = bar.getHigh();
                    st.low = bar.getLow();
                    st.close = bar.getClose();
                    st.volume = bar.getVolume();
                    st.time = bar.getTime();

                    if (instrument.getType() == Type.FOREX) {
                        st.volume *= 100000.0;
                    }

                    list.add(st);
                }

                return list;
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("getHistData() error: ", e.getMessage(), e);

            throw new ApiException("Server error: " + e.getMessage());
        }
    }
}
