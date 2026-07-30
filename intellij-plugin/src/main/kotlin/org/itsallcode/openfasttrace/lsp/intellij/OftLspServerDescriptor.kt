package org.itsallcode.openfasttrace.lsp.intellij

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import java.io.File
import java.nio.file.Files

private val LOG = logger<OftLspServerDescriptor>()

internal class OftLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "OpenFastTrace LSP") {

    override fun isSupportedFile(file: VirtualFile): Boolean = Companion.isSupportedFile(file)

    override val lspSemanticTokensSupport = OftSemanticTokensSupport()

    override fun createCommandLine(): GeneralCommandLine {
        val jarPath = resolveServerJar()
        LOG.info("Starting OpenFastTrace LSP server: $jarPath")
        return GeneralCommandLine("java", "-jar", jarPath)
    }

    private fun resolveServerJar(): String {
        val projectDir = File(project.basePath ?: "")
        val targetDir = projectDir.resolve("target")
        val jarInTarget = targetDir
            .listFiles { f -> f.name.matches(SERVER_JAR_PATTERN) }
            ?.maxByOrNull { it.lastModified() }
        if (jarInTarget != null && jarInTarget.exists()) {
            LOG.info("Using JAR from Maven target: ${jarInTarget.absolutePath}")
            return jarInTarget.absolutePath
        }

        val resource = javaClass.classLoader.getResourceAsStream("lib/openfasttrace-language-server.jar")
        if (resource != null) {
            val tempDir = Files.createTempDirectory("oft-lsp-server").toFile()
            val tempJar = File(tempDir, "openfasttrace-language-server.jar")
            resource.use { input -> tempJar.outputStream().use { input.copyTo(it) } }
            LOG.debug("Extracted bundled JAR to: ${tempJar.absolutePath}")
            return tempJar.absolutePath
        }

        error(
            "OpenFastTrace Language Server JAR not found.\n" +
                "Run 'mvn package' in the openfasttrace-language-server project root first,\n" +
                "or rebuild the IntelliJ plugin to bundle the JAR."
        )
    }

    companion object {
        private val SERVER_JAR_PATTERN = Regex(
            "openfasttrace-language-server-.*-standalone\\.jar"
        )

        private val SUPPORTED_EXTENSIONS = setOf(
            "md", "markdown",
            "ads", "adb",
            "bat",
            "c", "cc", "cpp", "c++", "h", "hh", "hpp", "h++",
            "c#", "cs",
            "cfg", "conf", "ini",
            "feature",
            "go",
            "groovy",
            "htm", "html", "xhtml", "json", "yaml", "yml", "toml",
            "java", "clj", "kt", "scala",
            "js", "mjs", "cjs", "ejs", "ts",
            "lua",
            "m", "mm",
            "php",
            "pl", "pm",
            "proto",
            "pu", "puml", "plantuml",
            "py",
            "r",
            "robot",
            "rs",
            "sh", "bash", "zsh",
            "sv", "v", "inc",
            "swift",
            "tf", "tfvars",
            "sql", "pls",
        )

        fun isSupportedFile(file: VirtualFile): Boolean =
            file.extension?.lowercase() in SUPPORTED_EXTENSIONS
    }
}
