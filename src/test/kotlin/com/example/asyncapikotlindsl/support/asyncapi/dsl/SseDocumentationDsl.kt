package com.example.asyncapikotlindsl.support.asyncapi.dsl

import com.example.asyncapikotlindsl.support.asyncapi.generator.AsyncApiDocumentStore
import com.example.asyncapikotlindsl.support.asyncapi.generator.DocumentingSseClient
import com.example.asyncapikotlindsl.support.asyncapi.generator.FieldDescriptor
import com.example.asyncapikotlindsl.support.asyncapi.generator.SchemaBuilder
import com.example.asyncapikotlindsl.support.asyncapi.generator.SseSnippet
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration

/**
 * `receive<T> { }` 블록의 리시버 (SSE 전용).
 *
 * 수신할 메시지 타입 `T` 의 필드를 [field] 로 선언하고, [trigger] 로 서버 발행을 유도하고,
 * [verify] 로 역직렬화한 payload 값을 검증한다.
 */
@AsyncApiDocsDslMarker
class SseReceiveSpec<T : Any> internal constructor(
    internal val payloadType: Class<T>,
    internal val componentName: String,
) {

    internal val fields = mutableListOf<FieldSpec>()
    internal var triggerBlock: (() -> Unit)? = null
    internal var verifyBlock: ((T) -> Unit)? = null

    fun field(
        path: String,
        description: String,
    ) = FieldSpec(path, description).also { fields += it }

    fun trigger(
        block: () -> Unit,
    ) {
        triggerBlock = block
    }

    fun verify(
        block: (T) -> Unit,
    ) {
        verifyBlock = block
    }
}

/**
 * `documentSse("resource/action") { ... }` 블록의 리시버.
 *
 * `channel(...)` 과 `receive<T> { }` 는 필수다. [execute] 가 SSE 연결 → 발행 → 수신 → 검증 →
 * 스니펫 생성 → 조립을 순서대로 수행하며, `verify` 나 field 대조가 실패하면 스니펫은 생성되지 않는다.
 */
@AsyncApiDocsDslMarker
class SseDocumentationDsl internal constructor(
    private val identifier: String,
    private val baseUrl: String,
) {

    private val objectMapper = jacksonObjectMapper()
    private val schemaBuilder = SchemaBuilder()
    private val store = AsyncApiDocumentStore()

    private var channelSpec: ChannelSpec? = null
    private val operationSpec = OperationSpec()
    private var receiveSpec: SseReceiveSpec<*>? = null

    fun channel(
        path: String,
        protocol: String,
        description: String,
    ) {
        channelSpec = ChannelSpec(path, protocol, description)
    }

    fun operation(
        block: OperationSpec.() -> Unit,
    ) {
        operationSpec.apply(block)
    }

    inline fun <reified T : Any> receive(
        noinline block: SseReceiveSpec<T>.() -> Unit,
    ) {
        receiveInternal(
            payloadType = T::class.java,
            componentName = T::class.simpleName ?: "Event",
            block = block,
        )
    }

    @PublishedApi
    internal fun <T : Any> receiveInternal(
        payloadType: Class<T>,
        componentName: String,
        block: SseReceiveSpec<T>.() -> Unit,
    ) {
        receiveSpec = SseReceiveSpec(payloadType, componentName).apply(block)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun execute() {
        val channel = channelSpec
            ?: throw IllegalStateException("channel(...) 선언이 필요합니다")
        val receive = receiveSpec as? SseReceiveSpec<Any>
            ?: throw IllegalStateException("receive<T> { } 선언이 필요합니다")

        val client = DocumentingSseClient(baseUrl)
        val payloadJson = try {
            client.connect(channel.path)
            receive.triggerBlock?.invoke()
            client.nextPayload(EVENT_TIMEOUT)
        } finally {
            client.close()
        }

        val descriptors = receive.fields.map { spec ->
            FieldDescriptor(
                path = spec.path,
                description = spec.description,
                optional = spec.optional,
            )
        }

        val payload = objectMapper.readValue(payloadJson, receive.payloadType)
        receive.verifyBlock?.invoke(payload)
        schemaBuilder.validate(descriptors, payloadJson)

        // 캡처한 실제 payload 는 스키마와 별개로 message.examples 에도 들어간다
        val fragment = SseSnippet(
            channelPath = channel.path,
            channelProtocol = channel.protocol,
            channelDescription = channel.description,
            operationSummary = operationSpec.summary,
            componentName = receive.componentName,
            payloadSchema = schemaBuilder.build(descriptors, payloadJson),
            examplePayload = objectMapper.readValue(payloadJson, EXAMPLE_TYPE),
        ).toFragment()

        store.writeFragment(identifier.replace('/', '-'), fragment)
        store.assemble()
    }

    companion object {
        private val EVENT_TIMEOUT: Duration = Duration.ofSeconds(10)
        private val EXAMPLE_TYPE = object : TypeReference<LinkedHashMap<String, Any>>() {}
    }
}
