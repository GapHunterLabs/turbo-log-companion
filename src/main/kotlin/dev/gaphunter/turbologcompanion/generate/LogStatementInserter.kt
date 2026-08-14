package dev.gaphunter.turbologcompanion.generate

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.PsiThrowStatement
import com.intellij.psi.codeStyle.CodeStyleManager
import dev.gaphunter.turbologcompanion.model.InsertionTarget
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtThrowExpression

/**
 * The actual PSI mutation, kept separate from
 * [dev.gaphunter.turbologcompanion.actions.InsertLogStatementAction]
 * specifically so it's directly unit-testable (call it synchronously,
 * wrapped in [WriteCommandAction], no pooled-thread/`invokeLater`
 * indirection needed) -- the action itself still dispatches through a
 * background thread for the PSI read + validation (CONSTITUTION.md
 * section 6), but that threading is a production-performance concern,
 * not something a correctness test should have to race against.
 */
object LogStatementInserter {

    fun insert(project: Project, target: InsertionTarget, statementText: String, isJava: Boolean) {
        WriteCommandAction.runWriteCommandAction(project, "Insert Log Statement", null, {
            val anchor = target.anchorStatement
            val parent = anchor.parent ?: return@runWriteCommandAction

            // A `return`/`throw` anchor is a terminal statement -- any
            // statement placed AFTER it is unreachable code, a real
            // compile error, not just a style nit. Found via live
            // testing 2026-08-14: logging the value in `return total;`
            // landed the log call after the return, producing exactly
            // that. For these, insert BEFORE the anchor instead of
            // after -- still logs the same value, right before control
            // leaves the block, without creating dead code.
            val insertBefore = when {
                isJava -> anchor is PsiReturnStatement || anchor is PsiThrowStatement
                else -> anchor is KtReturnExpression || anchor is KtThrowExpression
            }

            // Whichever side we insert on, always insert the STATEMENT
            // first (relative to the stable `anchor`), then the newline
            // relative to that freshly-added statement -- never chain a
            // second insertion off a just-created whitespace node. Doing
            // it the other way around (newline first) is what caused a
            // separate real bug found via live testing 2026-08-14: when
            // `anchor` was the last statement in a block, the whitespace
            // node returned by the first `addAfter` could get folded into
            // the block's existing trailing whitespace, and the second
            // `addAfter` then landed the statement outside the block
            // entirely.
            val newlineAndStatement: Pair<PsiElement, PsiElement> = if (isJava) {
                val factory = JavaPsiFacade.getElementFactory(project)
                val newStatement = factory.createStatementFromText(statementText, anchor)
                val newline = PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n")
                if (insertBefore) {
                    val insertedStatement = parent.addBefore(newStatement, anchor)
                    val insertedNewline = parent.addAfter(newline, insertedStatement)
                    insertedNewline to insertedStatement
                } else {
                    val insertedStatement = parent.addAfter(newStatement, anchor)
                    val insertedNewline = parent.addBefore(newline, insertedStatement)
                    insertedNewline to insertedStatement
                }
            } else {
                val factory = KtPsiFactory(project)
                val newExpression = factory.createExpression(statementText)
                val newline = factory.createNewLine()
                if (insertBefore) {
                    val insertedStatement = parent.addBefore(newExpression, anchor)
                    val insertedNewline = parent.addAfter(newline, insertedStatement)
                    insertedNewline to insertedStatement
                } else {
                    val insertedStatement = parent.addAfter(newExpression, anchor)
                    val insertedNewline = parent.addBefore(newline, insertedStatement)
                    insertedNewline to insertedStatement
                }
            }

            // With insertBefore, the new statement now sits BEFORE
            // `anchor` in the file -- anchor.startOffset is no longer
            // guaranteed to be the smaller bound, so take min/max
            // instead of assuming anchor comes first.
            val insertedStatement = newlineAndStatement.second
            val rangeStart = minOf(anchor.textRange.startOffset, insertedStatement.textRange.startOffset)
            val rangeEnd = maxOf(anchor.textRange.endOffset, insertedStatement.textRange.endOffset)
            CodeStyleManager.getInstance(project).reformatRange(parent, rangeStart, rangeEnd)
        })
    }
}
