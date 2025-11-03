import TestMode.KSP
import TestMode.REFLECT
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  id("com.google.devtools.ksp") apply false
  id("jacoco")
}

enum class TestMode {
  REFLECT,
  KSP,
}

val testMode =
  findProperty("kotlinTestMode")
    ?.toString()
    ?.let(TestMode::valueOf)
    ?: REFLECT

when (testMode) {
  REFLECT -> { /* Do nothing */ }
  KSP -> { apply(plugin = "com.google.devtools.ksp") }
}

tasks.withType<Test>().configureEach {
  jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required.set(true)
    html.required.set(true)
  }
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    allWarningsAsErrors.set(true)
    freeCompilerArgs.addAll(
      "-opt-in=kotlin.ExperimentalStdlibApi",
      "-Xannotation-default-target=param-property",
    )
  }
}

dependencies {
  when (testMode) {
    REFLECT -> {}
    KSP -> { "kspTest"(project(":moshi-kotlin-codegen")) }
  }
  testImplementation(project(":moshi"))
  testImplementation(project(":moshi-kotlin"))
  testImplementation(project(":moshi-kotlin-tests:extra-moshi-test-module"))
  testImplementation(kotlin("reflect"))
  testImplementation(libs.junit)
  testImplementation(libs.assertj)
  testImplementation(libs.truth)
}
