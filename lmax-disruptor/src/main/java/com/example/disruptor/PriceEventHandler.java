package com.example.disruptor;

import com.lmax.disruptor.EventHandler;

final class PriceEventHandler implements EventHandler<PriceEvent> {
    private final double spread;

    PriceEventHandler(double spread) {
        this.spread = spread;
    }

    @Override
    public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
        double bid = event.getPrice();
        double ask = bid + spread;
        System.out.println("Quote: Bid=" + bid + ", Ask=" + ask);
    }
}
