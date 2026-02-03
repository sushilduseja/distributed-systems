// src/test/java/com/portfolio/priceserver/DisruptorTest.java
package com.portfolio.priceserver;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DisruptorTest {
    private Disruptor<PriceEvent> disruptor;
    private RingBuffer<PriceEvent> ringBuffer;

    @BeforeEach
    void setup() {
        disruptor = new Disruptor<>(
                PriceEvent::new,
                16,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI,
                new YieldingWaitStrategy()
        );
    }

    @AfterEach
    void teardown() {
        disruptor.shutdown();
    }

    @Test
    void testDisruptorProcessesEvents() throws Exception {
        List<PriceData> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(5);

        disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            received.add(new PriceData(event.symbol(), event.price(), event.timestamp()));
            latch.countDown();
        });

        disruptor.start();
        ringBuffer = disruptor.getRingBuffer();

        for (int i = 0; i < 5; i++) {
            long sequence = ringBuffer.next();
            try {
                PriceEvent event = ringBuffer.get(sequence);
                event.set("TEST", 100.0 + i, System.currentTimeMillis());
            } finally {
                ringBuffer.publish(sequence);
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(5, received.size());

        for (int i = 0; i < 5; i++) {
            assertEquals("TEST", received.get(i).symbol());
            assertEquals(100.0 + i, received.get(i).price(), 0.01);
        }
    }

    @Test
    void testPriceDataJsonFormat() {
        PriceData data = new PriceData("TECH", 123.45, 1706868000000L);
        String json = data.toJson();

        assertTrue(json.contains("\"symbol\":\"TECH\""));
        assertTrue(json.contains("\"price\":123.45"));
        assertTrue(json.contains("\"timestamp\":1706868000000"));
    }
}