package dev.gaphunter.turbologcompanion.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Exercises the REAL `update()` path through a constructed
 * [com.intellij.openapi.actionSystem.DataContext] carrying the exact
 * same [CommonDataKeys] the action itself reads -- this is the check
 * that would have caught Bean Copy Companion's real disabled-action
 * bug (a `DataKey` that compiled fine but was never populated by the
 * real Project View) WITHOUT needing a live `runIde` sandbox. This test
 * class exists specifically because of that incident.
 *
 * The actual PSI insertion is tested directly and synchronously in
 * `LogStatementInserterTest` -- racing `actionPerformed()`'s real
 * pooled-thread + `invokeLater` dispatch from a test proved flaky
 * (the test's own event-queue pump can run before the background
 * thread has even submitted its `invokeLater` callback), so this
 * class only covers what's genuinely synchronous: `update()`.
 */
class InsertLogStatementActionTest : BasePlatformTestCase() {

    private fun dataContextFromFixture() = SimpleDataContext.builder()
        .add(CommonDataKeys.PROJECT, project)
        .add(CommonDataKeys.EDITOR, myFixture.editor)
        .add(CommonDataKeys.PSI_FILE, myFixture.file)
        .build()

    fun testActionIsEnabledWhenCaretIsOnAVariableReference() {
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
        val action = InsertLogStatementAction()
        val event = TestActionEvent.createTestEvent(action, dataContextFromFixture())
        action.update(event)

        assertTrue("action should be enabled when caret is on a real variable reference", event.presentation.isEnabledAndVisible)
    }

    fun testActionIsDisabledWhenCaretIsNotOnAVariableReference() {
        myFixture.configureByText(
            "Acme2.java",
            """
            class Acme2 {
                void method() {
                    Sys<caret>tem.out.println("hi");
                }
            }
            """.trimIndent(),
        )
        val action = InsertLogStatementAction()
        val event = TestActionEvent.createTestEvent(action, dataContextFromFixture())
        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testActionIsDisabledWithNoEditorInTheDataContext() {
        val dataContext = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
        val action = InsertLogStatementAction()
        val event = TestActionEvent.createTestEvent(action, dataContext)
        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }
}
