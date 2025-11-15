pluginManagement {
    // Include 'plugins build' to define convention plugins.
    includeBuild("build-logic")
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {

            // BOM

            // Microsphere Stack Dependencies (BOM)
            library(
                "microsphere-java-dependencies", "io.github.microsphere-projects", "microsphere-java-dependencies"
            ).version(providers.gradleProperty("microsphere-java-dependencies.version").get())

            library(
                "microsphere-spring-dependencies", "io.github.microsphere-projects", "microsphere-spring-dependencies"
            ).version(providers.gradleProperty("microsphere-spring-dependencies.version").get())

            library(
                "microsphere-spring-boot-dependencies",
                "io.github.microsphere-projects",
                "microsphere-spring-boot-dependencies"
            ).version(providers.gradleProperty("microsphere-spring-boot-dependencies.version").get())

            library(
                "microsphere-spring-cloud-dependencies",
                "io.github.microsphere-projects",
                "microsphere-spring-cloud-dependencies"
            ).version(providers.gradleProperty("microsphere-spring-cloud-dependencies.version").get())

            // Spring Stack Dependencies (BOM)
            library(
                "spring-framework-bom",
                "org.springframework",
                "spring-framework-bom"
            ).version(providers.gradleProperty("spring.version").get())

            library("spring-boot-dependencies", "org.springframework.boot", "spring-boot-dependencies")
                .version(providers.gradleProperty("spring-boot.version").get())

            library(
                "spring-cloud-dependencies", "org.springframework.cloud", "spring-cloud-dependencies"
            ).version(providers.gradleProperty("spring-cloud.version").get())

            // Libraries

            // JSR-305
            library("jsr305", "com.google.code.findbugs:jsr305:3.0.2")

            // Testing

            // H2
            library("h2", "com.h2database:h2:1.4.200")

            //  JUnit
            library(
                "junit-platform-launcher", "org.junit.platform", "junit-platform-launcher"
            ).version(providers.gradleProperty("junit-platform-launcher.version").get())

            library(
                "junit-jupiter-engine", "org.junit.jupiter", "junit-jupiter-engine"
            ).version(providers.gradleProperty("junit-jupiter-engine.version").get())

            // Logback
            library(
                "logback-classic", "ch.qos.logback", "logback-classic"
            ).version(providers.gradleProperty("logback.version").get())

        }
    }
}

rootProject.name = "microsphere-apidocs"

include(
)