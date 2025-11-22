package takagi.ru.paysage.data.dao

import androidx.room.*
import takagi.ru.paysage.data.model.ReplaceRule
import kotlinx.coroutines.flow.Flow

/**
 * 替换规则数据访问对象
 */
@Dao
interface ReplaceRuleDao {
    
    /**
     * 插入替换规则
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: ReplaceRule)
    
    /**
     * 批量插入替换规则
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<ReplaceRule>)
    
    /**
     * 更新替换规则
     */
    @Update
    suspend fun update(rule: ReplaceRule)
    
    /**
     * 删除替换规则
     */
    @Delete
    suspend fun delete(rule: ReplaceRule)
    
    /**
     * 根据ID删除替换规则
     */
    @Query("DELETE FROM replace_rules WHERE id = :ruleId")
    suspend fun deleteById(ruleId: String)
    
    /**
     * 获取所有替换规则
     */
    @Query("SELECT * FROM replace_rules ORDER BY `order` ASC, name ASC")
    fun getAllRules(): Flow<List<ReplaceRule>>
    
    /**
     * 获取所有启用的替换规则
     */
    @Query("SELECT * FROM replace_rules WHERE isEnabled = 1 ORDER BY `order` ASC")
    fun getEnabledRules(): Flow<List<ReplaceRule>>
    
    /**
     * 获取指定规则
     */
    @Query("SELECT * FROM replace_rules WHERE id = :ruleId")
    suspend fun getRuleById(ruleId: String): ReplaceRule?
    
    /**
     * 启用/禁用规则
     */
    @Query("UPDATE replace_rules SET isEnabled = :enabled WHERE id = :ruleId")
    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean)
    
    /**
     * 更新规则顺序
     */
    @Query("UPDATE replace_rules SET `order` = :order WHERE id = :ruleId")
    suspend fun updateRuleOrder(ruleId: String, order: Int)
    
    /**
     * 删除所有规则
     */
    @Query("DELETE FROM replace_rules")
    suspend fun deleteAll()
}
