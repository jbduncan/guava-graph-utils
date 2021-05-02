rootProject.name = "guava-graph-utils"
include("guava-graph-utils")
include("rewrite")

pluginManagement {
    plugins {
        id("com.diffplug.spotless") version "5.12.4"
        id("com.github.ben-manes.versions") version "0.38.0"
        id("org.openrewrite.rewrite") version "4.1.4"
        id("org.jetbrains.kotlin.jvm") version "1.5.0"
    }
}
