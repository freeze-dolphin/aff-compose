# Aff Compose

Compose your [Arcaea](https://arcaea.lowiro.com/) chart by Kotlin DSL.

## Features

1. Song and chart information generation.

2. milliseconds based mapping API.

This fork is intended to make special effects instead of composing playable chart completely using kotlin. So
the [Bar api](https://github.com/Arcaea-Infinity/aff-compose/blob/master/src/commonMain/kotlin/com/tairitsu/compose/arcaea/dsl/Bar.kt) is
deprecated

## Installation

[![](https://jitpack.io/v/freeze-dolphin/aff-compose.svg)](https://jitpack.io/#freeze-dolphin/aff-compose)

```kotlin

val affComposeVersion = "eed8e2ad78" // the hash / tag

repositories {
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.freeze-dolphin:aff-compose:${affComposeVersion}")
}
```

## Examples

1. [DemoMap](src/test/kotlin/com/tairitsu/compose/arcaea/DemoMap.kt).
   This file shows simple usage of the apis and how to serialize the chart into String and Json.

2. [ZZM - Composing Dream](https://github.com/Arcaea-Infinity/aff-compose/blob/master/src/jvmTest/kotlin/com/tairitsu/compose/arcaea/DemoMap.kt)
   This is the original DemoMap made by [@Eric_Lian](https://github.com/ExerciseBook), which uses the deprecated Bar dsl.  
   It's currently unavailable in this fork because a lot breaking changes have been made. But you can still refer to it and see more usages. 