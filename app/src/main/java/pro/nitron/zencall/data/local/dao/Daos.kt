package pro.nitron.zencall.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pro.nitron.zencall.data.local.entity.CallModeEntity
import pro.nitron.zencall.data.local.entity.CallRuleEntity
import pro.nitron.zencall.data.local.entity.CallerCategoryEntity
import pro.nitron.zencall.data.local.entity.CategoryNumberEntity
import pro.nitron.zencall.data.local.entity.ScreeningLogEntity

@Dao
interface ModeDao {
    @Query("SELECT * FROM call_modes ORDER BY createdAt ASC")
    fun getAllModesFlow(): Flow<List<CallModeEntity>>

    @Query("SELECT * FROM call_modes WHERE id = :id")
    suspend fun getModeById(id: String): CallModeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMode(mode: CallModeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertModes(modes: List<CallModeEntity>)

    @Delete
    suspend fun deleteMode(mode: CallModeEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM caller_categories ORDER BY isSystemDefault DESC, name ASC")
    fun getAllCategoriesFlow(): Flow<List<CallerCategoryEntity>>

    @Query("SELECT * FROM caller_categories")
    suspend fun getAllCategoriesSync(): List<CallerCategoryEntity>

    @Query("SELECT * FROM caller_categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CallerCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCategory(category: CallerCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CallerCategoryEntity>)

    @Delete
    suspend fun deleteCategory(category: CallerCategoryEntity)
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM call_rules ORDER BY priority DESC")
    fun getAllRulesFlow(): Flow<List<CallRuleEntity>>

    @Query("SELECT * FROM call_rules WHERE modeId = :modeId ORDER BY priority DESC")
    fun getRulesForModeFlow(modeId: String): Flow<List<CallRuleEntity>>

    @Query("SELECT * FROM call_rules WHERE modeId = :modeId ORDER BY priority DESC")
    suspend fun getRulesForModeSync(modeId: String): List<CallRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRule(rule: CallRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<CallRuleEntity>)

    @Query("DELETE FROM call_rules WHERE modeId = :modeId AND categoryId = :categoryId")
    suspend fun deleteRule(modeId: String, categoryId: String)
}

@Dao
interface NumberDao {
    @Query("SELECT * FROM category_numbers WHERE categoryId = :categoryId ORDER BY addedAt DESC")
    fun getNumbersForCategoryFlow(categoryId: String): Flow<List<CategoryNumberEntity>>

    @Query("SELECT * FROM category_numbers")
    suspend fun getAllNumbersSync(): List<CategoryNumberEntity>

    @Query("SELECT categoryId FROM category_numbers WHERE normalizedNumber = :normalizedNumber")
    suspend fun getCategoryIdsForNumber(normalizedNumber: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNumber(number: CategoryNumberEntity)

    @Query("DELETE FROM category_numbers WHERE categoryId = :categoryId AND normalizedNumber = :normalizedNumber")
    suspend fun removeNumber(categoryId: String, normalizedNumber: String)

    @Query("DELETE FROM category_numbers WHERE id = :id")
    suspend fun deleteNumberById(id: Long)
}

@Dao
interface ScreeningLogDao {
    @Query("SELECT * FROM screening_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsFlow(limit: Int = 100): Flow<List<ScreeningLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ScreeningLogEntity)

    @Query("DELETE FROM screening_logs")
    suspend fun clearLogs()

    @Query("DELETE FROM screening_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)
}
