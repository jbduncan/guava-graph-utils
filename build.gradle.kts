import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java

    id("com.diffplug.spotless") version "6.20.0"
    id("com.github.ben-manes.versions") version "0.47.0"
    id("org.openrewrite.rewrite") version "6.2.3"
}

group = "com.github.jbduncan.guavagraphutils"
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
    implementation("com.google.guava:guava:32.1.2-jre")

    testImplementation("net.jqwik:jqwik:1.7.4")
    testImplementation("org.jgrapht:jgrapht-guava:1.5.2")
    testImplementation("org.jgrapht:jgrapht-core:1.5.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.24.2")

    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:2.2.1"))
    rewrite("org.openrewrite.recipe:rewrite-java-security")
    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks")
}

tasks.test.configure {
    useJUnitPlatform {
        includeEngines("junit-jupiter", "jqwik")
    }
}

tasks.withType<JavaCompile>().named("compileTestJava").configure {
    options.compilerArgs.add("-parameters")
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

spotless {
    java {
        googleJavaFormat("1.17.0").reflowLongStrings()
        formatAnnotations()
    }
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    fun isStable(version: String): Boolean {
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
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

// TODO: Use static analysis tools:
//   - error-prone (errorprone.info)
//   - Checkstyle (which can be automatically fixed with OpenRewrite:
//     https://docs.openrewrite.org/running-recipes/popular-recipe-guides/automatically-fix-checkstyle-violations)
//   - NullAway

// TODO: Use CI
