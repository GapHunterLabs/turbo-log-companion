package dev.gaphunter.turbologcompanion.generate

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.codeStyle.CodeStyleManager
import dev.gaphunter.turbologcompanion.model.InsertionTarget
import org.jetbrains.kotlin.psi.KtPsiFactory

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

            val newlineAndStatement: Pair<PsiElement, PsiElement> = if (isJava) {
                val factory = JavaPsiFacade.getElementFactory(project)
                val newStatement = factory.createStatementFromText(statementText, anchor)
                val newline = PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n")
                val insertedNewline = parent.addAfter(newline, anchor)
                val insertedStatement = parent.addAfter(newStatement, insertedNewline)
                insertedNewline to insertedStatement
            } else {
                val factory = KtPsiFactory(project)
                val newExpression = factory.createExpression(statementText)
                val newline = factory.createNewLine()
                val insertedNewline = parent.addAfter(newline, anchor)
                val insertedStatement = parent.addAfter(newExpression, insertedNewline)
                insertedNewline to insertedStatement
            }

            CodeStyleManager.getInstance(project).reformatRange(
                parent, anchor.textRange.startOffset, newlineAndStatement.second.textRange.endOffset,
            )
        })
    }
}
