package com.ismail.dukascopy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.IOrder.State;
import com.ismail.dukascopy.model.ApiException;
import com.ismail.dukascopy.model.NewOrderResp;
import com.ismail.dukascopy.model.OrderSide;
import com.ismail.dukascopy.model.OrderType;
import com.ismail.dukascopy.service.DukasStrategy;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class OrderController
{
    @Autowired
    private DukasStrategy strategy;

    @RequestMapping(value = "/order", method = RequestMethod.POST)
    public NewOrderResp submitOrder(@RequestParam String clientOrderID, @RequestParam String instID, @RequestParam OrderSide side, @RequestParam OrderType orderType,
            @RequestParam double quantity, @RequestParam double price)
    {
        Instrument instrument = Instrument.valueOf(instID);

        if (instrument == null)
            throw new ApiException("Invalid instrument: " + instID);

        NewOrderResp resp = new NewOrderResp();
        resp.setSymbol(instID);
        resp.setClientOrderID(clientOrderID);

        try
        {
            long timeout = 5000;

            IOrder order = strategy.submitOrder(clientOrderID, instrument, side, orderType, quantity, price, timeout);

            if (order != null)
            {
                resp.setDukasOrderID(order.getId());
                

                if (order.getState() == State.FILLED)
                {
                    resp.setFillPrice(order.getOpenPrice());
                    resp.setFillQty(order.getAmount() * 1000000.0);
                }
                else
                {
                    resp.setFillPrice(0.0);
                    resp.setFillQty(0.0);
                }

                resp.setOrderSuccess(true);
            }
        }
        catch (Exception e)
        {
            log.error("submitOrder() error: ", e.getMessage(), e);

            resp.setRejectReason(e.getMessage());
            resp.setOrderSuccess(false);

            // throw new ApiException("Server error: " + e.getMessage());
        }

        return resp;
    }
}
