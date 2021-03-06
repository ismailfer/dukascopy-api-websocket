package com.ismail.dukascopy.model;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.ToString;

/**
 * Candle
 * 
 * @author ismail
 * @since 20220617
 */
@Data
@ToString
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Candle {
    public Optional<String> symbol = null;

    public double open = 0.0;

    public double high = 0.0;

    public double low = 0.0;

    public double close = 0.0;

    public double volume = 0.0;

    public double spread = 0.0;

    public OptionalLong ticks;

    public long timestamp;

    public OptionalInt period;

}
