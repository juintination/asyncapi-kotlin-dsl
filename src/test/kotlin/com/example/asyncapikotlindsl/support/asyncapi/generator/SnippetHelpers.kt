package com.example.asyncapikotlindsl.support.asyncapi.generator

/**
 * 한 방향(send 또는 receive)에 대한 문서화된 메시지.
 *
 * @param payloadSchema [SchemaBuilder.build] 결과
 * @param examplePayload 실제로 주고받은 payload
 */
internal data class DocumentedMessage(
    val componentName: String,
    val summary: String?,
    val payloadSchema: Map<String, Any>,
    val examplePayload: Map<String, Any>,
)

// path 세그먼트를 camelCase 로 이어 채널 이름을 만든다. /ws/chat -> wsChat, /app/chat -> appChat.
internal fun channelNameFromPath(
    path: String,
): String {
    val segments = path.trim('/').split(Regex("[/_-]")).filter { it.isNotEmpty() }
    if (segments.isEmpty()) {
        return "channel"
    }
    return segments.first() + segments.drop(1).joinToString("") { seg ->
        seg.replaceFirstChar { it.uppercase() }
    }
}

internal fun operationNode(
    action: String,
    channelName: String,
    messageKey: String,
    summary: String?,
): Map<String, Any> {
    val node = linkedMapOf<String, Any>(
        "action" to action,
        "channel" to linkedMapOf<String, Any>(
            "\$ref" to "#/channels/$channelName",
        ),
        "messages" to listOf(
            linkedMapOf<String, Any>(
                "\$ref" to "#/channels/$channelName/messages/$messageKey",
            ),
        ),
    )
    summary?.let { node["summary"] = it }
    return node
}

internal fun messageNode(
    message: DocumentedMessage,
): Map<String, Any> = linkedMapOf(
    "contentType" to "application/json",
    "payload" to message.payloadSchema,
    "examples" to listOf(
        linkedMapOf<String, Any>(
            "name" to "default",
            "payload" to message.examplePayload,
        ),
    ),
)
