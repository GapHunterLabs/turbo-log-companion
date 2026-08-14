package dev.gaphunter.turbologcompanion.generate

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinLanguage

/**
 * Same discipline as every other Gap Hunter Labs code-generating
 * plugin: the statement text is parsed into a throwaway, never-
 * persisted PSI file (wrapped in a minimal valid skeleton so a bare
 * statement is legal to parse on its own) and checked for syntax
 * errors BEFORE anything is inserted into the real file. Never added
 * to a real [com.intellij.psi.PsiDirectory], zero risk regardless of
 * what it contains.
 */
object InMemoryValidator {

    fun findFirstJavaSyntaxError(project: Project, statementText: String): String? {
        val wrapped = "final class __TCLC_Probe { void __m() { $statementText } }"
        return firstErrorIn(project, wrapped, "__TCLCProbe.java", JavaLanguage.INSTANCE)
    }

    fun findFirstKotlinSyntaxError(project: Project, statementText: String): String? {
        val wrapped = "private fun __tclcProbe() { $statementText }"
        return firstErrorIn(project, wrapped, "__TCLCProbe.kt", KotlinLanguage.INSTANCE)
    }

    private fun firstErrorIn(project: Project, text: String, fileName: String, language: com.intellij.lang.Language): String? {
        val psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, language, text, /* eventSystemEnabled = */ false, /* markAsCopy = */ true)
        val firstError = PsiTreeUtil.findChildOfType(psiFile, PsiErrorElement::class.java)
        return firstError?.errorDescription
    }
}
