package dev.gaphunter.turbologcompanion.generate

import dev.gaphunter.turbologcompanion.model.LOG_MARKER
import dev.gaphunter.turbologcompanion.model.LogContext

/**
 * Pure string rendering, no PSI mutation -- same separation already
 * proven in this catalog between "what should the text look like" and
 * "is it safe to write"/"where does it go" (see Bean Copy Companion's
 * `CopierWriter`/`InMemoryValidator` split).
 *
 * Class/method/line are baked in as literal text (never `getClass()`
 * or other runtime reflection) -- see [LogContext]'s KDoc for why.
 */
object LogStatementWriter {

    fun renderJava(ctx: LogContext): String {
        val label = label(ctx)
        return "System.out.println(\"$label \" + ${ctx.variableName});"
    }

    fun renderKotlin(ctx: LogContext): String {
        val label = label(ctx)
        return "println(\"$label \$${ctx.variableName}\")"
    }

    private fun label(ctx: LogContext): String =
        "$LOG_MARKER ${ctx.className}.${ctx.methodName}:${ctx.lineNumber} ~ ${ctx.variableName} ="
}
