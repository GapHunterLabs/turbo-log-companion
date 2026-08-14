package dev.gaphunter.turbologcompanion.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import dev.gaphunter.turbologcompanion.find.LogStatementFinder

/**
 * Removes every statement [InsertLogStatementAction] previously
 * inserted into the current file, in one action -- the direct
 * companion to "insert", so temporary debug prints never get left
 * behind by accident (a real, well-known complaint pattern against
 * ad-hoc `println` debugging in general, not any specific competitor
 * -- this plugin has no direct JetBrains Marketplace competitor to
 * cite, see README "Why it exists").
 */
class RemoveAllLogStatementsAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        // getActionUpdateThread() = BGT already guarantees read access here
        // (same convention as GenerateBeanCopyAction/GenerateTestSkeletonAction's
        // update() methods elsewhere in this catalog -- no explicit
        // runReadAction wrapping needed on top of the BGT contract).
        if (project == null || psiFile == null || DumbService.isDumb(project)) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        e.presentation.isEnabledAndVisible = LogStatementFinder.findAllInFile(psiFile).isNotEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            val elements = ApplicationManager.getApplication().runReadAction<List<PsiElement>> {
                LogStatementFinder.findAllInFile(psiFile)
            }
            if (elements.isEmpty()) {
                notify(project, "No Turbo Log Companion statements found in this file.", NotificationType.INFORMATION)
                return@executeOnPooledThread
            }

            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project, "Remove All Log Statements", null, {
                    // Delete from LAST to FIRST so earlier elements' text ranges/validity
                    // aren't invalidated by removing a later one first.
                    for (element in elements.sortedByDescending { it.textRange.startOffset }) {
                        val precedingWhitespace = element.prevSibling as? PsiWhiteSpace
                        element.delete()
                        precedingWhitespace?.delete()
                    }
                })
                notify(project, "Removed ${elements.size} log statement(s).", NotificationType.INFORMATION)
            }
        }
    }

    private fun notify(project: Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Turbo Log Companion")
            .createNotification(message, type)
            .notify(project)
    }
}
