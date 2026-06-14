package joyin.takgi.paysage.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardLogDao {
    @Insert
    suspend fun insert(log: ForwardLog)

    @Query("SELECT * FROM forward_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecent(): Flow<List<ForwardLog>>

    @Query("SELECT * FROM forward_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ForwardLog>>

    @Query("SELECT COUNT(*) FROM forward_logs WHERE DATE(timestamp/1000, 'unixepoch') = DATE('now')")
    fun getTodayCount(): Flow<Int>

    @Query("SELECT MAX(timestamp) FROM forward_logs WHERE filtered = 0 AND (emailSuccess = 1 OR telegramSuccess = 1)")
    fun getLastSuccessTimestamp(): Flow<Long?>

    @Query("DELETE FROM forward_logs WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}
