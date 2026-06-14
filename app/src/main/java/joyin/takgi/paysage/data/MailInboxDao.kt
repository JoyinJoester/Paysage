package joyin.takgi.paysage.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MailTrustedSenderDao {
    @Insert
    suspend fun insert(sender: MailTrustedSenderEntity): Long

    @Update
    suspend fun update(sender: MailTrustedSenderEntity)

    @Delete
    suspend fun delete(sender: MailTrustedSenderEntity)

    @Query("SELECT * FROM mail_trusted_senders ORDER BY enabled DESC, email ASC")
    fun observeAll(): Flow<List<MailTrustedSenderEntity>>

    @Query("SELECT * FROM mail_trusted_senders ORDER BY enabled DESC, email ASC")
    suspend fun getAllOnce(): List<MailTrustedSenderEntity>

    @Query("SELECT * FROM mail_trusted_senders WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): MailTrustedSenderEntity?
}

@Dao
interface MailCommandNonceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(nonce: MailCommandNonceEntity)

    @Query("SELECT nonceKey FROM mail_command_nonces")
    suspend fun usedNonceKeys(): List<String>

    @Query("DELETE FROM mail_command_nonces WHERE expiresAt < :cutoffMillis")
    suspend fun deleteExpired(cutoffMillis: Long)
}

@Dao
interface MailCommandRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: MailCommandRecordEntity)

    @Query("SELECT * FROM mail_command_records ORDER BY processedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MailCommandRecordEntity>>

    @Query("SELECT * FROM mail_command_records ORDER BY processedAtMillis DESC LIMIT :limit")
    suspend fun getRecentOnce(limit: Int): List<MailCommandRecordEntity>

    @Query("SELECT messageKey FROM mail_command_records WHERE messageKey IN (:messageKeys)")
    suspend fun findExistingMessageKeys(messageKeys: List<String>): List<String>

    @Query("DELETE FROM mail_command_records WHERE processedAtMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}
