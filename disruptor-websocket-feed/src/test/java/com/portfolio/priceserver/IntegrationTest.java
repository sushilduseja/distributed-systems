package com.portfolio.priceserver;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    private PriceServer server;
    private WebSocketClient client;

    @BeforeEach
    void setup() throws Exception {
        server = new PriceServer(8081);
        server.start();
        Thread.sleep(500);
        
        client = new WebSocketClient();
        client.start();
    }

    @AfterEach
    void teardown() throws Exception {
        if (client != null) {
            client.stop();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void testClientReceivesPriceUpdates() throws Exception {
        TestWebSocketClient testClient = new TestWebSocketClient(10);
        
        Session session = client.connect(
            testClient, 
            new URI("ws://localhost:8081/prices")
        ).get(5, TimeUnit.SECONDS);
        
        assertTrue(session.isOpen());
        assertTrue(testClient.awaitMessages(5, TimeUnit.SECONDS));
        
        List<String> messages = testClient.getMessages();
        assertTrue(messages.size() >= 10);
        
        for (String message : messages) {
            assertTrue(message.contains("\"symbol\":\"TECH\""));
            assertTrue(message.contains("\"price\":"));
            assertTrue(message.contains("\"timestamp\":"));
        }
        
        session.close();
    }

    @WebSocket
    static class TestWebSocketClient {
        private final List<String> messages = new ArrayList<>();
        private final CountDownLatch latch;

        public TestWebSocketClient(int expectedMessages) {
            this.latch = new CountDownLatch(expectedMessages);
        }

        @OnWebSocketConnect
        public void onConnect(Session session) {
        }

        @OnWebSocketMessage
        public void onMessage(String message) {
            messages.add(message);
            latch.countDown();
        }

        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
        }

        public boolean awaitMessages(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public List<String> getMessages() {
            return new ArrayList<>(messages);
        }
    }
}
