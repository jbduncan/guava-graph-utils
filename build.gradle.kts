buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        target("guava-graph-utils/src/**/*.java")
        googleJavaFormat("1.10.0")
    }

    kotlin {
        target("rewrite/src/**/*.kt")
        ktfmt("0.24")
    }
}
