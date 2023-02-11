import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java

    id("com.diffplug.spotless") version "6.15.0"
    id("com.github.ben-manes.versions") version "0.45.0"
    id("org.openrewrite.rewrite") version "5.35.0"
}

group = "me.jbduncan.guavagraphutils"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:31.1-jre")

    testImplementation("org.jgrapht:jgrapht-io:1.5.1")
    constraints {
        testImplementation("org.apache.commons:commons-text:1.10.0") {
            because("it forces jgrapht-io to avoid Apache Commons Text 1.8, which has a security vulnerability")
        }
    }
    testImplementation("org.jgrapht:jgrapht-guava:1.5.1")
    testImplementation("org.jgrapht:jgrapht-core:1.5.1")
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.assertj:assertj-core:3.24.2")

    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:1.14.0"))
    rewrite("org.openrewrite.recipe:rewrite-java-security")
    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks")
}

tasks.test.configure {
    useJUnitPlatform()
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.15.0")
    }
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

rewrite {
    activeRecipe("me.jbduncan.rewrite.CodeCleanup", "me.jbduncan.rewrite.SecurityBestPractices")

    configFile = file("$rootDir/config/rewrite.yml")
    failOnDryRunResults = true
}

tasks.named("check").configure {
    dependsOn(tasks.named("rewriteDryRun"))
}
