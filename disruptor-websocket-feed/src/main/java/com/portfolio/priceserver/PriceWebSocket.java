package com.portfolio.priceserver;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class PriceWebSocket {
    private final PriceEngine engine;
    private Session session;

    public PriceWebSocket(PriceEngine engine) {
        this.engine = engine;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        engine.addSession(this);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        engine.removeSession(this);
        this.session = null;
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        engine.removeSession(this);
    }

    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getRemote().sendString(message);
            } catch (Exception e) {
                // Ignore send failures
            }
        }
    }
}
