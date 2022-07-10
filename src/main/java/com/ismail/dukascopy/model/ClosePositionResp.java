package com.ismail.dukascopy.model;

import com.dukascopy.api.IOrder;
import com.dukascopy.api.IOrder.State;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.ToString;

/**
 * Close Position Response
 * 
 * @author Muhammed Mortgy
 * @since 20220707
 */
@Data
@ToString
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClosePositionResp {

    public String clientOrderID = null;

    public IOrder order = null;

    public boolean closeSuccess = false;

    public String rejectReason = null;

}
