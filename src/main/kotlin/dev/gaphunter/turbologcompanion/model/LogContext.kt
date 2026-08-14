package dev.gaphunter.turbologcompanion.model

import com.intellij.psi.PsiElement

/**
 * Everything needed to render one "TCLC"-marked log statement --
 * class/method/line are captured as plain strings/ints at PLAN time
 * (from real PSI), never re-derived at runtime via reflection
 * (`getClass()` etc.) -- that would break in a static context or a
 * Kotlin top-level function. Baking them in as literal text works
 * identically everywhere.
 */
data class LogContext(
    val variableName: String,
    val className: String,
    val methodName: String,
    val lineNumber: Int,
)

/** Where in the PSI tree the new statement must be inserted, and what it should say. */
data class InsertionTarget(
    val anchorStatement: PsiElement,
    val context: LogContext,
)

/** Shared marker so a later "Remove All Log Statements" can find exactly (and only) what this plugin inserted. */
const val LOG_MARKER = "TCLC"
