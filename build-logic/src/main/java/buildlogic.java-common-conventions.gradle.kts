plugins {
    `java`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
    registerFeature("optional") {
        usingSourceSet(sourceSets["main"])
    }

    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-source", "8", "-target", "8"))
}

tasks.javadoc {
    if (JavaVersion.current().isJava8Compatible) {
        options {
            this as StandardJavadocDocletOptions
            addBooleanOption("Xdoclint:none", true)
            addBooleanOption("quiet", true)
        }
    }
}
