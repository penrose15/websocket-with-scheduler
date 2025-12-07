package com.websocket.config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventHandler {
    private static final String TOPIC = "/topic";
    private final Map<String, Set<String>> topicSubscriber = new ConcurrentHashMap<>();

    private final WebSocketScheduler webSocketScheduler;

    public WebSocketEventHandler(WebSocketScheduler webSocketScheduler) {
        this.webSocketScheduler = webSocketScheduler;
    }

    @EventListener
    public void connectedToWebsocket(SessionSubscribeEvent event) {
        System.out.println("start connect");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String des = headerAccessor.getDestination();
        if (des != null && des.contains(TOPIC)) {
            String sessionId = headerAccessor.getSessionId();
            if (topicSubscriber.get(TOPIC) == null) {
                Set<String> sessionIdSet = new HashSet<>();
                sessionIdSet.add(sessionId);
                topicSubscriber.put(TOPIC, sessionIdSet);
                webSocketScheduler.startScheduler();
            } else if (topicSubscriber.containsKey(TOPIC)) {
                Set<String> sessionIds = topicSubscriber.get(TOPIC);
                if (sessionIds.isEmpty()) {
                    webSocketScheduler.startScheduler();
                }
                sessionIds.add(sessionId);
                topicSubscriber.put(TOPIC, sessionIds);
            }

        }
    }

    @EventListener
    public void unsubscription(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String des = headerAccessor.getFirstNativeHeader("id");
        if (des != null && des.contains(TOPIC)) {
            String sessionId = headerAccessor.getSessionId();
            if (topicSubscriber.get(TOPIC) == null) {
                webSocketScheduler.stopScheduler();
            } else if (topicSubscriber.containsKey(TOPIC)) {
                Set<String> sessionIds = topicSubscriber.get(TOPIC);
                sessionIds.remove(sessionId);
                if (sessionIds.isEmpty()) {
                    webSocketScheduler.stopScheduler();
                }
                topicSubscriber.put(TOPIC, sessionIds);
            }
        }
    }

    @EventListener
    public void disconnectWebSocket(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        topicSubscriber.forEach((key, value) -> {
            if (key.contains(TOPIC)) {
                value.remove(sessionId);
                if (value.isEmpty()) {
                    webSocketScheduler.stopScheduler();
                }
            }
        });

    }
}
