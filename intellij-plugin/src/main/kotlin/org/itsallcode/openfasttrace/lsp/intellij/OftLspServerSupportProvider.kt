package org.itsallcode.openfasttrace.lsp.intellij

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.LspServerSupportProvider.LspServerStarter

internal class OftLspServerSupportProvider : LspServerSupportProvider {

    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerStarter,
    ) {
        if (OftLspServerDescriptor.isSupportedFile(file)) {
            serverStarter.ensureServerStarted(OftLspServerDescriptor(project))
        }
    }
}
