plugins {
  application

  kotlin("jvm")
  kotlin("plugin.serialization")

  id("com.palantir.docker")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":kompute-runtime"))
}

application {
  mainClassName = "MainKt"
}

kotlin.sourceSets["main"].apply {
  kotlin.srcDir("src/main/functions")
}

docker {
  name = "docker.pkg.github.com/kendfinger/kompute/examples/small-workload"
  files(tasks.distTar)
}
