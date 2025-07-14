plugins {
    id("com.diffplug.spotless") version "6.25.0"
}

apply(plugin = "com.diffplug.gradle.spotless")

spotless {
    java {
        target("**/*.java")
        licenseHeaderFile("../spotless.license.kt")
        trimTrailingWhitespace()
        removeUnusedImports()
        googleJavaFormat()
        endWithNewline()
    }
    kotlin {
        target("**/*.kt")
        licenseHeaderFile("../spotless.license.kt")
        trimTrailingWhitespace()
        endWithNewline()
    }
}