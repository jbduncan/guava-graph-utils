import com.diffplug.gradle.spotless.SpotlessApply
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.compile.JavaCompile
import org.openrewrite.gradle.ResolveRewriteDependenciesTask
import org.openrewrite.gradle.RewriteDryRunTask
import org.openrewrite.gradle.RewriteRunTask

plugins {
    `java-library`

    id("com.diffplug.spotless") version "6.25.0"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("net.ltgt.errorprone") version "3.1.0"
    id("org.openrewrite.rewrite") version "6.8.4"
}

group = "com.github.jbduncan.guavagraphutils"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.guava:guava:33.0.0-jre")

    testImplementation("net.jqwik:jqwik:1.8.3")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.jgrapht:jgrapht-guava:1.5.2")
    testImplementation("org.jgrapht:jgrapht-core:1.5.2")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.jspecify:jspecify:0.3.0")
    testCompileOnly("org.jspecify:jspecify:0.3.0")

    errorprone("com.google.errorprone:error_prone_core:2.25.0")
    errorprone("com.uber.nullaway:nullaway:0.10.23")

    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:2.7.1"))
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
        googleJavaFormat("1.20.0")
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

tasks.withType<RewriteRunTask>().configureEach {
    notCompatibleWithConfigurationCache(
        "Uses Task, Project and Task.project at configuration time, which are unsupported by " +
                "the configuration cache"
    )
}

tasks.withType<RewriteDryRunTask>().configureEach {
    notCompatibleWithConfigurationCache(
        "Uses Task, Project and Task.project at configuration time, which are unsupported by " +
                "the configuration cache"
    )
}

tasks.withType<ResolveRewriteDependenciesTask>().configureEach {
    notCompatibleWithConfigurationCache(
        "Uses Configuration, Project and Task.project at configuration time, which are " +
                "unsupported by the configuration cache"
    )
}

tasks.check.configure {
    dependsOn(tasks.withType<RewriteDryRunTask>())
}

tasks.withType<SpotlessApply>().configureEach {
    mustRunAfter(tasks.withType<RewriteRunTask>())
}

// TODO: Adjust use of Gradle toolchains as per https://jakewharton.com/gradle-toolchains-are-rarely-a-good-idea/

// TODO: Use Gradle version catalogs (see other personal projects)

// TODO: Follow https://github.com/binkley/modern-java-practices

// TODO: Use static analysis tools:
//   - https://github.com/PicnicSupermarket/error-prone-support

// TODO: Use CI, building on latest Java and testing all the way through to the lowest Java
//       as per https://jakewharton.com/build-on-latest-java-test-through-lowest-java/

// TODO: Use RenovateBot or Dependabot with Gradle's dependency-submission action:
//       https://github.com/gradle/actions/blob/main/dependency-submission/README.md

// TODO: Do GitHub releases and upload to Maven Central. See JUnit 5 and junit-pioneer for examples.
