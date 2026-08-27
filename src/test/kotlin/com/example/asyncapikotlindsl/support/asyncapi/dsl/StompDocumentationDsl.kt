package com.example.asyncapikotlindsl.support.asyncapi.dsl

import com.example.asyncapikotlindsl.support.asyncapi.generator.AsyncApiDocumentStore
import com.example.asyncapikotlindsl.support.asyncapi.generator.DocumentedMessage
import com.example.asyncapikotlindsl.support.asyncapi.generator.DocumentingStompSession
import com.example.asyncapikotlindsl.support.asyncapi.generator.FieldDescriptor
import com.example.asyncapikotlindsl.support.asyncapi.generator.SchemaBuilder
import com.example.asyncapikotlindsl.support.asyncapi.generator.StompSnippet
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration

/**
 * `documentStomp("resource/action", endpoint) { ... }` 블록의 리시버.
 *
 * `destination(...)`, `send<T> { }`, `receive<T> { }` 모두 필수. [execute] 가 STOMP 연결 → 구독 →
 * 전송 → 수신 → verify → 양방향 field 대조 → 스니펫 생성/조립을 수행한다.
 */
@AsyncApiDocsDslMarker
class StompDocumentationDsl internal constructor(
    private val identifier: String,
    private val baseUrl: String,
    private val stompEndpoint: String,
) {

    private val objectMapper = jacksonObjectMapper()
    private val schemaBuilder = SchemaBuilder()
    private val store = AsyncApiDocumentStore()

    private var sendDestination: String? = null
    private var subscribeDestination: String? = null
    private var description: String = ""
    private var sendSpec: SendSpec<*>? = null
    private var receiveSpec: ReceiveMessageSpec<*>? = null

    fun destination(
        sendTo: String,
        subscribeTo: String,
        description: String,
    ) {
        sendDestination = sendTo
        subscribeDestination = subscribeTo
        this.description = description
    }

    inline fun <reified T : Any> send(
        summary: String? = null,
        noinline block: SendSpec<T>.() -> Unit,
    ) {
        sendInternal(T::class.simpleName ?: "Request", summary, block)
    }

    inline fun <reified T : Any> receive(
        summary: String? = null,
        noinline block: ReceiveMessageSpec<T>.() -> Unit,
    ) {
        receiveInternal(T::class.java, T::class.simpleName ?: "Response", summary, block)
    }

    @PublishedApi
    internal fun <T : Any> sendInternal(
        componentName: String,
        summary: String?,
        block: SendSpec<T>.() -> Unit,
    ) {
        sendSpec = SendSpec<T>(componentName, summary).apply(block)
    }

    @PublishedApi
    internal fun <T : Any> receiveInternal(
        payloadType: Class<T>,
        componentName: String,
        summary: String?,
        block: ReceiveMessageSpec<T>.() -> Unit,
    ) {
        receiveSpec = ReceiveMessageSpec(payloadType, componentName, summary).apply(block)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun execute() {
        val sendTo = sendDestination
            ?: throw IllegalStateException("destination(...) 선언이 필요합니다")
        val subscribeTo = subscribeDestination
            ?: throw IllegalStateException("destination(...) 선언이 필요합니다")
        val send = sendSpec as? SendSpec<Any>
            ?: throw IllegalStateException("send<T> { } 선언이 필요합니다")
        val receive = receiveSpec as? ReceiveMessageSpec<Any>
            ?: throw IllegalStateException("receive<T> { } 선언이 필요합니다")
        val sendJson = send.payloadJson
            ?: throw IllegalStateException("send { payload(\"...\") } 선언이 필요합니다")

        val session = DocumentingStompSession(baseUrl)
        val responseJson = try {
            session.connect(stompEndpoint)
            session.subscribe(subscribeTo)
            session.send(sendTo, sendJson)
            session.nextMessageBody(MESSAGE_TIMEOUT)
        } finally {
            session.close()
        }

        val received = objectMapper.readValue(responseJson, receive.payloadType)
        receive.verifyBlock?.invoke(received)

        val sendDescriptors = send.fields.map { FieldDescriptor(it.path, it.description, it.optional) }
        val receiveDescriptors = receive.fields.map { FieldDescriptor(it.path, it.description, it.optional) }
        schemaBuilder.validate(sendDescriptors, sendJson)
        schemaBuilder.validate(receiveDescriptors, responseJson)

        val fragment = StompSnippet(
            sendDestination = sendTo,
            subscribeDestination = subscribeTo,
            description = description,
            send = DocumentedMessage(
                componentName = send.componentName,
                summary = send.summary,
                payloadSchema = schemaBuilder.build(sendDescriptors, sendJson),
                examplePayload = objectMapper.readValue(sendJson, EXAMPLE_TYPE),
            ),
            receive = DocumentedMessage(
                componentName = receive.componentName,
                summary = receive.summary,
                payloadSchema = schemaBuilder.build(receiveDescriptors, responseJson),
                examplePayload = objectMapper.readValue(responseJson, EXAMPLE_TYPE),
            ),
        ).toFragment()

        store.writeFragment(identifier.replace('/', '-'), fragment)
        store.assemble()
    }

    companion object {
        private val MESSAGE_TIMEOUT: Duration = Duration.ofSeconds(10)
        private val EXAMPLE_TYPE = object : TypeReference<LinkedHashMap<String, Any>>() {}
    }
}
