
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

            // MyBatis
            library(
                "mybatis", "org.mybatis", "mybatis"
            ).version(providers.gradleProperty("mybatis.version").get())

            // MyBatis Spring
            library(
                "mybatis-spring", "org.mybatis", "mybatis-spring"
            ).version(providers.gradleProperty("mybatis-spring.version").get())

            // MyBatis Spring Boot
            library(
                "mybatis-spring-boot", "org.mybatis.spring.boot", "mybatis-spring-boot-starter"
            ).version(providers.gradleProperty("mybatis-spring-boot.version").get())

            // Testing

            // H2
            library("h2", "com.h2database:h2:1.4.200")

            //  JUnit
            library("junit-platform-launcher", "org.junit.platform:junit-platform-launcher:1.10.2")

            library("junit-jupiter-engine", "org.junit.jupiter:junit-jupiter-engine:5.10.2")

            // Logback
            library("logback-classic", "ch.qos.logback:logback-classic:1.2.12")

        }
    }
}

rootProject.name = "microsphere-apidocs"

include(
)