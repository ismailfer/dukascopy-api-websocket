package com.ismail.dukascopy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.ToString;

/**
 * Top of Book
 * 
 * @author ismail
 * @since 20220617
 */
@Data
@ToString
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopOfBook
{           
    public String symbol = null;

    public double bidQty = 0.0;

    public double bid = 0.0;
    
    public double ask = 0.0;
    
    public double askQty = 0.0;

    public double last = 0.0;

    public double spread;
    
    public double spreadBps;
    
    public long updateTime = 0L;

    public long updateNumber = 0;

    public int depthLevels = 0;
    
    public String mdSource;
        
    public boolean live = false;

}
