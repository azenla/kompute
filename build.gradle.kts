plugins {
  kotlin("jvm") version "1.3.72" apply false
  kotlin("plugin.serialization") version "1.3.72" apply false

  id("com.diffplug.gradle.spotless") version "3.27.2"
}

repositories {
  jcenter()
}

spotless {
  format("misc") {
    target("**/*.md", "**/.gitignore")

    trimTrailingWhitespace()
    endWithNewline()
  }

  kotlin {
    target(fileTree("src") {
      include("**/*.kt")
    })

    ktlint().userData(mapOf(
      "indent_size" to "2",
      "continuation_indent_size" to "2"
    ))
    endWithNewline()
  }

  kotlinGradle {
    ktlint().userData(mapOf(
      "indent_size" to "2",
      "continuation_indent_size" to "2"
    ))
    endWithNewline()
  }
}

tasks.withType<Wrapper> {
  gradleVersion = "6.5"
  distributionType = Wrapper.DistributionType.ALL
}
