package org.itsallcode.openfasttrace.lsp.intellij

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.Lsp4jClient
import com.intellij.platform.lsp.api.LspServerNotificationsHandler
import org.eclipse.lsp4j.PublishDiagnosticsParams

internal class OftLspClient(project: Project, handler: LspServerNotificationsHandler) :
    Lsp4jClient(ProblemReportingHandler(project, handler))

private class ProblemReportingHandler(
    private val project: Project,
    private val delegate: LspServerNotificationsHandler,
) : LspServerNotificationsHandler by delegate {

    override fun publishDiagnostics(params: PublishDiagnosticsParams) {
        delegate.publishDiagnostics(params)
        project.service<OftProblemsReporter>().diagnosticsPublished(params)
    }
}
