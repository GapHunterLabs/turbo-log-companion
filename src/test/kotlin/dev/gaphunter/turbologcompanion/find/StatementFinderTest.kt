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
