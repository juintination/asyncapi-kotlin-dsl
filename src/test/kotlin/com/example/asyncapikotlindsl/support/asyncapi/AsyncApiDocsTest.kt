package com.example.asyncapikotlindsl.support.asyncapi

import com.example.asyncapikotlindsl.support.asyncapi.dsl.SseDocumentationDsl
import com.example.asyncapikotlindsl.support.asyncapi.dsl.StompDocumentationDsl
import com.example.asyncapikotlindsl.support.asyncapi.dsl.WebSocketDocumentationDsl
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

/**
 * SSE / WebSocket 통합 테스트 + AsyncAPI 문서화의 공통 베이스 (plain JUnit 5).
 *
 * 상속하면 랜덤 포트로 앱이 뜬다. 필요한 빈은 서브클래스에 `@Autowired` 필드로 직접 선언한다.
 * `documentSse(...) { }` / `documentWebSocket(...) { }` 로 실제 연결·수신해 컨트롤러 동작을
 * 검증하면서 스니펫을 부산물로 만든다.
 *
 * pangyeori-be 의 RestDocsMvcTest 에 대응한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AsyncApiDocsTest {

    @LocalServerPort
    protected var port: Int = 0

    private val httpClient = HttpClient.newHttpClient()

    // identifier(resource/action) 로 SSE 한 건을 문서화한다.
    protected fun documentSse(
        identifier: String,
        block: SseDocumentationDsl.() -> Unit,
    ) {
        SseDocumentationDsl(
            identifier = identifier,
            baseUrl = "http://localhost:$port",
        ).apply(block).execute()
    }

    // identifier(resource/action) 로 raw WebSocket 한 건(send + receive)을 문서화한다.
    protected fun documentWebSocket(
        identifier: String,
        block: WebSocketDocumentationDsl.() -> Unit,
    ) {
        WebSocketDocumentationDsl(
            identifier = identifier,
            baseUrl = "http://localhost:$port",
        ).apply(block).execute()
    }

    // identifier(resource/action) 로 STOMP 한 건(send/subscribe destination)을 문서화한다.
    protected fun documentStomp(
        identifier: String,
        endpoint: String,
        block: StompDocumentationDsl.() -> Unit,
    ) {
        StompDocumentationDsl(
            identifier = identifier,
            baseUrl = "http://localhost:$port",
            stompEndpoint = endpoint,
        ).apply(block).execute()
    }

    // 이벤트 발행을 유도하는 헬퍼. HTTP 상태 코드를 반환한다.
    protected fun httpPost(
        path: String,
        jsonBody: String,
    ): Int {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
    }
}
