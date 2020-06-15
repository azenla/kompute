plugins {
  kotlin("jvm")
}

repositories {
  jcenter()
}

val ktorVersion = "1.3.2"
val kotlinSerializationVersion = "0.20.0"
val kotlinCoroutinesVersion = "1.3.7"

fun ktor(name: String): String = "io.ktor:ktor-$name:$ktorVersion"
fun kotlinx(name: String, version: String): String = "org.jetbrains.kotlinx:kotlinx-$name:$version"

dependencies {
  api(kotlin("stdlib-jdk8"))

  api(ktor("server-core"))
  api(ktor("server-netty"))
  api(ktor("serialization"))

  api(ktor("client-core"))
  api(ktor("client-apache"))
  api(ktor("client-serialization"))

  api(kotlinx("serialization-runtime", kotlinSerializationVersion))
  api(kotlinx("coroutines-core", kotlinCoroutinesVersion))
}
