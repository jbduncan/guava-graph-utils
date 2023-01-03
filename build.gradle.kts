import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java

    id("com.diffplug.spotless") version "5.12.4"
    id("com.github.ben-manes.versions") version "0.38.0"
    id("org.openrewrite.rewrite") version "4.2.2"
}

group = "me.jbduncan.guavagraphutils"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:30.1.1-jre")

    testImplementation("org.jgrapht:jgrapht-io:1.5.1")
    testImplementation("org.jgrapht:jgrapht-guava:1.5.1")
    testImplementation("org.jgrapht:jgrapht-core:1.5.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testImplementation("org.assertj:assertj-core:3.19.0")

    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks:1.3.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<AbstractArchiveTask>() {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.10.0")
    }
}

tasks.withType<DependencyUpdatesTask>() {
    fun isStable(version: String): Boolean {
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
        return stableKeyword || regex.matches(version)
    }

    rejectVersionIf {
        isStable(currentVersion) && !isStable(candidate.version)
    }
}

rewrite {
    activeRecipe("org.jbduncan.rewrite.CodeCleanup", "org.jbduncan.rewrite.SecurityBestPractices")

    configFile = file("$rootDir/config/rewrite.yml")
    failOnDryRunResults = true
}

tasks.named("check").configure {
    dependsOn(tasks.named("rewriteDryRun"))
}
