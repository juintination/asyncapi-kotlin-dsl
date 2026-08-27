package com.example.asyncapikotlindsl.support.asyncapi.generator

/**
 * STOMP 채팅 한 건에 대한 스니펫.
 *
 * STOMP 는 전송 destination(`/app/...`)과 구독 destination(`/topic/...`)이 달라서
 * 채널을 **두 개** 만들고 각각에 메시지를 인라인으로 걸고 `send` / `receive` operation 을 건다.
 * 공유 `components` 없음.
 */
internal class StompSnippet(
    private val sendDestination: String,
    private val subscribeDestination: String,
    private val description: String,
    private val send: DocumentedMessage,
    private val receive: DocumentedMessage,
) : AsyncApiSnippet {

    override fun toFragment(): Map<String, Any> {
        val sendChannel = channelNameFromPath(sendDestination)
        val receiveChannel = channelNameFromPath(subscribeDestination)
        val sendKey = send.componentName.replaceFirstChar { it.lowercaseChar() }
        val receiveKey = receive.componentName.replaceFirstChar { it.lowercaseChar() }
        val serverRef = serverRefFor("stomp")

        return linkedMapOf(
            "channels" to linkedMapOf<String, Any>(
                sendChannel to linkedMapOf<String, Any>(
                    "address" to sendDestination,
                    "description" to "$description — 전송",
                    "servers" to listOf(
                        linkedMapOf<String, Any>("\$ref" to serverRef),
                    ),
                    "messages" to linkedMapOf<String, Any>(
                        sendKey to messageNode(send),
                    ),
                ),
                receiveChannel to linkedMapOf<String, Any>(
                    "address" to subscribeDestination,
                    "description" to "$description — 구독",
                    "servers" to listOf(
                        linkedMapOf<String, Any>("\$ref" to serverRef),
                    ),
                    "messages" to linkedMapOf<String, Any>(
                        receiveKey to messageNode(receive),
                    ),
                ),
            ),
            "operations" to linkedMapOf<String, Any>(
                "${sendChannel}Send" to operationNode("send", sendChannel, sendKey, send.summary),
                "${receiveChannel}Receive" to operationNode("receive", receiveChannel, receiveKey, receive.summary),
            ),
        )
    }
}
