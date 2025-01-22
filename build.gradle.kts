import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`

    id("com.diffplug.spotless") version "6.25.0"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("net.ltgt.errorprone") version "4.1.0"
    id("org.gradlex.reproducible-builds") version "1.0"
    id("org.openrewrite.rewrite") version "6.28.1"
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
    api("com.google.guava:guava:33.3.1-jre")

    testImplementation("net.jqwik:jqwik:1.9.1")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.jgrapht:jgrapht-guava:1.5.2")
    testImplementation("org.jgrapht:jgrapht-core:1.5.2")
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.jspecify:jspecify:1.0.0")
    testCompileOnly("org.jspecify:jspecify:1.0.0")

    errorprone("com.google.errorprone:error_prone_core:2.36.0")
    errorprone("com.uber.nullaway:nullaway:0.12.1")

    rewrite(platform("org.openrewrite.recipe:rewrite-recipe-bom:2.23.1"))
    rewrite("org.openrewrite.recipe:rewrite-java-dependencies")
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

tasks.compileJava.configure {
    options.compilerArgs = listOf("-Xlint:all,-processing")
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

spotless {
    java {
        googleJavaFormat("1.25.0")
            .reflowLongStrings()
            .reorderImports(true)
            .formatJavadoc(true)
        formatAnnotations()
    }
}

tasks.dependencyUpdates.configure {
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
    isExportDatatables = true

    configFile = file("$rootDir/config/rewrite.yml")
    failOnDryRunResults = true
}

tasks.check.configure {
    dependsOn(tasks.rewriteDryRun)
}

tasks.spotlessApply.configure {
    mustRunAfter(tasks.rewriteRun)
}

// TODO: Adjust use of Gradle toolchains as per https://jakewharton.com/gradle-toolchains-are-rarely-a-good-idea/

// TODO: Use Gradle version catalogs (see other personal projects)

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
