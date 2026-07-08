package org.itsallcode.openfasttrace.lsp.intellij

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState

internal class OftCompletionConfidence : CompletionConfidence() {

    override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        val virtualFile = psiFile.virtualFile ?: return ThreeState.UNSURE
        if (!OftLspServerDescriptor.isSupportedFile(virtualFile)) {
            return ThreeState.UNSURE
        }
        val document = psiFile.viewProvider.document ?: return ThreeState.UNSURE
        val lineStart = document.getLineStartOffset(document.getLineNumber(offset))
        val linePrefix = document.getText(TextRange(lineStart, offset))
        return when {
            isInOpenCoverageTag(linePrefix) -> ThreeState.NO
            containsCodeCommentMarker(linePrefix) -> ThreeState.YES
            else -> ThreeState.UNSURE
        }
    }

    private fun isInOpenCoverageTag(linePrefix: String): Boolean {
        val bracketStart = linePrefix.lastIndexOf('[')
        if (bracketStart < 0) {
            return false
        }
        val beforeBracket = linePrefix.substring(0, bracketStart)
        if (!containsCodeCommentMarker(beforeBracket)) {
            return false
        }
        val afterBracket = linePrefix.substring(bracketStart)
        return afterBracket.contains("->") && !afterBracket.contains(']')
    }

    private fun containsCodeCommentMarker(text: String): Boolean =
        CODE_COMMENT_MARKERS.any(text::contains)

    companion object {
        private val CODE_COMMENT_MARKERS = listOf("//", "#", "--", ";", "/*", "<!--")
    }
}
