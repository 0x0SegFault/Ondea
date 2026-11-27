package app.ondea.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCryptoTest {
    private val sample = AppData(
        settings = Settings(onboarded = true, cycleLength = 30),
        cycles = listOf(Cycle(20000)),
        entries = listOf(DayEntry(20001, tempC = 36.45, note = "é \"quoted\"")),
    )

    @Test
    fun `round trip`() {
        val blob = BackupCrypto.encrypt(Repository.encode(sample), "correct horse".toCharArray(), iterations = 10_000)
        assertEquals(sample, Repository.decodeBackup(blob, "correct horse".toCharArray()))
    }

    @Test(expected = WrongPasswordException::class)
    fun `wrong password fails`() {
        val blob = BackupCrypto.encrypt(byteArrayOf(1, 2, 3), "a".toCharArray(), iterations = 10_000)
        BackupCrypto.decrypt(blob, "b".toCharArray())
    }

    @Test(expected = WrongPasswordException::class)
    fun `tampering is detected`() {
        val blob = BackupCrypto.encrypt(byteArrayOf(1, 2, 3), "a".toCharArray(), iterations = 10_000)
        blob[blob.size - 1] = (blob[blob.size - 1] + 1).toByte()
        BackupCrypto.decrypt(blob, "a".toCharArray())
    }

    @Test(expected = NotABackupException::class)
    fun `random file is rejected`() {
        BackupCrypto.decrypt("hello world, this is not a backup file at all".toByteArray(), "a".toCharArray())
    }

    @Test
    fun `unknown fields from newer versions are ignored`() {
        val json = """{"version":2,"cycles":[{"start":5,"future":true}],"newThing":1}"""
        assertArrayEquals(arrayOf(Cycle(5)), Repository.decode(json.toByteArray()).cycles.toTypedArray())
    }

    @Test
    fun `data saved by version 1_0_0 still loads`() {
        // Exact shape written by 1.0.0: no LH test, no ovulation-day choice.
        val v100 = """{"version":1,"settings":{"onboarded":true,"cycleLength":29,"periodLength":5,"ovulationDay":15,
            "fahrenheit":false,"appLock":false,"secureScreen":true},"cycles":[{"start":20400,"end":null}],
            "entries":[{"date":20401,"tempC":36.4,"flow":2,"mucus":0,"disturbed":false,"intercourse":false,"note":""}]}"""
        val d = Repository.decode(v100.toByteArray())
        assertEquals(15, d.settings.ovulationDay)
        assertEquals(0, d.entries.single().lh)
    }
}
