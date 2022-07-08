package com.ismail.dukascopy.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public enum OrderType {
    Market('1'), //
    Limit('2'), //
    Stop('3'), //
    Stop_Limit('4');

    private final char code;

    OrderType(char code) {
        this.code = code;
    }

    public static List<OrderType> getAllValues() {
        ArrayList<OrderType> list = new ArrayList<>();
        list.add(Market);
        list.add(Limit);
        list.add(Stop);
        list.add(Stop_Limit);

        return list;
    }
}