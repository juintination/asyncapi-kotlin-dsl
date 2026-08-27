package com.example.asyncapikotlindsl.support.asyncapi.dsl

internal data class ChannelSpec(
    val path: String,
    val protocol: String,
    val description: String,
)

/**
 * `operation { }` 로 선언하는 오퍼레이션 메타데이터.
 *
 * 지금은 `summary` 만 담지만 이후 tags, bindings 등이 추가될 자리다.
 */
@AsyncApiDocsDslMarker
class OperationSpec internal constructor() {

    internal var summary: String? = null

    fun summary(
        value: String,
    ) {
        summary = value
    }
}

/**
 * `field(path, description)` 로 선언한 payload 필드 하나.
 *
 * [optional] 이면 payload 에 없어도 검증을 통과한다. 타입은 캡처된 실제 값에서 추론한다.
 */
@AsyncApiDocsDslMarker
class FieldSpec internal constructor(
    internal val path: String,
    internal val description: String,
) {

    internal var optional: Boolean = false

    fun optional() = apply { optional = true }
}

/**
 * `send<T>(summary) { }` 블록의 리시버 (WebSocket / STOMP 공용).
 *
 * 클라이언트가 보낼 메시지 타입 `T` 의 필드를 [field] 로 선언하고, [payload] 로 실제 보낼 JSON 을 지정한다.
 */
@AsyncApiDocsDslMarker
class SendSpec<T : Any> internal constructor(
    internal val componentName: String,
    internal val summary: String?,
) {

    internal val fields = mutableListOf<FieldSpec>()
    internal var payloadJson: String? = null

    fun field(
        path: String,
        description: String,
    ) = FieldSpec(path, description).also { fields += it }

    fun payload(
        json: String,
    ) {
        payloadJson = json
    }
}

/**
 * `receive<T>(summary) { }` 블록의 리시버 (WebSocket / STOMP 공용, trigger 없음).
 *
 * 서버가 보낼 메시지 타입 `T` 의 필드를 [field] 로 선언하고, [verify] 로 수신 payload 값을 검증한다.
 */
@AsyncApiDocsDslMarker
class ReceiveMessageSpec<T : Any> internal constructor(
    internal val payloadType: Class<T>,
    internal val componentName: String,
    internal val summary: String?,
) {

    internal val fields = mutableListOf<FieldSpec>()
    internal var verifyBlock: ((T) -> Unit)? = null

    fun field(
        path: String,
        description: String,
    ) = FieldSpec(path, description).also { fields += it }

    fun verify(
        block: (T) -> Unit,
    ) {
        verifyBlock = block
    }
}
