import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java

    id("com.diffplug.spotless") version "6.21.0"
    id("com.github.ben-manes.versions") version "0.47.0"
    id("net.ltgt.errorprone") version "3.1.0"
    id("org.openrewrite.rewrite") version "6.3.2"
}

val rootPackage = "com.github.jbduncan.guavagraphutils"

group = rootPackage
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

    testImplementation("net.jqwik:jqwik:1.8.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.jgrapht:jgrapht-guava:1.5.2")
    testImplementation("org.jgrapht:jgrapht-core:1.5.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.jspecify:jspecify:0.3.0")

    errorprone("com.google.errorprone:error_prone_core:2.21.1")
    errorprone("com.uber.nullaway:nullaway:0.10.13")

    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:2.2.1"))
    rewrite("org.openrewrite.recipe:rewrite-java-security")
    rewrite("org.openrewrite.recipe:rewrite-migrate-java")
    rewrite("org.openrewrite.recipe:rewrite-recommendations:1.0.2")
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
        option("NullAway:AnnotatedPackages", rootPackage)
        option("NullAway:CheckOptionalEmptiness", "true")
        option("NullAway:HandleTestAssertionLibraries", "true")
    }
}

tasks.compileTestJava.configure {
    // Disable NullAway for tests because it gives too many false positives for
    // tests that check nullness at runtime.
    options.errorprone.disable("NullAway")
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
    activeRecipe(
            "com.github.jbduncan.rewrite.CodeCleanup",
            "com.github.jbduncan.rewrite.SecurityBestPractices")

    configFile = file("$rootDir/config/rewrite.yml")
    failOnDryRunResults = true
}

val rewriteDryRun: Task by tasks.getting {
    notCompatibleWithConfigurationCache(
            "Uses Task, Project and Task.project at configuration time, which are unsupported by " +
                    "the configuration cache")
}

val rewriteResolveDependencies: Task by tasks.getting {
    notCompatibleWithConfigurationCache(
            "Uses Configuration, Project and Task.project at configuration time, which are " +
                    "unsupported by the configuration cache")
}

tasks.named("check").configure {
    dependsOn(rewriteDryRun)
}

// TODO: Use static analysis tools:
//   - https://github.com/PicnicSupermarket/error-prone-support

// TODO: Use CI

// TODO: Use Gradle version catalogs (see other personal projects)

// TODO: Use RenovateBot

// TODO: Do GitHub releases and upload to Maven Central. See JUnit 5 and junit-pioneer for examples.
