package dev.gaphunter.turbologcompanion.actions

import com.intellij.lang.java.JavaLanguage
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import dev.gaphunter.turbologcompanion.find.StatementFinder
import dev.gaphunter.turbologcompanion.generate.InMemoryValidator
import dev.gaphunter.turbologcompanion.generate.LogStatementInserter
import dev.gaphunter.turbologcompanion.generate.LogStatementWriter
import dev.gaphunter.turbologcompanion.model.InsertionTarget

/**
 * Editor context-menu entry point. Caret must be on a variable,
 * parameter, or field reference -- resolution is [StatementFinder]'s
 * job, this class only orchestrates: find target -> render text off
 * the EDT -> validate -> insert on the EDT (the actual PSI mutation is
 * [LogStatementInserter], kept separate so it's directly unit-testable
 * without racing this class's background-thread dispatch). Same
 * threading discipline as every other Gap Hunter Labs plugin.
 */
class InsertLogStatementAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val target = resolveTarget(e)
        e.presentation.isEnabledAndVisible = project != null && !DumbService.isDumb(project) && target != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val target = resolveTarget(e) ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                val isJava = target.anchorStatement.language == JavaLanguage.INSTANCE
                val statementText = if (isJava) {
                    LogStatementWriter.renderJava(target.context)
                } else {
                    LogStatementWriter.renderKotlin(target.context)
                }

                val error = if (isJava) {
                    InMemoryValidator.findFirstJavaSyntaxError(project, statementText)
                } else {
                    InMemoryValidator.findFirstKotlinSyntaxError(project, statementText)
                }
                if (error != null) {
                    notify(project, "Could not insert a log statement safely here ($error) -- nothing was written.", NotificationType.ERROR)
                    return@runReadAction
                }

                ApplicationManager.getApplication().invokeLater {
                    LogStatementInserter.insert(project, target, statementText, isJava)
                    notify(project, "Log statement inserted for '${target.context.variableName}'.", NotificationType.INFORMATION)
                }
            }
        }
    }

    private fun resolveTarget(e: AnActionEvent): InsertionTarget? {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return null
        val elementAtCaret = psiFile.findElementAt(editor.caretModel.offset) ?: return null
        return StatementFinder.find(elementAtCaret)
    }

    private fun notify(project: Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Turbo Log Companion")
            .createNotification(message, type)
            .notify(project)
    }
}
