package org.itsallcode.openfasttrace.lsp.intellij

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.lsp.api.LspServerManager

private val LOG = logger<OftLspStartupActivity>()

internal class OftLspStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        LOG.info("Starting OpenFastTrace LSP server for project ${project.name}")
        LspServerManager.getInstance(project).ensureServerStarted(
            OftLspServerSupportProvider::class.java,
            OftLspServerDescriptor(project),
        )
    }
}
