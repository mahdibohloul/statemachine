import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  kotlin("jvm") version "2.2.21"
  kotlin("plugin.spring") version "2.2.21"

  id("com.vanniktech.maven.publish") version "0.34.0"
  id("com.diffplug.spotless") version "7.2.1"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"

  `java-library`
}

group = "io.github.mahdibohloul"
version = "0.12.0"
description = "statemachine"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

/**
 * The lowest Kotlin version that consumers of this library can use.
 * Spring Boot 4 requires Kotlin 2.2. Keep these values when you update the Kotlin compiler,
 * thus the library stays usable for all Spring Boot 4 applications.
 */
val minimumKotlinVersion = KotlinVersion.KOTLIN_2_2
val minimumKotlinStdlibVersion = "2.2.21"

/**
 * Optional Spring Boot version to compile and test against, for example `-PspringBootVersion=4.2.0`.
 * The Spring Boot BOM is applied only to the compile and test classpaths, thus the published
 * dependency versions do not change.
 */
val springBootVersion: String? = providers.gradleProperty("springBootVersion").orNull

dependencies {
  api("io.projectreactor:reactor-core:3.8.7")
  api("org.springframework:spring-context:7.0.9")
  api("box.tapsi.libs:utilities-starter:1.0.0")

  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.3.2")
  implementation("org.springframework.boot:spring-boot-autoconfigure:4.1.1")
  implementation("org.springframework:spring-tx:7.0.9")
  implementation("org.slf4j:slf4j-api:2.0.18")

  testImplementation("org.springframework.boot:spring-boot-starter-test:4.1.1")
  testImplementation(kotlin("test-junit5"))
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  testImplementation("io.projectreactor:reactor-test:3.8.7")

  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  if (springBootVersion != null) {
    val springBootBom = platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    compileOnly(springBootBom)
    testImplementation(springBootBom)
  }
}

kotlin {
  coreLibrariesVersion = minimumKotlinStdlibVersion
  compilerOptions {
    languageVersion = minimumKotlinVersion
    apiVersion = minimumKotlinVersion
    // Compile interface bodies to JVM default methods, and keep the DefaultImpls classes for binary
    // compatibility with code compiled against earlier versions. This is the Kotlin 2.2 default.
    jvmDefault = JvmDefaultMode.ENABLE
    // Spring Framework 7 and Reactor 3.8 use JSpecify nullability annotations.
    freeCompilerArgs.add("-Xjspecify-annotations=strict")
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
  // Read the project values at configuration time. Access to Task.project at execution time
  // is deprecated and is not compatible with the configuration cache.
  val readmeFile = file("README.md")
  val checks = listOf(
    Check("group ID", """<groupId>${project.group}</groupId>"""),
    Check("version", """<version>${project.version}</version>"""),
  )
  inputs.file(readmeFile)

  doLast {
    val content = readmeFile.readText()

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
