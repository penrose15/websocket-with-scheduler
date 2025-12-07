package com.websocket.config;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

@Component
public class WebSocketScheduler {
    private final TaskScheduler taskScheduler;
    private final ReadFileHelper readFileHelper;
    private ScheduledFuture<?> future;

    public WebSocketScheduler(TaskScheduler taskScheduler, ReadFileHelper readFileHelper) {
        this.taskScheduler = taskScheduler;
        this.readFileHelper = readFileHelper;
    }

    public void startScheduler() {
        if (future == null || future.isCancelled()) {
            System.out.println("start scheduler");
            future = taskScheduler.scheduleAtFixedRate(readFileHelper::readFileAndSendMessage, 1000);
        }
    }

    public void stopScheduler() {
        if (future != null && !future.isCancelled()) {
            System.out.println("stop scheduler");
            future.cancel(true);
        }
    }
}
