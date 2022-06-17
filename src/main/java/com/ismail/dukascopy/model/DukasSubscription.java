package com.ismail.dukascopy.model;

import java.util.Set;

import com.dukascopy.api.Instrument;

import lombok.Data;

@Data
public class DukasSubscription
{
    public String id;

    public long time;

    public boolean success;

    public Set<Instrument> instruments;

}