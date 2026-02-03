// src/main/java/com/portfolio/priceserver/PriceServer.java
package com.portfolio.priceserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.handler.HandlerList;

public class PriceServer {
    private final Server server;
    private final PriceEngine engine;

    public PriceServer(int port) {
        this.engine = new PriceEngine();
        this.server = new Server(port);

        ServletContextHandler wsContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        wsContext.setContextPath("/");

        JettyWebSocketServletContainerInitializer.configure(wsContext, (servletContext, wsContainer) -> {
            wsContainer.setMaxTextMessageSize(65536);
            wsContainer.addMapping("/prices", (req, resp) -> new PriceWebSocket(engine));
        });

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase(getClass().getClassLoader().getResource("static").toExternalForm());
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});

        HandlerList handlers = new HandlerList();
        handlers.addHandler(resourceHandler);
        handlers.addHandler(wsContext);

        server.setHandler(handlers);
    }

    public void start() throws Exception {
        server.start();
        engine.start();
    }

    public void stop() throws Exception {
        engine.stop();
        server.stop();
    }

    public void join() throws InterruptedException {
        server.join();
    }

    public static void main(String[] args) throws Exception {
        PriceServer server = new PriceServer(8080);
        server.start();
        System.out.println("Price server started at http://localhost:8080");
        server.join();
    }
}