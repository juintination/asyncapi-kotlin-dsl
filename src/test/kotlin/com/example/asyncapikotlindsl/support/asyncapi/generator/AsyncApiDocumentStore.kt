package com.example.asyncapikotlindsl.support.asyncapi.generator

import com.fasterxml.jackson.core.type.TypeReference
import java.io.File

/**
 * 스니펫 조각을 `build/asyncapi-snippets/` 에 쌓고([writeFragment]),
 * 전부 deep-merge 해 `build/asyncapi/asyncapi.yaml` 로 조립한다([assemble]).
 *
 * pangyeori-be 의 "REST Docs 조각 쓰기 + openapi3 태스크 조립" 두 단계에 대응한다.
 * Gradle Test 의 workingDir 이 프로젝트 루트라 경로는 상대 경로 상수로 충분하다.
 */
internal class AsyncApiDocumentStore {

    private val yamlMapper = asyncApiYamlMapper()

    fun writeFragment(
        name: String,
        fragment: Map<String, Any>,
    ) {
        val directory = File(SNIPPETS_DIR)
        directory.mkdirs()
        yamlMapper.writeValue(File(directory, "$name.yaml"), fragment)
    }

    // 조각이 하나도 없으면 예외.
    fun assemble() {
        val fragmentFiles = File(SNIPPETS_DIR)
            .listFiles { file -> file.isFile && file.extension == "yaml" }
            ?.sortedBy { it.name }
            .orEmpty()

        if (fragmentFiles.isEmpty()) {
            throw IllegalStateException("조립할 스니펫 조각이 없습니다: $SNIPPETS_DIR")
        }

        val document = baseDocument()
        fragmentFiles.forEach { file ->
            deepMerge(document, yamlMapper.readValue(file, MAP_TYPE))
        }

        val output = File(OUTPUT_PATH)
        output.parentFile?.mkdirs()
        yamlMapper.writeValue(output, document)
    }

    private fun baseDocument(): MutableMap<String, Any> = linkedMapOf(
        "asyncapi" to ASYNCAPI_VERSION,
        "info" to linkedMapOf<String, Any>(
            "title" to DOC_TITLE,
            "version" to DOC_VERSION,
        ),
        "servers" to linkedMapOf<String, Any>(
            SERVER_NAME to linkedMapOf<String, Any>(
                "host" to SERVER_HOST,
                "protocol" to SERVER_PROTOCOL,
            ),
        ),
    )

    @Suppress("UNCHECKED_CAST")
    private fun deepMerge(
        target: MutableMap<String, Any>,
        source: Map<String, Any>,
    ) {
        source.forEach { (key, value) ->
            val existing = target[key]
            if (existing is MutableMap<*, *> && value is Map<*, *>) {
                deepMerge(existing as MutableMap<String, Any>, value as Map<String, Any>)
            } else {
                target[key] = value
            }
        }
    }

    companion object {
        private const val SNIPPETS_DIR = "build/asyncapi-snippets"
        private const val OUTPUT_PATH = "build/asyncapi/asyncapi.yaml"
        private const val ASYNCAPI_VERSION = "3.0.0"
        private const val DOC_TITLE = "API Document"
        private const val DOC_VERSION = "1.0.0"
        private const val SERVER_NAME = "production"
        private const val SERVER_HOST = "localhost"
        private const val SERVER_PROTOCOL = "https"
        private val MAP_TYPE = object : TypeReference<LinkedHashMap<String, Any>>() {}
    }
}
