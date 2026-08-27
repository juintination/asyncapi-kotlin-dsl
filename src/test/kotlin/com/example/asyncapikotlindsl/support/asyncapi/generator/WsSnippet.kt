package com.example.asyncapikotlindsl.support.asyncapi.generator

/**
 * raw WebSocket 채널 하나에 대한 스니펫.
 *
 * 채널에 request/response 두 메시지를 **인라인**으로 걸고, `send` + `receive` 두 operation 을 만든다.
 * 공유 `components` 를 만들지 않아 다른 조각과 병합 충돌이 없다.
 */
internal class WsSnippet(
    private val channelPath: String,
    private val channelProtocol: String,
    private val channelDescription: String,
    private val send: DocumentedMessage,
    private val receive: DocumentedMessage,
) : AsyncApiSnippet {

    override fun toFragment(): Map<String, Any> {
        val channelName = channelNameFromPath(channelPath)
        val sendKey = send.componentName.replaceFirstChar { it.lowercaseChar() }
        val receiveKey = receive.componentName.replaceFirstChar { it.lowercaseChar() }

        return linkedMapOf(
            "channels" to linkedMapOf<String, Any>(
                channelName to linkedMapOf<String, Any>(
                    "address" to channelPath,
                    "description" to channelDescription,
                    "servers" to listOf(
                        linkedMapOf<String, Any>("\$ref" to serverRefFor(channelProtocol)),
                    ),
                    "messages" to linkedMapOf<String, Any>(
                        sendKey to messageNode(send),
                        receiveKey to messageNode(receive),
                    ),
                ),
            ),
            "operations" to linkedMapOf<String, Any>(
                "${channelName}Send" to operationNode("send", channelName, sendKey, send.summary),
                "${channelName}Receive" to operationNode("receive", channelName, receiveKey, receive.summary),
            ),
        )
    }
}
