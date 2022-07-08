package com.ismail.dukascopy.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public enum OrderSide {
    Buy('1'),
    Sell('2');

    private final char code;

    OrderSide(char code) {
        this.code = code;
    }

    public static List<OrderSide> getAllValues() {
        ArrayList<OrderSide> list = new ArrayList<>();
        list.add(Buy);
        list.add(Sell);

        return list;
    }
}