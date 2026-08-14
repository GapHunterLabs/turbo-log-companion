package dev.gaphunter.turbologcompanion.find

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class StatementFinderTest : BasePlatformTestCase() {

    fun testFindsAnchorForALocalVariableInJava() {
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

        val target = StatementFinder.find(element)

        assertNotNull(target)
        assertEquals("total", target!!.context.variableName)
        assertEquals("Acme", target.context.className)
        assertEquals("method", target.context.methodName)
        assertTrue(target.anchorStatement.text.contains("System.out.println"))
    }

    fun testFindsAnchorForAFieldInAStaticJavaMethod() {
        myFixture.configureByText(
            "Acme2.java",
            """
            class Acme2 {
                static int counter = 0;
                static void bump() {
                    coun<caret>ter = counter + 1;
                }
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!

        val target = StatementFinder.find(element)

        assertNotNull(target)
        assertEquals("counter", target!!.context.variableName)
        assertEquals("bump", target.context.methodName)
    }

    fun testFindsAnchorForALocalVariableInKotlin() {
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

        val target = StatementFinder.find(element)

        assertNotNull(target)
        assertEquals("total", target!!.context.variableName)
        assertEquals("Acme", target.context.className)
        assertEquals("method", target.context.methodName)
    }

    fun testFindsAnchorForATopLevelKotlinFunctionParameter() {
        myFixture.configureByText(
            "TopLevel.kt",
            """
            fun greet(name: String) {
                println("hi ${'$'}na<caret>me")
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!

        val target = StatementFinder.find(element)

        assertNotNull(target)
        assertEquals("name", target!!.context.variableName)
        assertEquals("TopLevel", target.context.className)
        assertEquals("greet", target.context.methodName)
    }

    fun testFindsAnchorOnTheDeclarationItselfWhenTheVariableIsNeverReadAfterwards() {
        // Real gap found via live testing 2026-08-14: a variable whose
        // ONLY appearance in the file is its own declaration (never
        // read again) has no PsiReferenceExpression at all -- the
        // "log right where I just declared it" workflow.
        myFixture.configureByText(
            "Acme4.java",
            """
            class Acme4 {
                static void processOrder(String orderId, int quantity) {
                    String sta<caret>tus = "PENDING";
                    System.out.println("Processing " + orderId);
                }
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!

        val target = StatementFinder.find(element)

        assertNotNull(target)
        assertEquals("status", target!!.context.variableName)
        assertEquals("processOrder", target.context.methodName)
        assertTrue(target.anchorStatement.text.contains("\"PENDING\""))
    }

    fun testFindsAnchorOnTheDeclarationItselfInKotlinWhenNeverReadAfterwards() {
        myFixture.configureByText(
            "Acme4.kt",
            """
            class Acme4 {
                fun processOrder(orderId: String) {
                    val sta<caret>tus = "PENDING"
                    println("Processing " + orderId)
                }
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!

        val target = StatementFinder.find(element)

        assertNotNull(target)
        assertEquals("status", target!!.context.variableName)
        assertEquals("processOrder", target.context.methodName)
    }

    fun testReturnsNullForAFieldDeclarationNameItself() {
        // A field has no enclosing block to insert a log statement into
        // -- correctly stays unsupported directly on the declaration
        // (still loggable from inside a method that references it, see
        // testFindsAnchorForAFieldInAStaticJavaMethod above).
        myFixture.configureByText(
            "Acme5.java",
            """
            class Acme5 {
                static int coun<caret>ter = 0;
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!

        assertNull(StatementFinder.find(element))
    }

    fun testReturnsNullForAClassLevelKotlinPropertyDeclarationName() {
        myFixture.configureByText(
            "Acme5.kt",
            """
            class Acme5 {
                val coun<caret>ter = 0
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!

        assertNull(StatementFinder.find(element))
    }

    fun testReturnsNullWhenCaretIsNotOnAVariableReference() {
        myFixture.configureByText(
            "Acme3.java",
            """
            class Acme3 {
                void method() {
                    Sys<caret>tem.out.println("hello");
                }
            }
            """.trimIndent(),
        )
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!

        assertNull(StatementFinder.find(element))
    }
}
