package joyin.takgi.paysage.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterDao {
    @Query("SELECT * FROM filter_rules WHERE isEnabled = 1")
    fun getAllEnabled(): Flow<List<FilterRule>>

    @Query("SELECT * FROM filter_rules")
    fun getAll(): Flow<List<FilterRule>>

    @Insert
    suspend fun insert(rule: FilterRule)

    @Update
    suspend fun update(rule: FilterRule)

    @Delete
    suspend fun delete(rule: FilterRule)

    @Query("SELECT * FROM filter_rules WHERE type = :type AND isEnabled = 1")
    suspend fun getByType(type: FilterType): List<FilterRule>
}
