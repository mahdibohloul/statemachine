plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25"
  id("io.spring.dependency-management") version "1.1.7"

  id("com.vanniktech.maven.publish") version "0.34.0"
  id("com.diffplug.spotless") version "7.2.1"
  id("io.gitlab.arturbosch.detekt") version "1.23.6"

  `java-library`
}

group = "io.github.mahdibohloul"
version = "0.9.0"
description = "statemachine"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.projectreactor:reactor-core:3.7.11")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.4")
  implementation("io.projectreactor.addons:reactor-extra:3.5.3")
  implementation("org.springframework.boot:spring-boot-autoconfigure:3.5.6")
  implementation("box.tapsi.libs:utilities-starter:0.9.2")
  implementation("org.springframework:spring-tx:6.2.11")
  implementation("org.slf4j:slf4j-api:2.0.17")

  testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.5")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.25")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
  testImplementation("io.projectreactor:reactor-test:3.7.11")

  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()

  pom {
    name.set("statemachine")
    description.set("A lightweight and reactive state machine library for Kotlin and Java applications.")
    url.set("https://github.com/mahdibohloul/statemachine")
    licenses {
      license {
        name.set("MIT License")
        url.set("https://opensource.org/licenses/MIT")
        distribution.set("repo")
      }
    }
    developers {
      developer {
        id.set("mahdibohloul")
        name.set("Mahdi Bohloul")
        email.set("mahdiibohloul@gmail.com")
        url.set("https://github.com/mahdibohloul/")
      }
    }
    scm {
      url.set("https://github.com/mahdibohloul/statemachine")
    }
  }
}

spotless {
  kotlin {
    target("src/**/*.kt")
    ktlint()
      .editorConfigOverride(
        mapOf(
          "indent_size" to 2,
          "ktlint_standard_filename" to "disabled",
          "ktlint_standard_max-line-length" to "120"
        )
      )
    trimTrailingWhitespace()
    leadingTabsToSpaces()
    endWithNewline()
  }
}

detekt {
  buildUponDefaultConfig = true
  allRules = true
  config.setFrom("$projectDir/detekt.yml")
  baseline = file("$projectDir/detekt-baseline.xml")
}

tasks.register("verifyReadmeContent") {
  doLast {
    val readmeFile = file("README.md")
    val content = readmeFile.readText()

    // List of checks
    val checks = listOf(
      Check("group ID", """<groupId>${project.group}</groupId>"""),
      Check("version", """<version>${project.version}</version>"""),
    )

    val errors = checks.mapNotNull { check ->
      if (!content.contains(check.expectedValue)) {
        "Missing or incorrect ${check.name}: ${check.expectedValue}"
      } else null
    }

    if (errors.isNotEmpty()) {
      throw GradleException(
        """
                README content verification failed!
                ${errors.joinToString("\n")}
                Please update the README.md with correct values
            """.trimIndent()
      )
    }
  }
}

tasks.check {
  dependsOn("verifyReadmeContent")
}

data class Check(val name: String, val expectedValue: String)
