// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.base.psi

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.DocumentUtil
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import kotlin.math.abs

fun PsiFile.getLineStartOffset(line: Int): Int? {
    return getLineStartOffset(line, skipWhitespace = true)
}

fun PsiFile.getLineStartOffset(line: Int, skipWhitespace: Boolean): Int? {
    val doc = viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(this)
    if (doc != null && DocumentUtil.isValidLine(line, doc)) {
        val startOffset = doc.getLineStartOffset(line)
        val element = findElementAt(startOffset) ?: return startOffset

        if (skipWhitespace && (element is PsiWhiteSpace || element is PsiComment)) {
            return PsiTreeUtil.skipSiblingsForward(element, PsiWhiteSpace::class.java, PsiComment::class.java)?.startOffset ?: startOffset
        }
        return startOffset
    }

    return null
}

fun PsiFile.getLineEndOffset(line: Int): Int? {
    val document = viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(this)
    if (document != null && DocumentUtil.isValidLine(line, document)) {
        return document.getLineEndOffset(line)
    }
    return null
}

fun PsiFile.getLineNumber(offset: Int): Int? {
    val document = viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(this) ?: return null
    return runCatching { document.getLineNumber(offset) }.getOrNull()
}

fun PsiElement.getLineNumber(start: Boolean = true): Int {
    val document = containingFile.viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(containingFile)
    val index = if (start) this.startOffset else this.endOffset
    if (index > (document?.textLength ?: 0)) return 0
    return document?.getLineNumber(index) ?: 0
}

// Copied to formatter
fun PsiElement.getLineCount(): Int {
    val doc = containingFile?.let { file -> PsiDocumentManager.getInstance(project).getDocument(file) }
    if (doc != null) {
        val spaceRange = textRange ?: TextRange.EMPTY_RANGE

        if (spaceRange.endOffset <= doc.textLength && spaceRange.startOffset < spaceRange.endOffset) {
            val startLine = doc.getLineNumber(spaceRange.startOffset)
            val endLine = doc.getLineNumber(spaceRange.endOffset)

            return endLine - startLine + 1
        }
    }

    return StringUtil.getLineBreakCount(text ?: error("Cannot count number of lines")) + 1
}

fun PsiElement.isMultiLine() = getLineCount() > 1

fun PsiElement.isOneLiner() = getLineCount() == 1

fun Document.getLineCountInRange(textRange: TextRange): Int = abs(getLineNumber(textRange.startOffset) - getLineNumber(textRange.endOffset))

fun Document.containsLineBreakInRange(textRange: TextRange): Boolean = getLineCountInRange(textRange) != 0
