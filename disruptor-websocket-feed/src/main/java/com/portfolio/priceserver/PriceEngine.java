// src/main/java/com/portfolio/priceserver/PriceEngine.java
package com.portfolio.priceserver;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class PriceEngine {
    private static final int RING_BUFFER_SIZE = 1024;

    private record StockConfig(String symbol, double startPrice, double volatility) {}

    private static final StockConfig[] STOCKS = {
            new StockConfig("TECH", 150.0, 2.0),
            new StockConfig("FINX", 85.0, 1.5),
            new StockConfig("ENRG", 220.0, 3.0),
            new StockConfig("HLTH", 95.0, 1.2)
    };

    private final Disruptor<PriceEvent> disruptor;
    private final RingBuffer<PriceEvent> ringBuffer;
    private final Set<PriceWebSocket> sessions = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread[] producerThreads;

    public PriceEngine() {
        this.disruptor = new Disruptor<>(
                PriceEvent::new,
                RING_BUFFER_SIZE,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI,
                new YieldingWaitStrategy()
        );

        disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            String json = new PriceData(event.symbol(), event.price(), event.timestamp()).toJson();
            broadcast(json);
        });

        disruptor.start();
        this.ringBuffer = disruptor.getRingBuffer();
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            producerThreads = new Thread[STOCKS.length];
            for (int i = 0; i < STOCKS.length; i++) {
                final StockConfig stock = STOCKS[i];
                producerThreads[i] = Thread.ofVirtual().start(() -> produceSimulation(stock));
            }
        }
    }

    public void stop() {
        running.set(false);
        if (producerThreads != null) {
            for (Thread thread : producerThreads) {
                try {
                    thread.join(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        disruptor.shutdown();
    }

    public void addSession(PriceWebSocket session) {
        sessions.add(session);
    }

    public void removeSession(PriceWebSocket session) {
        sessions.remove(session);
    }

    private void broadcast(String message) {
        sessions.forEach(session -> session.sendMessage(message));
    }

    private void produceSimulation(StockConfig stock) {
        Random random = ThreadLocalRandom.current();
        double currentPrice = stock.startPrice();

        while (running.get()) {
            double change = (random.nextDouble() - 0.5) * stock.volatility();
            currentPrice = Math.max(stock.startPrice() * 0.5,
                    Math.min(stock.startPrice() * 1.5, currentPrice + change));

            long sequence = ringBuffer.next();
            try {
                PriceEvent event = ringBuffer.get(sequence);
                event.set(stock.symbol(), currentPrice, System.currentTimeMillis());
            } finally {
                ringBuffer.publish(sequence);
            }

            try {
                Thread.sleep(500 + random.nextInt(501));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}