package dev.gaphunter.turbologcompanion.generate

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class InMemoryValidatorTest : BasePlatformTestCase() {

    fun testFindsNoErrorInAValidJavaStatement() {
        val error = InMemoryValidator.findFirstJavaSyntaxError(
            project, """System.out.println("TCLC Acme.method:1 ~ x = " + x);""",
        )
        assertNull(error)
    }

    fun testFindsASyntaxErrorInAMalformedJavaStatement() {
        val error = InMemoryValidator.findFirstJavaSyntaxError(
            project, """System.out.println("x" + x""", // missing closing paren + semicolon
        )
        assertNotNull(error)
    }

    fun testFindsNoErrorInAValidKotlinStatement() {
        val error = InMemoryValidator.findFirstKotlinSyntaxError(
            project, """println("TCLC Acme.method:1 ~ x = ${'$'}x")""",
        )
        assertNull(error)
    }

    fun testFindsASyntaxErrorInAMalformedKotlinStatement() {
        val error = InMemoryValidator.findFirstKotlinSyntaxError(
            project, """println("x" + x""", // missing closing paren
        )
        assertNotNull(error)
    }
}
