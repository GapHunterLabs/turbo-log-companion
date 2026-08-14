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

    fun testInsertsCorrectlyWhenTheTargetIsTheOnlyStatementInTheBlock() {
        // Reproduces a real bug found via live runIde testing 2026-08-14:
        // an assignment statement that is the LAST (here, only)
        // statement in a block, immediately followed by the block's
        // closing brace, landed the new statement AFTER that closing
        // brace instead of inside the block -- invalid Java.
        myFixture.configureByText(
            "Acme.java",
            """
            class Acme {
                private int retryCount = 0;

                void retry() {
                    retry<caret>Count = retryCount + 1;
                }
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!
        val target = StatementFinder.find(element)!!
        val statementText = LogStatementWriter.renderJava(target.context)

        LogStatementInserter.insert(project, target, statementText, isJava = true)

        val text = myFixture.file.text
        val retryMethodEnd = text.indexOf("void retry()").let { text.indexOf("}", it) }
        val classEnd = text.lastIndexOf("}")
        assertTrue(
            "expected the inserted TCLC statement BEFORE retry()'s closing brace, got:\n$text",
            text.indexOf("TCLC") in 0 until retryMethodEnd,
        )
        assertTrue(retryMethodEnd < classEnd)
        assertNoPsiErrors(myFixture.file)
    }

    fun testInsertsBeforeAJavaReturnStatementInsteadOfAfter() {
        // Real bug found via live runIde testing 2026-08-14: logging the
        // value in `return total;` landed the log call AFTER the
        // return -- unreachable code, a real compile error (not caught
        // by assertNoPsiErrors, which only checks for parse errors).
        myFixture.configureByText(
            "Acme.java",
            """
            class Acme {
                double method() {
                    double total = 1.0 + 2.0;
                    return tot<caret>al;
                }
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!
        val target = StatementFinder.find(element)!!
        val statementText = LogStatementWriter.renderJava(target.context)

        LogStatementInserter.insert(project, target, statementText, isJava = true)

        val text = myFixture.file.text
        val tclcIndex = text.indexOf("TCLC")
        val returnIndex = text.indexOf("return total;")
        assertTrue("expected TCLC log BEFORE the return, got:\n$text", tclcIndex in 0 until returnIndex)
        assertNoPsiErrors(myFixture.file)
    }

    fun testInsertsBeforeAJavaThrowStatementInsteadOfAfter() {
        myFixture.configureByText(
            "Acme.java",
            """
            class Acme {
                void method(String reason) {
                    throw new IllegalStateException(reas<caret>on);
                }
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!
        val target = StatementFinder.find(element)!!
        val statementText = LogStatementWriter.renderJava(target.context)

        LogStatementInserter.insert(project, target, statementText, isJava = true)

        val text = myFixture.file.text
        val tclcIndex = text.indexOf("TCLC")
        val throwIndex = text.indexOf("throw new")
        assertTrue("expected TCLC log BEFORE the throw, got:\n$text", tclcIndex in 0 until throwIndex)
        assertNoPsiErrors(myFixture.file)
    }

    fun testInsertsBeforeAKotlinReturnExpressionInsteadOfAfter() {
        myFixture.configureByText(
            "Acme.kt",
            """
            class Acme {
                fun method(): Double {
                    val total = 1.0 + 2.0
                    return tot<caret>al
                }
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!
        val target = StatementFinder.find(element)!!
        val statementText = LogStatementWriter.renderKotlin(target.context)

        LogStatementInserter.insert(project, target, statementText, isJava = false)

        val text = myFixture.file.text
        val tclcIndex = text.indexOf("TCLC")
        val returnIndex = text.indexOf("return total")
        assertTrue("expected TCLC log BEFORE the return, got:\n$text", tclcIndex in 0 until returnIndex)
        assertNoPsiErrors(myFixture.file)
    }

    private fun assertNoPsiErrors(file: com.intellij.psi.PsiFile) {
        val error = com.intellij.psi.util.PsiTreeUtil.findChildOfType(file, com.intellij.psi.PsiErrorElement::class.java)
        assertNull("file has a real syntax error after insertion: ${error?.errorDescription}", error)
    }
}
