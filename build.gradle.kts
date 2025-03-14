import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`

    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.diffplug.spotless)
    alias(libs.plugins.gradlex.reproducible.builds)
    alias(libs.plugins.net.ltgt.errorprone)
    alias(libs.plugins.openrewrite)
}

group = "com.github.jbduncan.guavagraphutils"

version = "0.1.0-SNAPSHOT"

repositories { mavenCentral() }

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

val oldestSupportedJavaVersion: Int = 17
val newestTestedJavaVersion: Int = 21

tasks.updateDaemonJvm {
    @Suppress("UnstableApiUsage")
    jvmVersion = JavaLanguageVersion.of(newestTestedJavaVersion)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(newestTestedJavaVersion))

tasks.compileJava {
    options.release.set(oldestSupportedJavaVersion)
    options.compilerArgs = listOf("-Xlint:all,-processing")
    options.errorprone {
        error("NullAway")
        option("NullAway:AnnotatedPackages", "${project.group}")
        option("NullAway:CheckOptionalEmptiness", "true")
        option("NullAway:HandleTestAssertionLibraries", "true")
    }
}

tasks.compileTestJava {
    options.release.set(oldestSupportedJavaVersion)
    options.compilerArgs = listOf("-parameters")

    // Disable NullAway for tests because it generates too many false positives
    // for tests that check nullness at runtime.
    options.errorprone.disable("NullAway")
}

tasks.test { useJUnitPlatformEngines() }

listOf(oldestSupportedJavaVersion, newestTestedJavaVersion).forEach { majorVersion ->
    val jdkTest =
        tasks.register<Test>("testJdk${majorVersion}Version") {
            javaLauncher =
                javaToolchains.launcherFor {
                    languageVersion = JavaLanguageVersion.of(majorVersion)
                }

            description = "Runs the test suite on JDK $majorVersion"
            group = LifecycleBasePlugin.VERIFICATION_GROUP

            // Copy inputs from normal Test task.
            classpath = sourceSets["test"].runtimeClasspath
            testClassesDirs = sourceSets["test"].output.classesDirs

            useJUnitPlatformEngines()

            dependsOn(tasks.compileTestJava)
        }

    tasks.check { dependsOn(jdkTest) }
}

private fun Test.useJUnitPlatformEngines() {
    useJUnitPlatform { includeEngines("junit-jupiter", "jqwik") }
}

spotless {
    java {
        googleJavaFormat(libs.versions.googleJavaFormat.get())
            .reflowLongStrings()
            .reorderImports(true)
            .formatJavadoc(true)
        formatAnnotations()
    }
    kotlin {
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
        target("*.kts")
    }
}

tasks.dependencyUpdates {
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    fun isStable(version: String): Boolean {
        val stableKeyword =
            listOf("RELEASE", "FINAL", "GA").any { version.contains(it, ignoreCase = true) }
        return stableKeyword || regex.matches(version)
    }

    rejectVersionIf { isStable(currentVersion) && !isStable(candidate.version) }
}

rewrite {
    activeRecipe(
        "com.github.jbduncan.rewrite.CodeCleanup",
        "com.github.jbduncan.rewrite.SecurityBestPractices",
    )
    isExportDatatables = true

    configFile = file("$rootDir/config/rewrite.yml")
    failOnDryRunResults = true
}

tasks.check { dependsOn(tasks.rewriteDryRun) }

tasks.spotlessApply {
    mustRunAfter(tasks.rewriteRun)
}

// TODO: Add Gradle dependency verification:
//       https://britter.dev/blog/2025/02/10/gradle-dependency-verification/

// TODO: Use static analysis tools:
//   - https://github.com/PicnicSupermarket/error-prone-support
//   - Custom Refaster templates:
//       - Guava Refaster templates
//       - Examples on Refaster website
//       - Inspirations for new templates from eg templates and ast-grep rules in
//         https://github.com/jbduncan/go-containers

// TODO: Adopt errorprone annotations:
//   - CheckReturnValue on package-info.java level
//   - Var
//       See Caffeine's source code for examples and see if any extra errorprone
//       checks need to be enabled.

// TODO: Caffeine seems to enable a JSpecify mode for Nullaway. Look into this
//       and enable it here.

// TODO: Look into other ways that Caffeine configures Nullaway.

// TODO: Look into ways that Caffeine configures errorprone and Refaster.

// TODO: Adopt pre-commit (https://github.com/pre-commit/pre-commit), lefthook
//       (github.com/evilmartians/lefthook), or mise's support for git pre-commit hooks

// TODO: Evaluate these Gradle plugins for use:
//   - https://docs.gradle.org/current/userguide/jacoco_plugin.html
//   - https://github.com/autonomousapps/dependency-analysis-gradle-plugin
//   - https://github.com/vanniktech/gradle-dependency-graph-generator-plugin
//   - https://github.com/dorongold/gradle-task-tree
//   - https://github.com/cashapp/licensee
//   - https://github.com/dropbox/dependency-guard
//   - https://github.com/spring-io/nohttp
//   - https://kordamp.org/kordamp-gradle-plugins
//   - https://github.com/freefair/gradle-plugins
//   - https://gradleup.com/projects/
//   - https://gradlex.org/

// TODO: Enable Gradle build scans: https://scans.gradle.com/

// TODO: Use RenovateBot or Dependabot with Gradle's dependency-submission action:
//       https://github.com/gradle/actions/blob/main/dependency-submission/README.md

// TODO: Look into flox (https://github.com/flox/flox) as an alternative to mise for tool management

// TODO: Follow https://github.com/binkley/modern-java-practices

// TODO: Add GitHub Actions CI. Refer to Caffeine's source code for inspiration:
//       https://github.com/ben-manes/caffeine/tree/master/.github/workflows

// TODO: Adopt OpenSSF and CodeQL as per JUnit 5's GitHub Actions workflows

// TODO: Structure Gradle project more idiomatically by following
//   https://github.com/jjohannes/idiomatic-gradle

// TODO: Do GitHub releases and upload to Maven Central. See JUnit 5 and junit-pioneer for examples.
//   May need these plugins:
//   - https://github.com/gradle-nexus/publish-plugin
//   - https://github.com/shipkit/shipkit-changelog
//   - https://github.com/shipkit/shipkit-auto-version
//   - https://github.com/melix/japicmp-gradle-plugin

// TODO: Experiment with Declarative Gradle (https://declarative.gradle.org/)

// TODO: Consider fuzzing with com.code_intelligence.jazzer. Search Caffeine's
//       source code for more info.
