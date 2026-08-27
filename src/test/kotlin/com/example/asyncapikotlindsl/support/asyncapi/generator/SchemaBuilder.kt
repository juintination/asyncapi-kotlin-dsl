package com.example.asyncapikotlindsl.support.asyncapi.generator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * 문서화할 payload 필드 하나의 정보.
 *
 * 타입은 캡처된 실제 값에서 추론한다. [optional] 이면 payload 에 없어도 검증을 통과한다.
 */
internal data class FieldDescriptor(
    val path: String,
    val description: String,
    val optional: Boolean,
)

/**
 * 선언한 필드 목록과 실제 캡처된 payload 를 대조(validate)하고,
 * 통과하면 AsyncAPI payload 스키마를 만든다(build).
 *
 * Phase 1 은 평평한 객체 payload 만 다룬다 (중첩 obj/array 는 이후).
 */
internal class SchemaBuilder {

    private val objectMapper = jacksonObjectMapper()

    // 선언한 fields 와 실제 payload 를 대조.
    // 선언 안 된 필드가 payload 에 있거나, 선언한 필수 필드가 payload 에 없으면 AssertionError.
    fun validate(
        fields: List<FieldDescriptor>,
        payloadJson: String,
    ) {
        val root = objectMapper.readTree(payloadJson)
        if (!root.isObject) {
            throw AssertionError("객체 페이로드만 지원합니다 (Phase 1)")
        }

        val declaredPaths = fields.map { it.path }.toSet()
        val actualPaths = root.fieldNames().asSequence().toSet()

        val undeclared = actualPaths - declaredPaths
        if (undeclared.isNotEmpty()) {
            throw AssertionError("문서화되지 않은 페이로드 필드: $undeclared — field(\"...\") 로 선언하세요")
        }

        val missing = fields.filterNot { it.optional }.map { it.path }.filterNot { it in actualPaths }
        if (missing.isNotEmpty()) {
            throw AssertionError("선언한 필수 필드가 페이로드에 없습니다: $missing")
        }
    }

    // fields 로부터 payload 스키마(type: object + required + properties)를 만든다.
    // 각 필드 타입은 payloadJson 의 실제 값에서 추론한다.
    fun build(
        fields: List<FieldDescriptor>,
        payloadJson: String,
    ): Map<String, Any> {
        val root = objectMapper.readTree(payloadJson)

        val properties = LinkedHashMap<String, Any>()
        fields.forEach { field ->
            properties[field.path] = linkedMapOf<String, Any>(
                "type" to inferType(root.get(field.path)),
                "description" to field.description,
            )
        }

        val schema = LinkedHashMap<String, Any>()
        schema["type"] = "object"
        val required = fields.filterNot { it.optional }.map { it.path }
        if (required.isNotEmpty()) {
            schema["required"] = required
        }
        schema["properties"] = properties
        return schema
    }

    private fun inferType(
        node: JsonNode?,
    ): String = when {
        node == null || node.isNull -> "string"
        node.isTextual -> "string"
        node.isBoolean -> "boolean"
        node.isIntegralNumber -> "integer"
        node.isNumber -> "number"
        node.isArray -> "array"
        node.isObject -> "object"
        else -> "string"
    }
}
