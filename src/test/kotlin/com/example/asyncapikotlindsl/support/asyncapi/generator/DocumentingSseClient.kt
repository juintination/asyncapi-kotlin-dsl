package com.example.asyncapikotlindsl.support.asyncapi.generator

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

/**
 * 문서화 중 실제 SSE 스트림을 받는 클라이언트.
 *
 * `java.net.http.HttpClient` 만 쓴다 (WebFlux 의존성 없음). [connect] 가 리턴하면 구독이 등록된 상태이고,
 * 이후 가상 스레드에서 `text/event-stream` 의 `data:` 라인을 읽어 payload 문자열로 모은다.
 */
internal class DocumentingSseClient(
    private val baseUrl: String,
) {

    private val httpClient = HttpClient.newHttpClient()
    private val capturedPayloads = LinkedBlockingQueue<String>()

    @Volatile
    private var closed = false
    private var lineStream: Stream<String>? = null
    private var readerThread: Thread? = null

    // SSE 엔드포인트에 연결한다. 200 이 아니면 예외. 리턴 시점엔 구독이 등록돼 있다.
    fun connect(
        path: String,
    ) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Accept", "text/event-stream")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines())
        if (response.statusCode() != HTTP_OK) {
            throw IllegalStateException("SSE 연결에 실패했습니다: HTTP ${response.statusCode()}")
        }

        val stream = response.body()
        lineStream = stream
        readerThread = Thread.ofVirtual()
            .name("documenting-sse-reader")
            .start { readLoop(stream) }
    }

    // 다음 이벤트의 data 를 timeout 까지 기다린다. 못 받으면 예외.
    fun nextPayload(
        timeout: Duration,
    ): String {
        val payload = capturedPayloads.poll(timeout.toMillis(), TimeUnit.MILLISECONDS)
        if (payload == null) {
            throw IllegalStateException("${timeout.toMillis()}ms 안에 SSE 이벤트를 수신하지 못했습니다")
        }
        return payload
    }

    // 스트림과 읽기 스레드를 정리한다.
    fun close() {
        closed = true
        readerThread?.interrupt()
        try {
            lineStream?.close()
        } catch (ignored: Exception) {
            // 스트림 종료 중 발생하는 예외는 무시한다
        }
    }

    private fun readLoop(
        stream: Stream<String>,
    ) {
        val data = StringBuilder()

        try {
            val iterator = stream.iterator()
            while (!closed && iterator.hasNext()) {
                val line = iterator.next()
                when {
                    line.isEmpty() -> {
                        if (data.isNotEmpty()) {
                            capturedPayloads.add(data.toString().trimEnd('\n'))
                            data.setLength(0)
                        }
                    }

                    line.startsWith(FIELD_DATA) ->
                        data.append(line.removePrefix(FIELD_DATA).trim()).append('\n')

                    else -> Unit
                }
            }
        } catch (ignored: Exception) {
            // 연결이 닫히면 읽기 루프를 종료한다
        }
    }

    companion object {
        private const val HTTP_OK = 200
        private const val FIELD_DATA = "data:"
    }
}
