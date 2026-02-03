package com.example.disruptor;

import com.lmax.disruptor.EventTranslatorOneArg;

final class PriceEventTranslator implements EventTranslatorOneArg<PriceEvent, Double> {
    @Override
    public void translateTo(PriceEvent event, long sequence, Double price) {
        event.setPrice(price);
    }
}
