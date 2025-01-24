import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    `jvm-test-suite`

    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.diffplug.spotless)
    alias(libs.plugins.gradlex.reproducible.builds)
    alias(libs.plugins.net.ltgt.errorprone)
    alias(libs.plugins.openrewrite)
}

group = "com.github.jbduncan.guavagraphutils"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.guava)

    testImplementation(libs.jqwik)
    testImplementation(libs.assertj)
    testImplementation(libs.jgrapht)
    testImplementation(libs.jgrapht.guava)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    compileOnly(libs.jspecify)
    testCompileOnly(libs.jspecify)

    errorprone(libs.errorprone)
    errorprone(libs.nullaway)

    rewrite(platform(libs.openrewrite.recipe.bom))
    rewrite(libs.openrewrite.recipe.rewrite.java.dependencies)
    rewrite(libs.openrewrite.recipe.rewrite.java.security)
    rewrite(libs.openrewrite.recipe.rewrite.migrate.java)
    rewrite(libs.openrewrite.recipe.rewrite.recommendations)
    rewrite(libs.openrewrite.recipe.rewrite.testing.frameworks)
}

tasks.test {
    useJUnitPlatform {
        includeEngines("junit-jupiter", "jqwik")
    }
}

tasks.compileJava {
    options.compilerArgs = listOf("-Xlint:all,-processing")
    options.errorprone {
        error("NullAway")
        option("NullAway:AnnotatedPackages", "${project.group}")
        option("NullAway:CheckOptionalEmptiness", "true")
        option("NullAway:HandleTestAssertionLibraries", "true")
    }
}

tasks.compileTestJava {
    options.compilerArgs = listOf("-parameters")
    // Disable NullAway for tests because it gives too many false positives for
    // tests that check nullness at runtime.
    options.errorprone.disable("NullAway")
}

spotless {
    java {
        googleJavaFormat(libs.versions.googleJavaFormat.get())
            .reflowLongStrings()
            .reorderImports(true)
            .formatJavadoc(true)
        formatAnnotations()
    }
}

tasks.dependencyUpdates {
    fun isStable(version: String): Boolean {
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.contains(it, ignoreCase = true) }
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
    isExportDatatables = true

    configFile = file("$rootDir/config/rewrite.yml")
    failOnDryRunResults = true
}

tasks.check {
    dependsOn(tasks.rewriteDryRun)
}

tasks.spotlessApply {
    mustRunAfter(tasks.rewriteRun)
}

// TODO: Adjust use of Gradle toolchains as per https://jakewharton.com/gradle-toolchains-are-rarely-a-good-idea/

// TODO: Experiment with Declarative Gradle (https://declarative.gradle.org/)

// TODO: Follow https://github.com/binkley/modern-java-practices

// TODO: Use static analysis tools:
//   - https://github.com/PicnicSupermarket/error-prone-support
//   - Custom Refaster templates:
//       - Guava Refaster templates
//       - Examples on Refaster website
//       - Inspirations for new templates from eg templates and ast-grep rules in https://github.com/jbduncan/go-containers

// TODO: Adopt pre-commit (https://github.com/pre-commit/pre-commit)

// TODO: Use CI, building on latest Java and testing all the way through to the lowest Java
//       as per https://jakewharton.com/build-on-latest-java-test-through-lowest-java/

// TODO: Use RenovateBot or Dependabot with Gradle's dependency-submission action:
//       https://github.com/gradle/actions/blob/main/dependency-submission/README.md

// TODO: Do GitHub releases and upload to Maven Central. See JUnit 5 and junit-pioneer for examples.
