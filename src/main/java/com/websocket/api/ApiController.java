package com.websocket.api;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiController {

    // 테스트용 api, 없어도 무방
    @MessageMapping("/hello") // /app/hello 로 들어옴 (prefix가 /app)
    @SendTo("/topic/hi")
    public Map<String, Object> websocketApi() {
        return Map.of("hello", "world");
    }
}
