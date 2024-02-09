import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java

    id("com.diffplug.spotless") version "6.25.0"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("net.ltgt.errorprone") version "3.1.0"
    id("org.openrewrite.rewrite") version "6.8.1"
}

group = "com.github.jbduncan.guavagraphutils"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:33.0.0-jre")

    testImplementation("net.jqwik:jqwik:1.8.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.jgrapht:jgrapht-guava:1.5.2")
    testImplementation("org.jgrapht:jgrapht-core:1.5.2")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.jspecify:jspecify:0.3.0")
    testCompileOnly("org.jspecify:jspecify:0.3.0")

    errorprone("com.google.errorprone:error_prone_core:2.24.1")
    errorprone("com.uber.nullaway:nullaway:0.10.22")

    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:2.6.2"))
    rewrite("org.openrewrite.recipe:rewrite-java-security")
    rewrite("org.openrewrite.recipe:rewrite-migrate-java")
    rewrite("org.openrewrite.recipe:rewrite-recommendations")
    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks")
}

tasks.test.configure {
    useJUnitPlatform {
        includeEngines("junit-jupiter", "jqwik")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs = listOf("-Xlint:all,-processing")
    options.encoding = "UTF-8"
}

tasks.compileJava.configure {
    options.errorprone {
        error("NullAway")
        option("NullAway:AnnotatedPackages", "${project.group}")
        option("NullAway:CheckOptionalEmptiness", "true")
        option("NullAway:HandleTestAssertionLibraries", "true")
    }
}

tasks.compileTestJava.configure {
    options.compilerArgs = listOf("-parameters")
    // Disable NullAway for tests because it gives too many false positives for
    // tests that check nullness at runtime.
    options.errorprone.disable("NullAway")
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dirMode = Integer.parseInt("0755", 8)
    fileMode = Integer.parseInt("0644", 8)
}

spotless {
    java {
        googleJavaFormat("1.19.2")
            .reflowLongStrings()
            .reorderImports(true)
            .formatJavadoc(true)
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
    activeRecipe(
        "com.github.jbduncan.rewrite.CodeCleanup",
        "com.github.jbduncan.rewrite.SecurityBestPractices"
    )

    configFile = file("$rootDir/config/rewrite.yml")
    failOnDryRunResults = true
}

val rewriteRun: Task by tasks.getting {
    notCompatibleWithConfigurationCache(
        "Uses Task, Project and Task.project at configuration time, which are unsupported by " +
                "the configuration cache"
    )
}

val rewriteDryRun: Task by tasks.getting {
    notCompatibleWithConfigurationCache(
        "Uses Task, Project and Task.project at configuration time, which are unsupported by " +
                "the configuration cache"
    )
}

val rewriteResolveDependencies: Task by tasks.getting {
    notCompatibleWithConfigurationCache(
        "Uses Configuration, Project and Task.project at configuration time, which are " +
                "unsupported by the configuration cache"
    )
}

tasks.named("check").configure {
    dependsOn(rewriteDryRun)
}

// TODO: Use Gradle version catalogs (see other personal projects)

// TODO: Follow https://github.com/binkley/modern-java-practices

// TODO: Expand use of OpenRewrite:
//   - https://docs.openrewrite.org/recipes/java/migrate/guava
//   - https://docs.openrewrite.org/recipes/java/migrate/util/javautilapis
//   - https://docs.openrewrite.org/recipes/java/testing/assertj
//   - https://docs.openrewrite.org/recipes/java/testing/cleanup
//   - https://docs.openrewrite.org/recipes/java/testing/hamcrest
//   - https://docs.openrewrite.org/recipes/java/testing/junit5
//   - https://docs.openrewrite.org/recipes/java/migrate/usejavautilbase64
//   - https://docs.openrewrite.org/recipes/java/migrate/io
//   - https://docs.openrewrite.org/recipes/java/migrate/lang/usetextblocks
//   - https://docs.openrewrite.org/recipes/java/migrate/net
//   - https://docs.openrewrite.org/recipes/java/migrate/sql
//   - https://docs.openrewrite.org/recipes/java/migrate/util
//   - https://docs.openrewrite.org/recipes/java/migrate/concurrent
//   - https://docs.openrewrite.org/recipes/java/migrate/logging
//   - https://docs.openrewrite.org/recipes/java/migrate/javax
//   - https://docs.openrewrite.org/recipes/java/security
//   - https://docs.openrewrite.org/recipes/java/migrate/lang/stringrulesrecipes
//   - https://docs.openrewrite.org/recipes/java/testing/mockito
//   - https://docs.openrewrite.org/recipes/kotlin
//   - https://docs.openrewrite.org/recipes/staticanalysis/nodoublebraceinitialization

// TODO: Can we simplify config/rewrite.yml by finding and removing redundant recipes?

// TODO: Use static analysis tools:
//   - https://github.com/PicnicSupermarket/error-prone-support

// TODO: Use CI

// TODO: Use RenovateBot

// TODO: Do GitHub releases and upload to Maven Central. See JUnit 5 and junit-pioneer for examples.

// TODO: Enable Gradle Build Cache (local):
//   - https://docs.gradle.org/current/userguide/build_cache.html
//   - https://docs.gradle.org/current/userguide/caching_java_projects.html
//   - https://docs.gradle.org/current/userguide/build_cache_debugging.html
//   - https://docs.gradle.org/current/userguide/common_caching_problems.html
