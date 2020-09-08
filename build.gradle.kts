plugins {
  kotlin("jvm") version "1.4.0" apply false
  kotlin("plugin.serialization") version "1.4.0" apply false

  id("com.diffplug.spotless") version "5.3.0"
  id("com.palantir.docker") version "0.25.0" apply false
}

repositories {
  mavenCentral()
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
  gradleVersion = "6.6.1"
  distributionType = Wrapper.DistributionType.ALL
}
