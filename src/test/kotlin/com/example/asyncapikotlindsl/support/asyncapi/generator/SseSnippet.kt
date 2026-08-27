package com.example.asyncapikotlindsl.support.asyncapi.generator

/**
 * SSE 채널 하나에 대한 스니펫. `receive` action 의 channel/operation/message 조각을 만든다.
 *
 * @param payloadSchema [SchemaBuilder.build] 결과
 * @param examplePayload 테스트에서 실제로 수신한 payload. `message.examples[].payload` 로 들어간다
 */
internal class SseSnippet(
    private val channelPath: String,
    private val channelDescription: String,
    private val operationSummary: String?,
    private val componentName: String,
    private val payloadSchema: Map<String, Any>,
    private val examplePayload: Map<String, Any>,
) {

    fun toFragment(): Map<String, Any> {
        val channelName = channelPath.trim('/').substringAfterLast('/').ifEmpty { "channel" }
        val messageKey = componentName.replaceFirstChar { it.lowercaseChar() }
        val operationKey = "receive$componentName"

        val operationNode = linkedMapOf<String, Any>(
            "action" to "receive",
            "channel" to linkedMapOf<String, Any>(
                "\$ref" to "#/channels/$channelName",
            ),
            "messages" to listOf(
                linkedMapOf<String, Any>(
                    "\$ref" to "#/channels/$channelName/messages/$messageKey",
                ),
            ),
        )
        operationSummary?.let { operationNode["summary"] = it }

        return linkedMapOf(
            "channels" to linkedMapOf<String, Any>(
                channelName to linkedMapOf<String, Any>(
                    "address" to channelPath,
                    "description" to channelDescription,
                    "messages" to linkedMapOf<String, Any>(
                        messageKey to linkedMapOf<String, Any>(
                            "\$ref" to "#/components/messages/$componentName",
                        ),
                    ),
                ),
            ),
            "operations" to linkedMapOf<String, Any>(
                operationKey to operationNode,
            ),
            "components" to linkedMapOf<String, Any>(
                "messages" to linkedMapOf<String, Any>(
                    componentName to linkedMapOf<String, Any>(
                        "contentType" to "application/json",
                        "payload" to payloadSchema,
                        "examples" to listOf(
                            linkedMapOf<String, Any>(
                                "name" to "default",
                                "payload" to examplePayload,
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}
