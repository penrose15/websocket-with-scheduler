## 만들게 된 계기

- 회사에서 n초에 1번 갱신되는 데이터를 통해 실시간 조회 API를 만들어야 했음
- 만약 여러 사용자가 n초에 1번씩 실시간 조회를 위해 API를 호출하면 사용자 수에 따라 데이터를 조회하는 횟수가 선형적으로 늘어나게 됨
- 문제는 그 원천 데이터가 파일이라 데이터를 조회하는 횟수가 많을수록 File I/O도 같이 늘어나 성능에 이슈가 발생할 수 있음
- 최초 1회만 조회하고 이를 여러 사용자에게 전달하기 위해 websocket + scheduler를 통해 구현

## 구조
```
├── WebsocketApplication.java
├── api
│   └── ApiController.java
└── config
    ├── ReadFileHelper.java // 파일 읽는 목업 클래스
    ├── SchedulerConfig.java // taskScheduler 설정
    ├── WebSocketConfig.java // 웹소켓 설정
    ├── WebSocketEventHandler.java // 웹소켓 이벤트(CONNECTED, DISCONNECTED, SUBSCRIBE..) 발생시 이뤄질 행동
    └── WebSocketScheduler.java // 스케쥴러

```

```groovy
dependencies {
    ...
    implementation 'org.springframework.boot:spring-boot-starter-websocket' // 얘 추가
}
```

## WebSocketConfig
```java
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket-start") // 웹소켓 최초 핸드쉐이킹 위치
                .setAllowedOrigins("*"); // CORS 설정
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // 구독 prefix
        registry.setApplicationDestinationPrefixes("/app"); // 클라이언트가 /app를 가진 목적지(@MessageMapping /app/hello) 로 메시지 날림
    }
```

## WebSocketEventHandler
```java
package com.websocket.config;

@Component
public class WebSocketEventHandler {
    private static final String TOPIC = "/topic";
    // 회사에서는 토픽이 1개만 있을것이므로 Set<String> 사용해도 무방 (단, 동시성 이슈는 고려해야 함)
    private final Map<String, Set<String>> topicSubscriber = new ConcurrentHashMap<>();

    private final WebSocketScheduler webSocketScheduler;

    public WebSocketEventHandler(WebSocketScheduler webSocketScheduler) {
        this.webSocketScheduler = webSocketScheduler;
    }

    @EventListener
    public void connectedToWebsocket(SessionSubscribeEvent event) {
        System.out.println("start connect");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String des = headerAccessor.getDestination(); // 목적지, (/topic)
        ...
        Set<String> sessionIdSet = new HashSet<>(); // 세션 아이디를 저장
        sessionIdSet.add(sessionId);
        topicSubscriber.put(TOPIC, sessionIdSet);
        webSocketScheduler.startScheduler(); // 스케쥴러 시작
    }

    @EventListener
    public void unsubscription(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String des = headerAccessor.getFirstNativeHeader("id");
        ...
        Set<String> sessionIds = topicSubscriber.get(TOPIC);
        sessionIds.remove(sessionId); // 구독 해제한 sessionId를 제거
        if (sessionIds.isEmpty()) { // 만약 구독하고 있는 id가 없다면 스케쥴러 정지
            webSocketScheduler.stopScheduler();
        }
        topicSubscriber.put(TOPIC, sessionIds);
    }

    @EventListener
    public void disconnectWebSocket(SessionDisconnectEvent event) { // 새로고침으로 인해 connection 끊길 때 고려
        String sessionId = event.getSessionId();
        topicSubscriber.forEach((key, value) -> {
            if (key.contains(TOPIC)) {
                value.remove(sessionId);
                if (value.isEmpty()) { // 만약 구독하고 있는 id 가 없다면 스케쥴러 정지
                    webSocketScheduler.stopScheduler();
                }
            }
        });

    }
}
```

## SchedulerConfig
```java
package com.websocket.config;

@Configuration
public class SchedulerConfig {
    // 스케쥴러 threadpool을 1로 설정한 이유 :
    // 1. 파일 IO는 꽤 부담이 큰 작업이므로 스레드 사용을 최소로 함
    // 2. 어차피 스케쥴러 내부에서 한번만 파일을 읽으면 구독자들 모두한테 메시지 뿌리기만 하면 되서 스레드 풀이 클 필요 x

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("websocket-t-");
        taskScheduler.initialize();
        return taskScheduler;
    }
}

```

## WebSocketScheduler
```java
package com.websocket.config;

@Component
public class WebSocketScheduler {
    private final TaskScheduler taskScheduler;
    private final ReadFileHelper readFileHelper;
    private ScheduledFuture<?> future;

    public WebSocketScheduler(TaskScheduler taskScheduler, ReadFileHelper readFileHelper) {
        this.taskScheduler = taskScheduler;
        this.readFileHelper = readFileHelper;
    }

    public void startScheduler() { // 스케쥴러 시작
        if (future == null || future.isCancelled()) {
            future = taskScheduler.scheduleAtFixedRate(readFileHelper::readFileAndSendMessage, 1000);
        }
    }

    public void stopScheduler() { // 스케쥴러 정지
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
        }
    }
}
```
