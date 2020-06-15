plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
}

repositories {
  jcenter()
}

dependencies {
  implementation(project(":kompute-runtime"))
}

kotlin.sourceSets["main"].apply {
  kotlin.srcDir("src/main/functions")
}
