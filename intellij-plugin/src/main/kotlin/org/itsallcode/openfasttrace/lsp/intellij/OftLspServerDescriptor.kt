package org.itsallcode.openfasttrace.lsp.intellij

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.Lsp4jClient
import com.intellij.platform.lsp.api.LspServerNotificationsHandler
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCustomization
import com.intellij.platform.lsp.api.customization.LspRenameCustomizer
import com.intellij.platform.lsp.api.customization.LspRenameSupport
import com.intellij.platform.lsp.api.customization.LspSemanticTokensCustomizer
import com.intellij.psi.PsiFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

private val LOG = logger<OftLspServerDescriptor>()

internal class OftLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "OpenFastTrace LSP") {

    override fun isSupportedFile(file: VirtualFile): Boolean = Companion.isSupportedFile(file)

    override fun createLsp4jClient(handler: LspServerNotificationsHandler): Lsp4jClient =
        OftLspClient(project, handler)

    override val lspCustomization: LspCustomization = object : LspCustomization() {

        override val semanticTokensCustomizer: LspSemanticTokensCustomizer = OftSemanticTokensSupport()

        override val renameCustomizer: LspRenameCustomizer = object : LspRenameSupport() {

            // only allow renames for OFT IDs in supported files
            override fun shouldRunRename(psiFile: PsiFile): Boolean {
                val file = psiFile.virtualFile ?: return false
                if (!Companion.isSupportedFile(file)) {
                    return false
                }
                val editor = FileEditorManager.getInstance(psiFile.project).selectedTextEditor
                    ?: return false
                return getRenameableRangeAtOffset(editor.document, editor.caretModel.offset) != null
            }

            override fun getRenameableRangeAtOffset(document: Document, offset: Int): TextRange? {
                val lineNumber = document.getLineNumber(offset)
                val lineStart = document.getLineStartOffset(lineNumber)
                val lineEnd = document.getLineEndOffset(lineNumber)
                val line = document.getText(TextRange(lineStart, lineEnd))
                val col = offset - lineStart
                return OFT_ID_PATTERN.findAll(line)
                    .firstOrNull { col in it.range.first..(it.range.last + 1) }
                    ?.let { TextRange(lineStart + it.range.first, lineStart + it.range.last + 1) }
            }
        }
    }

    override fun createCommandLine(): GeneralCommandLine {
        val jarPath = resolveServerJar()
        val java = javaExecutable()
        LOG.info("Starting OpenFastTrace LSP server with $java: $jarPath")
        return GeneralCommandLine(java, "-jar", jarPath)
    }

    private fun javaExecutable(): String {
        val launcher = Path.of(System.getProperty("java.home"), "bin",
            if (SystemInfo.isWindows) "java.exe" else "java")
        if (Files.isExecutable(launcher)) {
            return launcher.toString()
        }
        LOG.warn("No launcher in the IDE runtime at $launcher, falling back to 'java' on the PATH")
        return "java"
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

        private val OFT_ID_PATTERN = Regex("""\p{Alpha}+~\p{Alpha}[\w-]*(?:\.[\w-]+)*~\d+""")

        private val SUPPORTED_EXTENSIONS = setOf(
            "md", "markdown", "rst",
            "ads", "adb",
            "bat",
            "c", "cc", "cpp", "c++", "h", "hh", "hpp", "h++",
            "dox",
            "c#", "cs",
            "cfg", "conf", "ini",
            "feature",
            "go",
            "groovy",
            "htm", "html", "xhtml", "xml", "fxml", "json", "yaml", "yml", "toml",
            "java", "clj", "kt", "kts", "scala",
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
