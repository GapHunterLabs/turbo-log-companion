package dev.gaphunter.turbologcompanion.find

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class LogStatementFinderTest : BasePlatformTestCase() {

    fun testFindsAPreviouslyInsertedJavaLogStatement() {
        myFixture.configureByText(
            "Acme.java",
            """
            class Acme {
                void method() {
                    int total = 1;
                    System.out.println("TCLC Acme.method:3 ~ total = " + total);
                    doSomethingElse();
                }
            }
            """.trimIndent(),
        )

        val found = LogStatementFinder.findAllInFile(myFixture.file)

        assertEquals(1, found.size)
        assertTrue(found.first().text.contains("TCLC"))
    }

    fun testDoesNotMatchAnUnrelatedPrintlnCall() {
        myFixture.configureByText(
            "Acme2.java",
            """
            class Acme2 {
                void method() {
                    System.out.println("just a regular log line");
                }
            }
            """.trimIndent(),
        )

        assertTrue(LogStatementFinder.findAllInFile(myFixture.file).isEmpty())
    }

    fun testFindsMultipleInsertedStatementsInKotlin() {
        myFixture.configureByText(
            "Acme.kt",
            """
            class Acme {
                fun method() {
                    val a = 1
                    println("TCLC Acme.method:3 ~ a = ${'$'}a")
                    val b = 2
                    println("TCLC Acme.method:5 ~ b = ${'$'}b")
                }
            }
            """.trimIndent(),
        )

        assertEquals(2, LogStatementFinder.findAllInFile(myFixture.file).size)
    }
}
