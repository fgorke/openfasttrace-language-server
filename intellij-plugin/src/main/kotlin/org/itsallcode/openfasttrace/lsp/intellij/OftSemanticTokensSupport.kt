package org.itsallcode.openfasttrace.lsp.intellij

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.platform.lsp.api.customization.LspSemanticTokensSupport
import com.intellij.psi.PsiFile

internal class OftSemanticTokensSupport : LspSemanticTokensSupport() {

    override fun shouldAskServerForSemanticTokens(psiFile: PsiFile): Boolean {
        val virtualFile = psiFile.virtualFile ?: return false
        return OftLspServerDescriptor.isSupportedFile(virtualFile)
    }

    override fun getTextAttributesKey(tokenType: String, modifiers: List<String>): TextAttributesKey? =
        when (tokenType) {
            "type" -> DefaultLanguageHighlighterColors.STRING
            "keyword" -> DefaultLanguageHighlighterColors.KEYWORD
            "macro" -> DefaultLanguageHighlighterColors.METADATA
            else -> null
        }
}
