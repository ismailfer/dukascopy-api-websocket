package com.ismail.dukascopy.controller;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.LinkedHashMap;

/**
 * Jetty Server to host the websockets
 * 
 * @author ismail
 * @since 20220617
 */
@Slf4j
public class JettyServer
{

    private final Server server;
    private final ServerConnector connector;
    ServletContextHandler context = null;

    LinkedHashMap<String, Class> websocketMappings = new LinkedHashMap<>();

    public JettyServer()
    {
        server = new Server();
        connector = new ServerConnector(server);
        server.addConnector(connector);

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);


    }

    /**
     * example:
     * "/ticker/*", TickerWebsocket.class
     *
     * @param pMapping
     * @param pWebsocketClass
     */
    public void addWebsocket(String pMapping, Class pWebsocketClass)
    {

            // Add websockets
            websocketMappings.put(pMapping, pWebsocketClass);
    }

    public void configureWebsockets()
    {
        // Configure specific websocket behavior
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) ->
        {
            // Configure default max size
            wsContainer.setMaxTextMessageSize(65535);

            // Add websockets
            // wsContainer.addMapping("/ticker/*", TickerWebsocket.class);
            for (String mapping : websocketMappings.keySet())
            {
                wsContainer.addMapping(mapping, websocketMappings.get(mapping));
            }
        });
    }

    public void setPort(int port)
    {
        connector.setPort(port);
    }

    public void start() throws Exception
    {
        server.start();
    }

    public URI getURI()
    {
        return server.getURI();
    }

    public void stop() throws Exception
    {
        server.stop();
    }

    public void join() throws InterruptedException
    {        
        server.join();
    }

}