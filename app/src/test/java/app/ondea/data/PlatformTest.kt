package app.ondea.data

import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformTest {
    // Screens keep dates in rememberSaveable, which needs java.io.Serializable on Android.
    @Test
    fun `shared LocalDate survives screen rotation`() {
        assertTrue(java.io.Serializable::class.java.isAssignableFrom(kotlinx.datetime.LocalDate::class.java))
    }
}
