package com.example.asyncapikotlindsl.support.asyncapi.generator

import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.concurrent.CompletionStage
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * 문서화 중 실제 WebSocket 세션을 여는 클라이언트.
 *
 * `java.net.http.WebSocket`(JDK 내장)만 쓴다. 조각난 텍스트 프레임은 이어붙여 한 메시지로 큐에 넣는다.
 */
internal class DocumentingWebSocketSession(
    private val baseUrl: String,
) {

    private val messages = LinkedBlockingQueue<String>()
    private val buffer = StringBuilder()
    private var webSocket: WebSocket? = null

    // baseUrl 의 http(s) 를 ws(s) 로 바꿔 접속한다.
    fun connect(
        path: String,
    ) {
        val uri = URI.create(baseUrl.replaceFirst("http", "ws") + path)
        webSocket = HttpClient.newHttpClient()
            .newWebSocketBuilder()
            .buildAsync(uri, Listener())
            .join()
    }

    // 텍스트 메시지를 보낸다.
    fun send(
        text: String,
    ) {
        webSocket!!.sendText(text, true).join()
    }

    // 다음 텍스트 메시지를 timeout 까지 기다린다. 못 받으면 예외.
    fun nextMessage(
        timeout: Duration,
    ): String {
        val message = messages.poll(timeout.toMillis(), TimeUnit.MILLISECONDS)
        if (message == null) {
            throw IllegalStateException("${timeout.toMillis()}ms 안에 WebSocket 메시지를 수신하지 못했습니다")
        }
        return message
    }

    // 세션을 닫는다.
    fun close() {
        try {
            webSocket?.sendClose(WebSocket.NORMAL_CLOSURE, "done")?.join()
        } catch (ignored: Exception) {
            // 종료 중 예외는 무시한다
        }
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
            buffer.append(data)
            if (last) {
                messages.add(buffer.toString())
                buffer.setLength(0)
            }
            return null
        }
    }
}
