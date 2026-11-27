package app.ondea.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CrashLogTest {
    @Test
    fun anonymizeKeepsTypesAndCodeLinesOnly() {
        val trace = """
            Ondea 1.3.0 · Google Pixel 8, Android 15 (API 35)

            java.lang.IllegalStateException: note "my private note" is too long
            	at app.ondea.ui.DayEditor.save(DayEditor.kt:42)
            	at app.ondea.ui.TodayScreen.onClick(TodayScreen.kt:10)
            Caused by: java.lang.NumberFormatException: For input string: "36,4x"
            	... 12 more
        """.trimIndent()
        val clean = CrashLog.anonymize(trace)
        assertEquals(
            """
            Ondea 1.3.0 · Google Pixel 8, Android 15 (API 35)
            java.lang.IllegalStateException
                at app.ondea.ui.DayEditor.save(DayEditor.kt:42)
                at app.ondea.ui.TodayScreen.onClick(TodayScreen.kt:10)
            Caused by: java.lang.NumberFormatException
                ... 12 more
            """.trimIndent(),
            clean,
        )
        assertFalse(clean.contains("private"))
    }

    @Test
    fun followUpCodesAreRandomHex() {
        val a = Feedback.newFollowUpCode()
        assertEquals(32, a.length)
        assert(a.all { it in "0123456789abcdef" })
        assert(a != Feedback.newFollowUpCode())
    }
}
