package com.example.asyncapikotlindsl.support.asyncapi.generator

import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.concurrent.CompletionStage
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal data class StompFrame(
    val command: String,
    val headers: Map<String, String>,
    val body: String,
)

/**
 * 문서화 중 실제 STOMP 세션을 여는 클라이언트.
 *
 * `java.net.http.WebSocket`(JDK 내장) 위에 최소 STOMP 1.2 프레임(CONNECT/SUBSCRIBE/SEND/MESSAGE)을
 * 직접 인코딩·디코딩한다. Spring STOMP 클라이언트 의존성을 쓰지 않는다.
 */
internal class DocumentingStompSession(
    private val baseUrl: String,
) {

    private val rawText = StringBuilder()
    private val frames = LinkedBlockingQueue<StompFrame>()
    private val subscriptionId = AtomicInteger()
    private var webSocket: WebSocket? = null

    fun connect(
        endpoint: String,
    ) {
        val uri = URI.create(baseUrl.replaceFirst("http", "ws") + endpoint)
        webSocket = HttpClient.newHttpClient()
            .newWebSocketBuilder()
            .subprotocols("v12.stomp")
            .buildAsync(uri, Listener())
            .join()

        sendFrame("CONNECT", mapOf("accept-version" to "1.2", "host" to "localhost"))
        val connected = nextFrame(HANDSHAKE_TIMEOUT)
        if (connected.command != "CONNECTED") {
            throw IllegalStateException("STOMP CONNECT 실패: ${connected.command} ${connected.body}")
        }
    }

    fun subscribe(
        destination: String,
    ) {
        sendFrame(
            "SUBSCRIBE",
            mapOf("id" to "sub-${subscriptionId.incrementAndGet()}", "destination" to destination),
        )
        // SimpleBroker 가 SUBSCRIBE RECEIPT 를 보장하지 않아, 등록이 반영될 시간을 짧게 준다.
        Thread.sleep(SUBSCRIBE_SETTLE_MILLIS)
    }

    fun send(
        destination: String,
        body: String,
    ) {
        sendFrame(
            "SEND",
            mapOf("destination" to destination, "content-type" to "application/json"),
            body,
        )
    }

    // 다음 MESSAGE 프레임의 body 를 timeout 까지 기다린다.
    fun nextMessageBody(
        timeout: Duration,
    ): String {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            val waitMillis = ((deadline - System.nanoTime()) / 1_000_000).coerceAtLeast(1)
            val frame = frames.poll(waitMillis, TimeUnit.MILLISECONDS) ?: break
            if (frame.command == "MESSAGE") {
                return frame.body
            }
        }
        throw IllegalStateException("${timeout.toMillis()}ms 안에 STOMP MESSAGE 를 수신하지 못했습니다")
    }

    fun close() {
        try {
            sendFrame("DISCONNECT", emptyMap())
            webSocket?.sendClose(WebSocket.NORMAL_CLOSURE, "done")?.join()
        } catch (ignored: Exception) {
            // 종료 중 예외는 무시한다
        }
    }

    private fun sendFrame(
        command: String,
        headers: Map<String, String>,
        body: String = "",
    ) {
        val text = buildString {
            append(command).append('\n')
            headers.forEach { (key, value) -> append(key).append(':').append(value).append('\n') }
            append('\n')
            append(body)
            append(NUL)
        }
        webSocket!!.sendText(text, true).join()
    }

    private fun nextFrame(
        timeout: Duration,
    ): StompFrame = frames.poll(timeout.toMillis(), TimeUnit.MILLISECONDS)
        ?: throw IllegalStateException("${timeout.toMillis()}ms 안에 STOMP 프레임을 수신하지 못했습니다")

    private fun drain() {
        synchronized(rawText) {
            var end = rawText.indexOf(NUL.toString())
            while (end >= 0) {
                val raw = rawText.substring(0, end)
                rawText.delete(0, end + 1)
                if (raw.isNotBlank()) {
                    frames.add(parse(raw))
                }
                end = rawText.indexOf(NUL.toString())
            }
        }
    }

    private fun parse(
        raw: String,
    ): StompFrame {
        val headerEnd = raw.indexOf("\n\n")
        val head = if (headerEnd >= 0) raw.substring(0, headerEnd) else raw
        val body = if (headerEnd >= 0) raw.substring(headerEnd + 2) else ""
        val lines = head.split("\n")
        val headers = lines.drop(1)
            .filter { it.contains(':') }
            .associate { it.substringBefore(':') to it.substringAfter(':') }
        return StompFrame(lines.first().trim(), headers, body)
    }

    private inner class Listener : WebSocket.Listener {

        override fun onOpen(
            webSocket: WebSocket,
        ) {
            webSocket.request(Long.MAX_VALUE)
        }

        override fun onText(
            webSocket: WebSocket,
            data: CharSequence,
            last: Boolean,
        ): CompletionStage<*>? {
            synchronized(rawText) { rawText.append(data) }
            drain()
            return null
        }
    }

    companion object {
        private const val NUL = '\u0000'
        private val HANDSHAKE_TIMEOUT: Duration = Duration.ofSeconds(5)
        private const val SUBSCRIBE_SETTLE_MILLIS = 200L
    }
}
