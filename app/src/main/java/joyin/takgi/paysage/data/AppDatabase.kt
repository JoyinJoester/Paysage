package joyin.takgi.paysage.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FilterRule::class,
        ForwardLog::class,
        ForwardAccount::class,
        PendingForwardMessage::class,
        MailTrustedSenderEntity::class,
        MailCommandNonceEntity::class,
        MailCommandRecordEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun filterDao(): FilterDao
    abstract fun forwardLogDao(): ForwardLogDao
    abstract fun forwardAccountDao(): ForwardAccountDao
    abstract fun pendingForwardDao(): PendingForwardDao
    abstract fun mailTrustedSenderDao(): MailTrustedSenderDao
    abstract fun mailCommandNonceDao(): MailCommandNonceDao
    abstract fun mailCommandRecordDao(): MailCommandRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paysage_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_forward_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sender TEXT NOT NULL,
                        content TEXT NOT NULL,
                        smsTimestamp INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        attempts INTEGER NOT NULL,
                        lastError TEXT NOT NULL,
                        nextAttemptAt INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mail_trusted_senders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        email TEXT NOT NULL,
                        allowedActions TEXT NOT NULL,
                        enabled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_mail_trusted_senders_email
                    ON mail_trusted_senders(email)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mail_command_nonces (
                        nonceKey TEXT PRIMARY KEY NOT NULL,
                        sender TEXT NOT NULL,
                        nonce TEXT NOT NULL,
                        action TEXT NOT NULL,
                        usedAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mail_command_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        messageKey TEXT NOT NULL,
                        messageNumber INTEGER NOT NULL,
                        sender TEXT NOT NULL,
                        normalizedSender TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        receivedAtMillis INTEGER NOT NULL,
                        action TEXT NOT NULL,
                        decisionCode TEXT NOT NULL,
                        allowed INTEGER NOT NULL,
                        executed INTEGER NOT NULL,
                        resultMessage TEXT NOT NULL,
                        processedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_mail_command_records_messageKey
                    ON mail_command_records(messageKey)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_mail_command_records_processedAtMillis
                    ON mail_command_records(processedAtMillis)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val nonceUpdates = mutableListOf<Array<String>>()
                db.query("SELECT nonceKey, sender, nonce FROM mail_command_nonces").use { cursor ->
                    val nonceKeyIndex = cursor.getColumnIndexOrThrow("nonceKey")
                    val senderIndex = cursor.getColumnIndexOrThrow("sender")
                    val nonceIndex = cursor.getColumnIndexOrThrow("nonce")
                    while (cursor.moveToNext()) {
                        val oldNonceKey = cursor.getString(nonceKeyIndex)
                        val oldSender = cursor.getString(senderIndex)
                        val oldNonce = cursor.getString(nonceIndex)
                        val newNonceKey = MailDatabasePrivacyMigration.migrateNonceKey(
                            oldNonceKey = oldNonceKey,
                            sender = oldSender,
                            nonce = oldNonce
                        )
                        val newSender = MailDatabasePrivacyMigration.migrateNonceSender(oldSender)
                        val newNonce = MailDatabasePrivacyMigration.migrateNonceValue(oldNonce)
                        nonceUpdates += arrayOf(newNonceKey, newSender, newNonce, oldNonceKey)
                    }
                }
                nonceUpdates.forEach { update ->
                    db.execSQL(
                        "DELETE FROM mail_command_nonces WHERE nonceKey = ? AND nonceKey <> ?",
                        arrayOf(update[0], update[3])
                    )
                    db.execSQL(
                        """
                        UPDATE mail_command_nonces
                        SET nonceKey = ?, sender = ?, nonce = ?
                        WHERE nonceKey = ?
                        """.trimIndent(),
                        update
                    )
                }

                val recordUpdates = mutableListOf<Array<Any>>()
                db.query("SELECT id, messageKey, normalizedSender FROM mail_command_records").use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow("id")
                    val messageKeyIndex = cursor.getColumnIndexOrThrow("messageKey")
                    val senderIndex = cursor.getColumnIndexOrThrow("normalizedSender")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        val oldMessageKey = cursor.getString(messageKeyIndex)
                        val oldSender = cursor.getString(senderIndex)
                        val newMessageKey = MailDatabasePrivacyMigration.migrateRecordMessageKey(oldMessageKey)
                        val newSender = MailDatabasePrivacyMigration.migrateRecordSender(oldSender)
                        recordUpdates += arrayOf(newMessageKey, newSender, id)
                    }
                }
                recordUpdates.forEach { update ->
                    db.execSQL(
                        """
                        UPDATE mail_command_records
                        SET messageKey = ?, normalizedSender = ?
                        WHERE id = ?
                        """.trimIndent(),
                        update
                    )
                }
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE forward_accounts ADD COLUMN smtpProvider TEXT NOT NULL DEFAULT 'CUSTOM'")
                db.execSQL("ALTER TABLE forward_accounts ADD COLUMN smtpAuthType TEXT NOT NULL DEFAULT 'PASSWORD'")
                db.execSQL("ALTER TABLE forward_accounts ADD COLUMN smtpCredentialRef TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE forward_accounts ADD COLUMN emailEncryptionEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE forward_accounts ADD COLUMN emailEncryptionKeyRef TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
