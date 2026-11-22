package app.ondea.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvExportTest {
    @Test
    fun twoDecimalsIsTheSameOnEveryPlatform() {
        assertEquals("36.45", CsvExport.twoDecimals(36.449))
        assertEquals("36.50", CsvExport.twoDecimals(36.5))
        assertEquals("0.05", CsvExport.twoDecimals(0.05))
        assertEquals("-0.20", CsvExport.twoDecimals(-0.2))
    }
}
