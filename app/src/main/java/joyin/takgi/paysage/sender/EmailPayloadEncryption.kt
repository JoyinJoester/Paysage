package joyin.takgi.paysage.sender

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedEmailPayload(
    val version: String,
    val ivBase64: String,
    val ciphertextBase64: String
)

object EmailPayloadEncryption {
    const val FORMAT_VERSION = "PAYSAGE-AESGCM-V1"
    private const val KEY_BYTES = 32
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    fun generateKeyBase64(): String {
        val key = ByteArray(KEY_BYTES)
        random.nextBytes(key)
        return Base64.getEncoder().encodeToString(key)
    }

    fun normalizeKeyBase64(input: String): String? {
        val compact = input.filterNot { it.isWhitespace() }
        val key = runCatching { Base64.getDecoder().decode(compact) }.getOrNull() ?: return null
        return if (key.size == KEY_BYTES) {
            Base64.getEncoder().encodeToString(key)
        } else {
            null
        }
    }

    fun encrypt(plainText: String, keyBase64: String): EncryptedEmailPayload {
        val key = Base64.getDecoder().decode(keyBase64)
        require(key.size == KEY_BYTES) { "AES key must be 256-bit Base64." }

        val iv = ByteArray(IV_BYTES)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(TAG_BITS, iv)
        )
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedEmailPayload(
            version = FORMAT_VERSION,
            ivBase64 = Base64.getEncoder().encodeToString(iv),
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext)
        )
    }
}
