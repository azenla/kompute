plugins {
  kotlin("jvm")
}

repositories {
  mavenCentral()
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

val ktorVersion = "1.4.0"
val kotlinSerializationVersion = "1.0-M1-1.4.0-rc"
val kotlinCoroutinesVersion = "1.3.9"

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

  api("org.slf4j:slf4j-api:1.7.30")
  api("org.slf4j:slf4j-simple:1.7.30")

  api("io.github.resilience4j:resilience4j-all:1.5.0")
  api("io.github.resilience4j:resilience4j-kotlin:1.5.0")
}

tasks.compileKotlin {
  kotlinOptions.jvmTarget = "11"
}
