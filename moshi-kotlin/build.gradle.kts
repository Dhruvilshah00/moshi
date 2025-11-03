import org.gradle.jvm.tasks.Jar

plugins {
  kotlin("jvm")
  id("com.vanniktech.maven.publish.base")
  id("org.jetbrains.dokka")
  id("jacoco")
}

dependencies {
  api(project(":moshi"))
  api(kotlin("reflect"))

  testImplementation(kotlin("test"))
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

tasks.withType<Test>().configureEach {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required.set(true)
    html.required.set(true)
  }
}

tasks.withType<Jar>().configureEach {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.moshi.kotlin")
  }
}
