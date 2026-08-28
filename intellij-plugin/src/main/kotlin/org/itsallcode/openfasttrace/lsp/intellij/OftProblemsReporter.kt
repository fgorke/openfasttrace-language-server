package org.itsallcode.openfasttrace.lsp.intellij

import com.intellij.analysis.problemsView.FileProblem
import com.intellij.analysis.problemsView.ProblemsCollector
import com.intellij.analysis.problemsView.ProblemsProvider
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.PublishDiagnosticsParams
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

private val LOG = logger<OftProblemsReporter>()

@Service(Service.Level.PROJECT)
// [impl->adr~report-project-wide-trace-problems-in-intellij~1]
internal class OftProblemsReporter(override val project: Project) : ProblemsProvider {

    private val reportedProblems = ConcurrentHashMap<String, Set<FileProblem>>()

    fun diagnosticsPublished(params: PublishDiagnosticsParams) {
        val file = findFile(params.uri) ?: return
        val current = params.diagnostics.orEmpty()
            .map {
                OftFileProblem(this, file, it.message, it.range.start.line,
                    it.range.start.character, it.severity)
            }
            .toSet()
        val previous = if (current.isEmpty()) {
            reportedProblems.remove(params.uri).orEmpty()
        } else {
            reportedProblems.put(params.uri, current).orEmpty()
        }
        applyDifference(previous, current)
    }

    private fun applyDifference(previous: Set<FileProblem>, current: Set<FileProblem>) {
        val removed = previous - current
        val added = current - previous
        if (removed.isEmpty() && added.isEmpty()) {
            return
        }
        ApplicationManager.getApplication().invokeLater({
            if (project.isDisposed) {
                return@invokeLater
            }
            val collector = ProblemsCollector.getInstance(project)
            removed.forEach(collector::problemDisappeared)
            added.forEach(collector::problemAppeared)
        }, project.disposed)
    }

    private fun findFile(uri: String): VirtualFile? {
        return try {
            val path = Path.of(URI.create(uri))
            val fileSystem = LocalFileSystem.getInstance()
            fileSystem.findFileByNioFile(path) ?: fileSystem.refreshAndFindFileByNioFile(path)
        } catch (exception: RuntimeException) {
            LOG.warn("Could not resolve $uri: ${exception.message}")
            null
        }
    }
}

private data class OftFileProblem(
    override val provider: ProblemsProvider,
    override val file: VirtualFile,
    override val text: String,
    override val line: Int,
    override val column: Int,
    private val severity: DiagnosticSeverity?,
) : FileProblem {

    override val icon: Icon
        get() = when (severity) {
            DiagnosticSeverity.Error -> HighlightDisplayLevel.ERROR.icon
            DiagnosticSeverity.Warning -> HighlightDisplayLevel.WARNING.icon
            DiagnosticSeverity.Hint -> HighlightDisplayLevel.WEAK_WARNING.icon
            else -> AllIcons.General.Information
        }

    override fun toString(): String = "$text (${file.name}:${line + 1})"
}
