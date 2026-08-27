# AsyncAPI Kotlin DSL

AsyncAPI Kotlin DSL 기반 AsyncAPI React UI 문서화 프로젝트 (SSE · WebSocket · STOMP)

통합 테스트를 실행하면 실제로 주고받은 메시지가 곧 문서가 된다. 테스트가 통과하지 않으면 문서도 생성되지 않는다.

---

## 지원 프로토콜

| 프로토콜            | 진입점                 | 방향             |
|-----------------|---------------------|----------------|
| SSE             | `documentSse`       | receive        |
| WebSocket (raw) | `documentWebSocket` | send + receive |
| STOMP           | `documentStomp`     | send + receive |

---

## DSL 구조

```
// SSE — 서버가 발행하는 이벤트
documentSse("identifier") {
    channel(path = "/events/...", protocol = "https", description = "...")
    operation { summary("...") }                       // 생략 가능

    receive<ResponseType> {
        field("path", "description")                   // 선언 안 한 payload 필드가 있으면 실패
        trigger { httpPost(path = "...", jsonBody = "...") }   // 서버 발행 유도 (REST 호출 등)
        verify { it.foo shouldBe "..." }
    }
}

// WebSocket (raw) — 클라이언트 전송 → 서버 브로드캐스트
documentWebSocket("identifier") {
    channel(path = "/ws/...", protocol = "ws", description = "...")

    send<RequestType>(summary = "...") {
        field("path", "description")
        payload("""{ ... }""")                         // 실제로 보낼 JSON
    }
    receive<ResponseType>(summary = "...") {
        field("path", "description")
        verify { it.foo shouldBe "..." }
    }
}

// STOMP — 전송/구독 destination
documentStomp("identifier", endpoint = "/ws-stomp") {
    destination(sendTo = "/app/...", subscribeTo = "/topic/...", description = "...")

    send<RequestType>(summary = "...") {
        field("path", "description")
        payload("""{ ... }""")
    }
    receive<ResponseType>(summary = "...") {
        field("path", "description")
        verify { it.foo shouldBe "..." }
    }
}
```

### 사용 예시

#### SSE

```
class NotificationControllerTest : AsyncApiDocsTest() {

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Test
    fun `구독자가 연결된 상태에서 알림을 발행하면 구독자가 수신한다`() {
        documentSse("notifications/receiveNotification") {
            channel(
                path = "/events/notifications",
                protocol = "https",
                description = "서버가 발행하는 실시간 알림 이벤트",
            )
            operation { summary("서버가 발행하는 알림 이벤트를 수신한다") }

            receive<NotificationResponse> {
                field("type", "알림 종류")
                field("message", "알림 본문")
                trigger {
                    httpPost(
                        path = "/events/notifications",
                        jsonBody = """{"type":"notification","message":"점심 스터디"}""",
                    ) shouldBe 204
                }
                verify {
                    it.type shouldBe "notification"
                    it.message shouldBe "점심 스터디"
                }
            }
        }

        notificationRepository.findAll().map { it.message } shouldContain "점심 스터디"
    }
}
```

#### WebSocket (raw)

```
class ChatWebSocketHandlerTest : AsyncApiDocsTest() {

    @Test
    fun `채팅 메시지를 보내면 sentAt 이 붙어 브로드캐스트된다`() {
        documentWebSocket("chat/sendMessage") {
            channel(
                path = "/ws/chat",
                protocol = "ws",
                description = "실시간 채팅 채널",
            )
            send<ChatMessageRequest>(summary = "채팅 메시지 전송") {
                field("sender", "보낸 사람")
                field("text", "메시지 본문")
                payload("""{"sender":"heee","text":"점심 뭐 먹지"}""")
            }
            receive<ChatMessageResponse>(summary = "브로드캐스트된 메시지 수신") {
                field("sender", "보낸 사람")
                field("text", "메시지 본문")
                field("sentAt", "서버 수신 시각(ISO-8601)")
                verify {
                    it.sender shouldBe "heee"
                    it.text shouldBe "점심 뭐 먹지"
                }
            }
        }
    }
}
```

#### STOMP

