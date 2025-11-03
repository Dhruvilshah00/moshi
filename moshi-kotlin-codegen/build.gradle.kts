import com.vanniktech.maven.publish.JavadocJar.None
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  id("com.google.devtools.ksp")
  id("com.vanniktech.maven.publish.base")
  id("jacoco")
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    optIn.add("com.squareup.moshi.kotlin.codegen.api.InternalMoshiCodegenApi")
  }
}

tasks.compileTestKotlin {
  compilerOptions {
    optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
  }
}

tasks.test {
  minHeapSize = "2048m"
  maxHeapSize = "2048m"
  jvmArgs("-Djava.awt.headless=true")
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required.set(true)
    html.required.set(true)
  }
}

dependencies {
  implementation(project(":moshi"))
  api(libs.kotlinpoet)
  implementation(libs.kotlinpoet.ksp)
  implementation(libs.asm)

  implementation(libs.autoService)
  ksp(libs.autoService.ksp)

  compileOnly(libs.ksp)
  compileOnly(libs.ksp.api)
  compileOnly(libs.kotlin.compilerEmbeddable)

  testImplementation(libs.ksp)
  testImplementation(libs.ksp.api)
  testImplementation(libs.kotlin.compilerEmbeddable)
  testImplementation(libs.kotlin.annotationProcessingEmbeddable)
  testImplementation(libs.kotlinCompileTesting.ksp)

  testImplementation(project(":moshi"))
  testImplementation(kotlin("reflect"))
  testImplementation(libs.kotlinpoet.ksp)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.kotlinCompileTesting)
}

configure<MavenPublishBaseExtension> {
  configure(KotlinJvm(javadocJar = None()))
}
