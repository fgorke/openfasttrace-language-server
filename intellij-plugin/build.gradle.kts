import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.6.0"
}

group = "org.itsallcode.openfasttrace"
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate(providers.gradleProperty("intellijVersion"))
        pluginVerifier()
    }
}

// Copy the standalone server JAR into plugin resources before packaging.
// Run 'mvn package' in the parent directory first to produce the JAR.
val copyServerJar by tasks.registering(Copy::class) {
    val serverJar = rootProject.projectDir.resolve(
        "../target/openfasttrace-language-server-0.1.0-SNAPSHOT-standalone.jar"
    )
    from(serverJar) {
        rename { "openfasttrace-language-server.jar" }
    }
    into(layout.projectDirectory.dir("src/main/resources/lib"))
    onlyIf { serverJar.exists() }
}

tasks.named("processResources") {
    dependsOn(copyServerJar)
}

intellijPlatform {
    pluginConfiguration {
        name = "OpenFastTrace Language Server"
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
    buildSearchableOptions = false
}
