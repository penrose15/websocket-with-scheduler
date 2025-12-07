package com.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class ReadFileHelper {
    private final SimpMessagingTemplate messagingTemplate;

    public ReadFileHelper(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void readFileAndSendMessage() {
        log.info("readFile");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int randomNumber = random.nextInt(0, 100);
        messagingTemplate.convertAndSend("/topic/hi", "hello" + randomNumber);
    }
}
