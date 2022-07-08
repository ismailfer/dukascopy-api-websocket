package com.ismail.dukascopy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.ToString;

/**
 * New Order Response
 * 
 * @author ismail
 * @since 20220704
 */
@Data
@ToString
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewOrderResp {
    public String symbol = null;

    public String clientOrderID = null;

    public String dukasOrderID = null;

    public double fillQty = 0.0;

    public double fillPrice = 0.0;

    public boolean orderSuccess = false;

    public String rejectReason = null;

}
