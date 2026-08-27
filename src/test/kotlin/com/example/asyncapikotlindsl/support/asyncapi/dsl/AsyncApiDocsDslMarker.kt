package com.example.asyncapikotlindsl.support.asyncapi.dsl

/**
 * DSL 블록이 바깥 블록의 메서드를 실수로 호출하지 못하도록 스코프를 격리한다.
 *
 * 예: `receive { }` 안에서 `channel()` 을 호출하면 컴파일 에러.
 */
@DslMarker
annotation class AsyncApiDocsDslMarker
