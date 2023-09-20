package com.alex.pingmonitor;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class PingHourlyRun {

    public int id;
    public Timestamp ts;
    public BigDecimal avgResponse;
    public int cnt;
    public int cntUnreachable;

    public PingHourlyRun(int id, Timestamp ts, BigDecimal avgResponse, int cnt, int cntUnreachable) {
        this.id = id;
        this.ts = ts;
        this.avgResponse = avgResponse;
        this.cnt = cnt;
        this.cntUnreachable = cntUnreachable;
    }

    @Override
    public String toString() {
        return  id + ", " +
                ts + ", " +
                avgResponse + ", " +
                cnt + ", " +
                cntUnreachable;
    }

}