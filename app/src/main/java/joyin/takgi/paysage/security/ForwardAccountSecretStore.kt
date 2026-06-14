package joyin.takgi.paysage.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import joyin.takgi.paysage.data.AccountType
import joyin.takgi.paysage.data.ForwardAccount
import java.util.UUID

class ForwardAccountSecretStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun newCredentialRef(): String = "smtp_${UUID.randomUUID()}"

    fun newEncryptionKeyRef(): String = "aes_${UUID.randomUUID()}"

    fun accountCredentialRef(account: ForwardAccount): String =
        account.smtpCredentialRef.ifBlank {
            if (account.id > 0) "smtp_account_${account.id}" else newCredentialRef()
        }

    fun accountEncryptionKeyRef(account: ForwardAccount): String =
        account.emailEncryptionKeyRef.ifBlank {
            if (account.id > 0) "aes_account_${account.id}" else newEncryptionKeyRef()
        }

    fun readCredential(ref: String): String =
        preferences.getString(KEY_CREDENTIAL_PREFIX + ref, "").orEmpty()

    fun writeCredential(ref: String, secret: String) {
        preferences.edit()
            .putString(KEY_CREDENTIAL_PREFIX + ref, secret)
            .apply()
    }

    fun clearCredential(ref: String) {
        preferences.edit()
            .remove(KEY_CREDENTIAL_PREFIX + ref)
            .apply()
    }

    fun readEncryptionKey(ref: String): String =
        preferences.getString(KEY_ENCRYPTION_PREFIX + ref, "").orEmpty()

    fun writeEncryptionKey(ref: String, keyBase64: String) {
        preferences.edit()
            .putString(KEY_ENCRYPTION_PREFIX + ref, keyBase64)
            .apply()
    }

    fun clearEncryptionKey(ref: String) {
        preferences.edit()
            .remove(KEY_ENCRYPTION_PREFIX + ref)
            .apply()
    }

    fun clearAccount(account: ForwardAccount) {
        account.smtpCredentialRef.takeIf { it.isNotBlank() }?.let(::clearCredential)
        account.emailEncryptionKeyRef.takeIf { it.isNotBlank() }?.let(::clearEncryptionKey)
    }

    fun migratePlaintextCredential(account: ForwardAccount): ForwardAccount {
        if (account.type != AccountType.EMAIL || account.smtpPassword.isBlank()) return account
        val ref = accountCredentialRef(account)
        writeCredential(ref, account.smtpPassword)
        return account.copy(
            smtpCredentialRef = ref,
            smtpPassword = ""
        )
    }

    companion object {
        private const val PREFS_NAME = "paysage_forward_account_secrets"
        private const val KEY_CREDENTIAL_PREFIX = "credential_"
        private const val KEY_ENCRYPTION_PREFIX = "encryption_"
    }
}
