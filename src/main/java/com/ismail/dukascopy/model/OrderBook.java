package com.ismail.dukascopy.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

/**
 * Full Order Book
 * 
 * @author ismail
 * @since 20220617
 */
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderBook {

    public String symbol = null;

    public double bidQty = 0.0;

    public double bid = 0.0;

    public double ask = 0.0;

    public double askQty = 0.0;

    public double last = 0.0;

    public double spread;

    public double spreadBps;

    public long updateTime = 0L;

    public int updateNumber = 0;

    public int depthLevels = 0;

    public boolean live = false;

    public List<OrderBookEntry> bids = null;

    public List<OrderBookEntry> asks = null;

}
