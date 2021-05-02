import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("com.github.ben-manes.versions")
    id("org.jetbrains.kotlin.jvm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openrewrite:rewrite-java:7.2.2")
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.0")
    }
    runtimeOnly("org.openrewrite:rewrite-java-11:7.2.2")
    runtimeOnly("org.openrewrite:rewrite-java-8:7.2.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testImplementation("org.openrewrite:rewrite-test:7.2.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    fun isStable(version: String): Boolean {
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
        return stableKeyword || regex.matches(version)
    }

    rejectVersionIf {
        isStable(currentVersion) && !isStable(candidate.version)
    }
}
