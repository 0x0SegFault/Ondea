package app.ondea.data

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val GCM_TAG_BITS = 128
private const val GCM_IV_BYTES = 12

/**
 * AES-256-GCM with a non-exportable key held by the Android Keystore
 * (hardware-backed / StrongBox when the device has it).
 */
class KeystoreCipher(private val alias: String = "ondea_data_key") {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val sealed = cipher.doFinal(plain)
        return ByteBuffer.allocate(1 + iv.size + sealed.size)
            .put(FORMAT_VERSION).put(iv).put(sealed).array()
    }

    fun decrypt(blob: ByteArray): ByteArray {
        if (blob.size < 1 + GCM_IV_BYTES || blob[0] != FORMAT_VERSION) {
            throw GeneralSecurityException("Unknown data format")
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = blob.copyOfRange(1, 1 + GCM_IV_BYTES)
        cipher.init(Cipher.DECRYPT_MODE, existingKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(blob, 1 + GCM_IV_BYTES, blob.size - 1 - GCM_IV_BYTES)
    }

    fun deleteKey() {
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    private fun existingKey(): SecretKey =
        keyStore.getKey(alias, null) as? SecretKey
            ?: throw GeneralSecurityException("Encryption key is missing")

    private fun key(): SecretKey = (keyStore.getKey(alias, null) as? SecretKey) ?: generate()

    private fun generate(): SecretKey {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generate(strongBox = true)
            } catch (_: Exception) {
                // No StrongBox on this device; fall back to the TEE-backed keystore.
            }
        }
        return generate(strongBox = false)
    }

    private fun generate(strongBox: Boolean): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .apply { if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setIsStrongBoxBacked(true) }
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val FORMAT_VERSION: Byte = 1
    }
}

class WrongPasswordException : Exception()
class NotABackupException : Exception()

/**
 * Password-protected backup files, portable between devices:
 * MAGIC | iterations (int) | salt (16) | iv (12) | AES-256-GCM(ciphertext + tag).
 * The key is derived with PBKDF2-HMAC-SHA256.
 */
object BackupCrypto {
    private val MAGIC = "ONDEA-BACKUP-1\n".toByteArray(Charsets.US_ASCII)
    private const val SALT_BYTES = 16
    const val DEFAULT_ITERATIONS = 600_000

    fun encrypt(plain: ByteArray, password: CharArray, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(GCM_IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derive(password, salt, iterations), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(MAGIC)
        val sealed = cipher.doFinal(plain)
        return ByteBuffer.allocate(MAGIC.size + 4 + SALT_BYTES + GCM_IV_BYTES + sealed.size)
            .put(MAGIC).putInt(iterations).put(salt).put(iv).put(sealed).array()
    }

    fun decrypt(blob: ByteArray, password: CharArray): ByteArray {
        val header = MAGIC.size + 4 + SALT_BYTES + GCM_IV_BYTES
        if (blob.size <= header || !blob.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw NotABackupException()
        }
        val buf = ByteBuffer.wrap(blob, MAGIC.size, blob.size - MAGIC.size)
        val iterations = buf.int
        if (iterations !in 10_000..10_000_000) throw NotABackupException()
        val salt = ByteArray(SALT_BYTES).also { buf.get(it) }
        val iv = ByteArray(GCM_IV_BYTES).also { buf.get(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(password, salt, iterations), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(MAGIC)
        return try {
            cipher.doFinal(blob, header, blob.size - header)
        } catch (_: GeneralSecurityException) {
            throw WrongPasswordException()
        }
    }

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(raw, "AES").also { raw.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }
}
