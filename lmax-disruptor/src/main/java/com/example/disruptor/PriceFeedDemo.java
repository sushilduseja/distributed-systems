package com.example.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import java.util.concurrent.Executors;

public final class PriceFeedDemo {
    public static void main(String[] args) {
        Disruptor<PriceEvent> disruptor =
                new Disruptor<>(PriceEvent::new, 1024, Executors.defaultThreadFactory());

        disruptor.handleEventsWith(new PriceEventHandler(0.0002));

        RingBuffer<PriceEvent> ringBuffer = disruptor.start();
        try {
            PriceEventTranslator translator = new PriceEventTranslator();
            for (int i = 0; i < 5; i++) {
                double price = 1.3956 + (i * 0.0001);
                ringBuffer.publishEvent(translator, price);
            }
        } finally {
            disruptor.shutdown();
        }
    }
}
