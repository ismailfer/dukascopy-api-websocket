package com.ismail.dukascopy.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public enum TimeInForce {
    Day('0'), //
    Good_till_cancel('1'), //
    Immediate_or_cancel('3'), //
    Fill_or_kill('4'), //
    Good_till_date('6');

    private final char code;

    TimeInForce(char code) {
        this.code = code;
    }

    public static List<TimeInForce> getAllValues() {
        ArrayList<TimeInForce> list = new ArrayList<>();
        list.add(Day);
        list.add(Good_till_cancel);
        list.add(Immediate_or_cancel);
        list.add(Fill_or_kill);
        list.add(Good_till_date);

        return list;
    }
}