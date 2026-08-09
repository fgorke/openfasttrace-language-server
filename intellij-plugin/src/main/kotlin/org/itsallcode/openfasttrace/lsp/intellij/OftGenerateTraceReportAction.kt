package org.itsallcode.openfasttrace.lsp.intellij

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import org.eclipse.lsp4j.ExecuteCommandParams
import java.nio.file.Path

private val LOG = logger<OftGenerateTraceReportAction>()

private const val GENERATE_TRACE_REPORT_COMMAND = "oft.generateTraceReport"

private const val REPORT_TIMEOUT_MS = 60_000

private val PRESETS = listOf(
    "html" to "HTML report",
    "plain-all" to "Plain text, every item",
    "plain-failures" to "Plain text, defects only",
    "plain-direct-failures" to "Plain text, defects only, without inherited ones",
    "plain-summary" to "Plain text, one line summary",
)

internal class OftGenerateTraceReportAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project?.let { runningServer(it) != null } == true
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val server = runningServer(project) ?: return

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(PRESETS.map { it.second })
            .setTitle("Generate OpenFastTrace Report")
            .setItemChosenCallback { chosen ->
                val preset = PRESETS.first { it.second == chosen }.first
                generate(project, server, preset)
            }
            .createPopup()
            .showCenteredInCurrentWindow(project)
    }

    private fun generate(project: Project, server: LspServer, preset: String) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val params = ExecuteCommandParams(GENERATE_TRACE_REPORT_COMMAND, listOf(preset))
            val path = server.sendRequestSync(REPORT_TIMEOUT_MS) {
                it.workspaceService.executeCommand(params)
            } as? String
            if (path == null) {
                LOG.warn("The language server returned no report path for preset '$preset'")
                return@executeOnPooledThread
            }
            openInEditor(project, path)
        }
    }

    private fun openInEditor(project: Project, path: String) {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(Path.of(path))
        if (file == null) {
            LOG.warn("Could not find the generated report at $path")
            return
        }
        ApplicationManager.getApplication().invokeLater {
            FileEditorManager.getInstance(project).openFile(file, true)
        }
    }

    private fun runningServer(project: Project): LspServer? =
        LspServerManager.getInstance(project)
            .getServersForProvider(OftLspServerSupportProvider::class.java)
            .firstOrNull()
}
