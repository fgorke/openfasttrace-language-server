import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform") version "2.17.0"
}

group = "org.itsallcode.openfasttrace"

version = providers.provider {
    val pomFile = project.projectDir.resolve("../pom.xml").absoluteFile
    if (!pomFile.exists()) throw GradleException("Could not find project pom.xml at ${pomFile.absolutePath}")
    val text = pomFile.readText()
    val regex = Regex("(?s)<project[\\s\\S]*?<version>([^<]+)</version>")
    val match = regex.find(text)
    match?.groupValues?.get(1)?.trim() ?: throw GradleException("Could not extract <version> from ${pomFile.absolutePath}")
}.get()

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

val copyServerJar by tasks.registering(Copy::class) {
    val serverJar = providers.provider {
        val targetDir = rootProject.projectDir.resolve("../target")
        val candidates = targetDir.listFiles { _, name ->
            name.startsWith("openfasttrace-language-server-") && name.endsWith("-standalone.jar")
        }?.sortedByDescending { it.lastModified() }.orEmpty()

        candidates.firstOrNull() ?: throw GradleException(
            "No standalone server JAR found in ${targetDir.absolutePath}. Run 'mvn package' in repository root first."
        )
    }

    from(serverJar) {
        rename { "openfasttrace-language-server.jar" }
    }
    into(layout.projectDirectory.dir("src/main/resources/lib"))
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
