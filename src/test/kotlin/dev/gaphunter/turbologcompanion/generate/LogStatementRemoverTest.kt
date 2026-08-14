package dev.gaphunter.turbologcompanion.generate

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.gaphunter.turbologcompanion.find.LogStatementFinder

/**
 * Real bug found via live testing 2026-08-14: with several inserted
 * statements, "Remove All Log Statements" only removed the first one
 * and threw a `PsiInvalidElementAccessException` -- even with just ONE
 * inserted statement, it still threw. Root cause and fix documented in
 * [LogStatementRemover].
 */
class LogStatementRemoverTest : BasePlatformTestCase() {

    fun testRemovesASingleInsertedStatementWithoutThrowing() {
        myFixture.configureByText(
            "Acme.java",
            """
            class Acme {
                void method() {
                    int total = 1 + 2;
                    System.out.println("TCLC Acme.method:3 ~ total = " + total);
                }
            }
            """.trimIndent(),
        )

        val elements = LogStatementFinder.findAllInFile(myFixture.file)
        assertEquals(1, elements.size)

        val removed = LogStatementRemover.removeAll(project, elements)

        assertEquals(1, removed)
        assertFalse(myFixture.file.text.contains("TCLC"))
        assertNoPsiErrors()
    }

    fun testRemovesAllThreeInsertedStatementsAcrossDifferentMethods() {
        myFixture.configureByText(
            "Acme.java",
            """
            class Acme {
                private int retryCount = 0;

                double calculateTotal(double subtotal, double taxRate) {
                    double total = subtotal * (1 + taxRate);
                    System.out.println("TCLC Acme.calculateTotal:4 ~ subtotal = " + subtotal);
                    return total;
                }

                static void processOrder(String orderId) {
                    String status = "PENDING";
                    System.out.println("TCLC Acme.processOrder:10 ~ status = " + status);
                    System.out.println("Processing " + orderId);
                }

                void retry() {
                    retryCount = retryCount + 1;
                    System.out.println("TCLC Acme.retry:16 ~ retryCount = " + retryCount);
                }
            }
            """.trimIndent(),
        )

        val elements = LogStatementFinder.findAllInFile(myFixture.file)
        assertEquals(3, elements.size)

        val removed = LogStatementRemover.removeAll(project, elements)

        assertEquals(3, removed)
        assertFalse("expected zero TCLC statements left, got:\n${myFixture.file.text}", myFixture.file.text.contains("TCLC"))
        assertEquals(0, LogStatementFinder.findAllInFile(myFixture.file).size)
        assertNoPsiErrors()
        // The real code around each removed statement must survive untouched.
        assertTrue(myFixture.file.text.contains("return total;"))
        assertTrue(myFixture.file.text.contains("System.out.println(\"Processing \" + orderId);"))
        assertTrue(myFixture.file.text.contains("retryCount = retryCount + 1;"))
    }

    private fun assertNoPsiErrors() {
        val error = PsiTreeUtil.findChildOfType(myFixture.file, com.intellij.psi.PsiErrorElement::class.java)
        assertNull("file has a real syntax error after removal: ${error?.errorDescription}", error)
    }
}
