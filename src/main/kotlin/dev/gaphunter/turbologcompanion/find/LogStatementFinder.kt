package dev.gaphunter.turbologcompanion.find

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import dev.gaphunter.turbologcompanion.model.LOG_MARKER
import org.jetbrains.kotlin.psi.KtBlockExpression

/**
 * Finds every statement this plugin previously inserted, for "Remove
 * All Log Statements" -- matches on the exact opening of the label
 * text `InsertLogStatementAction`/`LogStatementWriter` always renders
 * (`"TCLC `), restricted to elements that are themselves a direct
 * child of a real block. That second condition matters: without it, a
 * match could land on the string literal itself (a child of the
 * statement) instead of the whole statement, or on an unrelated
 * expression that merely CONTAINS one of our statements as a
 * sub-element. Stops descending once it finds a block-level match, so
 * a single inserted statement is never reported twice.
 */
object LogStatementFinder {

    private val MARKER_TEXT = "\"$LOG_MARKER "

    fun findAllInFile(file: PsiFile): List<PsiElement> {
        val found = mutableListOf<PsiElement>()
        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                ProgressManager.checkCanceled()
                val parent = element.parent
                val isBlockChild = parent is PsiCodeBlock || parent is KtBlockExpression
                if (isBlockChild && element.text.contains(MARKER_TEXT)) {
                    found += element
                    return // don't descend into our own match
                }
                super.visitElement(element)
            }
        })
        return found
    }
}
