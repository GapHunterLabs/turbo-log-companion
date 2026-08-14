package dev.gaphunter.turbologcompanion.generate

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.gaphunter.turbologcompanion.find.LogStatementFinder
import dev.gaphunter.turbologcompanion.find.StatementFinder

class LogStatementInserterTest : BasePlatformTestCase() {

    fun testInsertsAValidStatementIntoARealJavaFile() {
        myFixture.configureByText(
            "Acme.java",
            """
            class Acme {
                void method() {
                    int total = 1 + 2;
                    System.out.println(tot<caret>al);
                }
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!
        val target = StatementFinder.find(element)!!
        val statementText = LogStatementWriter.renderJava(target.context)
        assertNull(InMemoryValidator.findFirstJavaSyntaxError(project, statementText))

        LogStatementInserter.insert(project, target, statementText, isJava = true)

        val text = myFixture.file.text
        assertTrue("expected an inserted TCLC statement, got:\n$text", text.contains("TCLC"))
        assertTrue(text.contains("total ="))
        assertEquals(1, LogStatementFinder.findAllInFile(myFixture.file).size)
        // The file as a whole must still parse clean after the real insertion.
        assertNoPsiErrors(myFixture.file)
    }

    fun testInsertsAValidStatementIntoARealKotlinFile() {
        myFixture.configureByText(
            "Acme.kt",
            """
            class Acme {
                fun method() {
                    val total = 1 + 2
                    println(tot<caret>al)
                }
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!
        val target = StatementFinder.find(element)!!
        val statementText = LogStatementWriter.renderKotlin(target.context)
        assertNull(InMemoryValidator.findFirstKotlinSyntaxError(project, statementText))

        LogStatementInserter.insert(project, target, statementText, isJava = false)

        val text = myFixture.file.text
        assertTrue("expected an inserted TCLC statement, got:\n$text", text.contains("TCLC"))
        assertEquals(1, LogStatementFinder.findAllInFile(myFixture.file).size)
        assertNoPsiErrors(myFixture.file)
    }

    private fun assertNoPsiErrors(file: com.intellij.psi.PsiFile) {
        val error = com.intellij.psi.util.PsiTreeUtil.findChildOfType(file, com.intellij.psi.PsiErrorElement::class.java)
        assertNull("file has a real syntax error after insertion: ${error?.errorDescription}", error)
    }
}