```
class ChatStompControllerTest : AsyncApiDocsTest() {

    @Test
    fun `app-chat 으로 보내면 topic-chat 구독자가 메시지를 받는다`() {
        documentStomp("chat/stompChat", endpoint = StompConfig.ENDPOINT) {
            destination(
                sendTo = "/app/chat",
                subscribeTo = "/topic/chat",
                description = "STOMP 채팅",
            )
            send<ChatMessageRequest>(summary = "메시지 전송 (/app/chat)") {
                field("sender", "보낸 사람")
                field("text", "메시지 본문")
                payload("""{"sender":"heee","text":"STOMP 안녕"}""")
            }
            receive<ChatMessageResponse>(summary = "구독 메시지 수신 (/topic/chat)") {
                field("sender", "보낸 사람")
                field("text", "메시지 본문")
                field("sentAt", "서버 수신 시각(ISO-8601)")
                verify { it.sender shouldBe "heee" }
            }
        }
    }
}
```

---

## DSL 상세

### 채널

| 메서드                                             | 설명                                                                            |
|-------------------------------------------------|-------------------------------------------------------------------------------|
| `channel(path, protocol, description)`          | SSE·WebSocket 채널. `protocol`(`https` / `ws` / `wss`)에 따라 `#/servers/*`에 바인딩된다 |
| `destination(sendTo, subscribeTo, description)` | STOMP 전용. 전송(`/app/...`)·구독(`/topic/...`) destination이 각각 별도 채널이 된다           |

### 메시지

| 메서드                          | 설명                                                                           |
|------------------------------|------------------------------------------------------------------------------|
| `receive<T> { }`             | 서버가 보내는 메시지 타입 `T` 를 문서화한다                                                   |
| `send<T>(summary) { }`       | 클라이언트가 보내는 메시지 타입 `T` 를 문서화한다 (WebSocket·STOMP)                              |
| `field(path, description)`   | payload 필드 선언. 선언 목록과 실제 payload를 대조한다. 타입은 캡처된 값에서 추론한다                     |
| `field(...).optional()`      | payload에 해당 필드가 없어도 검증을 통과시킨다                                                |
| `payload(json)`              | `send` 블록에서 실제로 전송할 JSON 문자열                                                 |
| `trigger { }`                | `receive` 블록에서 서버 발행을 유도하는 코드 (보통 `httpPost(...)`). SSE 전용                   |
| `verify { }`                 | 역직렬화한 수신 payload를 검증한다. 실패하면 스니펫이 생성되지 않는다                                   |
| `operation { summary(...) }` | AsyncAPI operation의 `summary`. SSE 전용 (WebSocket·STOMP는 `send`/`receive` 인자) |

### `field()` 검증

`field()`로 선언한 목록과 테스트에서 실제로 주고받은 payload를 대조한다.

- payload에 있는데 `field()`로 선언하지 않은 필드가 있으면 → `AssertionError`
- `field()`로 선언했는데(`.optional()` 아님) payload에 없으면 → `AssertionError`
- 통과하면 `description` + 추론된 타입으로 스키마가 만들어지고, 실제 payload가 `examples`로 들어간다

### 테스트 베이스 — `AsyncApiDocsTest`

`@SpringBootTest(webEnvironment = RANDOM_PORT)`를 단 abstract 클래스. 상속하면 다음을 쓸 수 있다.

| 멤버                                | 설명                              |
|-----------------------------------|---------------------------------|
| `documentSse(id) { }`             | SSE 문서화 진입점                     |
| `documentWebSocket(id) { }`       | raw WebSocket 문서화 진입점           |
| `documentStomp(id, endpoint) { }` | STOMP 문서화 진입점                   |
| `httpPost(path, jsonBody): Int`   | 이벤트 발행 유도용 헬퍼. HTTP 상태 코드를 반환한다 |
| `port`                            | 랜덤 포트 (`@LocalServerPort`)      |

`@Autowired` 필드는 서브클래스에 직접 선언한다. 통합 테스트는 plain JUnit 5 (`@Test`)로 작성한다.

---

## AsyncAPI 문서 생성

```
./gradlew generateDocs
```

통합 테스트 실행 → `build/asyncapi/asyncapi.yaml` 생성 → `build/resources/main/static/` 복사까지 한 번에 수행된다.  
`verify`나 `field` 대조가 실패하면 스니펫이 생성되지 않는다 (문서 = 통과한 테스트의 증거).

이후 애플리케이션을 실행하면 다음에서 문서를 확인할 수 있다.

- `/asyncapi.html` — AsyncAPI React 컴포넌트로 렌더된 문서
- `/asyncapi.yaml` — 원본 스펙

`bootRun` · `bootJar` · `build`는 모두 문서 생성에 의존한다.
