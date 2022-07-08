package com.ismail.dukascopy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.ToString;

/**
 * Position Detail 
 * 
 * @author ismail
 * @since 20220707
 */
@Data
@ToString
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Position
{           
    public String symbol = null;

    public String clientOrderID = null;

    public String dukasOrderID = null;
    
    public double quantity;
    
    public double openPrice;

    public double openQuantity;
    
    public long closeTime;
    
    public double closePrice;
    
    public double closeQuantity;
    
    public double commission;
    
    public double stopLossPrice;
    
    public double takeProfitPrice;
    
    public boolean buySide;
    
    public long creationTime;
    
    public String state;
    
    public boolean valid;
    
    public String errorMsg;
  
}
