package takagi.ru.paysage.data.dao

import androidx.room.*
import takagi.ru.paysage.data.model.ReaderConfig
import kotlinx.coroutines.flow.Flow

/**
 * 阅读器配置数据访问对象
 */
@Dao
interface ReaderConfigDao {
    
    /**
     * 保存或更新配置
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: ReaderConfig)
    
    /**
     * 获取默认配置
     */
    @Query("SELECT * FROM reader_configs WHERE id = 0")
    suspend fun getDefaultConfig(): ReaderConfig?
    
    /**
     * 获取默认配置(Flow)
     */
    @Query("SELECT * FROM reader_configs WHERE id = 0")
    fun getDefaultConfigFlow(): Flow<ReaderConfig?>
    
    /**
     * 获取指定配置
     */
    @Query("SELECT * FROM reader_configs WHERE id = :configId")
    suspend fun getConfig(configId: Int): ReaderConfig?
    
    /**
     * 获取所有配置
     */
    @Query("SELECT * FROM reader_configs ORDER BY id ASC")
    fun getAllConfigs(): Flow<List<ReaderConfig>>
    
    /**
     * 删除配置
     */
    @Query("DELETE FROM reader_configs WHERE id = :configId")
    suspend fun deleteConfig(configId: Int)
}
