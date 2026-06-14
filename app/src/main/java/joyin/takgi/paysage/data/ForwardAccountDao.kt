package joyin.takgi.paysage.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardAccountDao {
    @Insert
    suspend fun insert(account: ForwardAccount)

    @Update
    suspend fun update(account: ForwardAccount)

    @Delete
    suspend fun delete(account: ForwardAccount)

    @Query("SELECT * FROM forward_accounts WHERE isEnabled = 1")
    fun getEnabled(): Flow<List<ForwardAccount>>

    @Query("SELECT * FROM forward_accounts")
    fun getAll(): Flow<List<ForwardAccount>>
}
