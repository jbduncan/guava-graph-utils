import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.openrewrite.gradle.RewriteDryRunTask

plugins {
    java

    id("com.github.ben-manes.versions")
    id("org.openrewrite.rewrite")
}

group = "org.jbduncan"
version = "0.1.0-SNAPSHOT"

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

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testImplementation("org.assertj:assertj-core:3.19.0")

    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks:1.2.1")
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

rewrite {
    activeRecipe("org.jbduncan.rewrite.CodeCleanup", "org.jbduncan.rewrite.SecurityBestPractices")

    configFile = file("$rootDir/config/rewrite.yml")
}

tasks.named("check").configure {
    dependsOn(tasks.named("rewriteDryRun"))
}

tasks.withType<RewriteDryRunTask>().configureEach {
    val outputFiles = fileTree("$buildDir/reports/rewrite") { include("*.patch") }

    doFirst {
        outputFiles.forEach {
            if (!it.delete()) {
                throw GradleException("Failed to delete '$it'")
            }
        }
    }

    doLast {
        outputFiles.forEach {
            throw GradleException("Not all refactorings have been applied. See '$it'.")
        }
    }
}
