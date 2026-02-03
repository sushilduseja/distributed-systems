package com.portfolio.priceserver;

public class PriceEvent {
    private String symbol;
    private double price;
    private long timestamp;

    public void set(String symbol, double price, long timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.timestamp = timestamp;
    }

    public String symbol() {
        return symbol;
    }

    public double price() {
        return price;
    }

    public long timestamp() {
        return timestamp;
    }
}
