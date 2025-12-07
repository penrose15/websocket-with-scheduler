package com.websocket.api;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiController {

    // 테스트용 api, 없어도 무방
    @MessageMapping("/hello")
    @SendTo("/topic/hi")
    public Map<String, Object> websocketApi() {
        return Map.of("hello", "world");
    }
}
