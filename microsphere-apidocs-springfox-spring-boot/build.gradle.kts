plugins {
    id("buildlogic.java-library-conventions")
}

dependencies {
    // BOM
    // Spring Framework BOM
    implementation(platform(libs.spring.framework.bom))

    // Spring Boot Dependencies
    implementation(platform(libs.spring.boot.dependencies))

    // Microsphere Dependencies
    // implementation(platform(libs.microsphere.spring.dependencies))
    implementation(platform(libs.microsphere.spring.boot.dependencies))

    // Microsphere
    "optionalApi"("io.github.microsphere-projects:microsphere-spring-boot-core:0.1.5")
    "optionalApi"("io.github.microsphere-projects:microsphere-annotation-processor:0.1.5")

    // Springfox
    "optionalApi"("io.springfox:springfox-boot-starter:3.0.0")

    // Apache Dubbo
    // "optionalApi"(platform(libs.dubbo.spring.boot.starter))
    "optionalApi"("org.apache.dubbo:dubbo-spring-boot-starter:3.2.16")

    // Spring Boot
    "optionalApi"("org.springframework.boot:spring-boot-starter-web")
    "optionalApi"("org.springframework.boot:spring-boot-configuration-processor")

    // JSR305
    "optionalApi"(libs.jsr305)

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("ch.qos.logback:logback-classic")
}