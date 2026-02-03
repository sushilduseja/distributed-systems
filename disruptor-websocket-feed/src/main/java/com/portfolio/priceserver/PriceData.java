package com.portfolio.priceserver;

public record PriceData(String symbol, double price, long timestamp) {
    public String toJson() {
        return String.format("{\"symbol\":\"%s\",\"price\":%.2f,\"timestamp\":%d}", 
            symbol, price, timestamp);
    }
}

