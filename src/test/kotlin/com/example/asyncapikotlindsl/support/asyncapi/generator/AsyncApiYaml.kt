package com.example.asyncapikotlindsl.support.asyncapi.generator

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

internal fun asyncApiYamlMapper() = YAMLMapper.builder()
    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    .build()
    .apply { registerKotlinModule() }
