package dev.gaphunter.turbologcompanion.generate

import dev.gaphunter.turbologcompanion.model.LogContext
import junit.framework.TestCase

class LogStatementWriterTest : TestCase() {

    fun testRendersAValidJavaPrintlnWithBakedInContext() {
        val text = LogStatementWriter.renderJava(LogContext("total", "Acme", "method", 42))

        assertEquals(
            """System.out.println("TCLC Acme.method:42 ~ total = " + total);""",
            text,
        )
    }

    fun testRendersAValidKotlinPrintlnWithBakedInContext() {
        val text = LogStatementWriter.renderKotlin(LogContext("total", "Acme", "method", 42))

        assertEquals(
            """println("TCLC Acme.method:42 ~ total = ${'$'}total")""",
            text,
        )
    }

    fun testNeverCallsGetClassOrOtherRuntimeReflection() {
        val java = LogStatementWriter.renderJava(LogContext("x", "TopLevel", "init", 1))
        val kotlin = LogStatementWriter.renderKotlin(LogContext("x", "TopLevel", "init", 1))

        assertFalse(java.contains("getClass"))
        assertFalse(kotlin.contains("::class"))
    }
}
