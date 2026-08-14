package dev.gaphunter.turbologcompanion.generate

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

/**
 * The actual PSI mutation behind "Remove All Log Statements", kept
 * separate from [dev.gaphunter.turbologcompanion.actions.RemoveAllLogStatementsAction]
 * for the same direct-unit-testability reason as [LogStatementInserter].
 */
object LogStatementRemover {

    fun removeAll(project: Project, elements: List<PsiElement>): Int {
        if (elements.isEmpty()) return 0

        WriteCommandAction.runWriteCommandAction(project, "Remove All Log Statements", null, {
            // Delete from LAST to FIRST (by document offset) so an
            // earlier element's offset/validity is never affected by
            // removing a later one first.
            for (element in elements.sortedByDescending { it.textRange.startOffset }) {
                val parent = element.parent ?: continue
                val precedingWhitespace = element.prevSibling as? PsiWhiteSpace

                // Delete the statement AND its leading whitespace as ONE
                // deleteChildRange call, not two separate .delete()
                // calls. Real bug found via live testing 2026-08-14:
                // `element.delete()` followed by
                // `precedingWhitespace.delete()` threw
                // PsiInvalidElementAccessException -- deleting the
                // statement silently invalidated the already-captured
                // whitespace reference (leaf whitespace nodes get
                // coalesced/recreated as a side effect of an adjacent
                // structural edit, a known PSI gotcha). It also meant
                // only the first of several inserted statements ever
                // got removed -- the exception aborted the write action
                // partway through, and whatever had already been
                // deleted stayed deleted. deleteChildRange performs
                // both removals as a single tree mutation, so there's
                // no stale-reference window at all.
                if (precedingWhitespace != null) {
                    parent.deleteChildRange(precedingWhitespace, element)
                } else {
                    element.delete()
                }
            }
        })

        return elements.size
    }
}
