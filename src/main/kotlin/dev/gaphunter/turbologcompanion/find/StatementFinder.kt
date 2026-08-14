package dev.gaphunter.turbologcompanion.find

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiTreeUtil
import dev.gaphunter.turbologcompanion.model.InsertionTarget
import dev.gaphunter.turbologcompanion.model.LogContext
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

/**
 * Resolves "the element under the caret" into a real, safe insertion
 * point -- never guesses. If the caret isn't on a variable/parameter/
 * field reference, or no block-level statement contains it, this
 * returns null and the action refuses with an honest notification
 * (see [dev.gaphunter.turbologcompanion.actions.InsertLogStatementAction]) --
 * same "degrade, never silently do the wrong thing" discipline as
 * every other Gap Hunter Labs plugin.
 *
 * Always anchors on the nearest ancestor whose direct parent is a real
 * block ([PsiCodeBlock] for Java, [KtBlockExpression] for Kotlin) --
 * e.g. a reference inside a braceless `if (x > 0) foo(x);` anchors on
 * the WHOLE if-statement, not mid-expression. Slightly less precise
 * placement in that rare case, but always syntactically safe: the new
 * statement is a real sibling of a real statement, never inserted
 * inside an expression or a statement header.
 */
object StatementFinder {

    fun find(elementAtCaret: PsiElement): InsertionTarget? {
        val javaRef = PsiTreeUtil.getParentOfType(elementAtCaret, PsiReferenceExpression::class.java, false)
        if (javaRef != null) findJava(javaRef)?.let { return it }

        // Caret on the variable's DECLARATION itself (e.g. `String
        // status = "PENDING";` when `status` is never read afterward) --
        // not a PsiReferenceExpression, so the branch above never
        // matches it. Found via live testing 2026-08-14: a variable
        // whose only appearance in the file is its own declaration
        // couldn't be logged at all, even though "log right where I just
        // declared it" is the single most common real workflow.
        val javaDecl = PsiTreeUtil.getParentOfType(elementAtCaret, PsiLocalVariable::class.java, false)
        if (javaDecl != null) findJavaDeclaration(javaDecl)?.let { return it }

        val ktRef = PsiTreeUtil.getParentOfType(elementAtCaret, KtSimpleNameExpression::class.java, false)
        if (ktRef != null) findKotlin(ktRef)?.let { return it }

        val ktDecl = PsiTreeUtil.getParentOfType(elementAtCaret, KtProperty::class.java, false)
        if (ktDecl != null && ktDecl.isLocal) findKotlinDeclaration(ktDecl)?.let { return it }

        return null
    }

    private fun findJava(reference: PsiReferenceExpression): InsertionTarget? {
        val resolved = reference.resolve() as? PsiVariable ?: return null
        if (resolved !is PsiLocalVariable && resolved !is PsiParameter && resolved !is PsiField) return null

        val anchor = walkUpToBlockChild(reference, isBlock = { it is PsiCodeBlock }) ?: return null
        val document = documentFor(anchor) ?: return null

        val className = PsiTreeUtil.getParentOfType(anchor, PsiClass::class.java)?.name ?: "TopLevel"
        val methodName = PsiTreeUtil.getParentOfType(anchor, PsiMethod::class.java)?.name ?: "init"
        val line = document.getLineNumber(anchor.textRange.startOffset) + 1

        return InsertionTarget(anchor, LogContext(resolved.name ?: return null, className, methodName, line))
    }

    private fun findJavaDeclaration(local: PsiLocalVariable): InsertionTarget? {
        val anchor = walkUpToBlockChild(local, isBlock = { it is PsiCodeBlock }) ?: return null
        val document = documentFor(anchor) ?: return null

        val className = PsiTreeUtil.getParentOfType(anchor, PsiClass::class.java)?.name ?: "TopLevel"
        val methodName = PsiTreeUtil.getParentOfType(anchor, PsiMethod::class.java)?.name ?: "init"
        val line = document.getLineNumber(anchor.textRange.startOffset) + 1

        return InsertionTarget(anchor, LogContext(local.name ?: return null, className, methodName, line))
    }

    private fun findKotlin(reference: KtSimpleNameExpression): InsertionTarget? {
        // PsiElement.getReferences() (plain platform API) instead of the
        // Kotlin-specific `mainReference` extension -- same PSI-only,
        // no-Analysis-API discipline already proven elsewhere in this
        // catalog (see api-security-companion's KotlinTypeAnnotationResolver),
        // and avoids depending on an extension whose exact package path
        // varies across Kotlin plugin versions.
        val resolved = reference.references.firstNotNullOfOrNull { it.resolve() } ?: return null
        if (resolved !is KtProperty && resolved !is KtParameter) return null

        val anchor = walkUpToBlockChild(reference, isBlock = { it is KtBlockExpression }) ?: return null
        val document = documentFor(anchor) ?: return null

        val className = PsiTreeUtil.getParentOfType(anchor, KtClassOrObject::class.java)?.name ?: "TopLevel"
        val methodName = PsiTreeUtil.getParentOfType(anchor, KtNamedFunction::class.java)?.name ?: "init"
        val line = document.getLineNumber(anchor.textRange.startOffset) + 1

        return InsertionTarget(anchor, LogContext(reference.getReferencedName(), className, methodName, line))
    }

    private fun findKotlinDeclaration(property: KtProperty): InsertionTarget? {
        val anchor = walkUpToBlockChild(property, isBlock = { it is KtBlockExpression }) ?: return null
        val document = documentFor(anchor) ?: return null

        val className = PsiTreeUtil.getParentOfType(anchor, KtClassOrObject::class.java)?.name ?: "TopLevel"
        val methodName = PsiTreeUtil.getParentOfType(anchor, KtNamedFunction::class.java)?.name ?: "init"
        val line = document.getLineNumber(anchor.textRange.startOffset) + 1

        return InsertionTarget(anchor, LogContext(property.name ?: return null, className, methodName, line))
    }

    private fun walkUpToBlockChild(start: PsiElement, isBlock: (PsiElement) -> Boolean): PsiElement? {
        var current = start
        while (true) {
            val parent = current.parent ?: return null
            if (isBlock(parent)) return current
            current = parent
        }
    }

    private fun documentFor(element: PsiElement): Document? =
        element.containingFile?.viewProvider?.document
}
