package joyin.takgi.paysage.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingForwardDao {
    @Insert
    suspend fun insert(message: PendingForwardMessage): Long

    @Update
    suspend fun update(message: PendingForwardMessage)

    @Query("DELETE FROM pending_forward_messages WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query(
        """
        SELECT * FROM pending_forward_messages
        WHERE nextAttemptAt <= :now
        ORDER BY createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun dueMessages(now: Long, limit: Int): List<PendingForwardMessage>

    @Query(
        """
        SELECT * FROM pending_forward_messages
        WHERE sender = :sender AND content = :content AND smsTimestamp = :timestamp
        LIMIT 1
        """
    )
    suspend fun findExact(sender: String, content: String, timestamp: Long): PendingForwardMessage?

    @Query("SELECT COUNT(*) FROM pending_forward_messages")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_forward_messages")
    suspend fun pendingCount(): Int

    @Query("SELECT * FROM pending_forward_messages ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PendingForwardMessage>>
}
