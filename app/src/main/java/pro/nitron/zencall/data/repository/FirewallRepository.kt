package pro.nitron.zencall.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pro.nitron.zencall.data.local.ZenCallDatabase
import pro.nitron.zencall.data.local.entity.CallModeEntity
import pro.nitron.zencall.data.local.entity.CallRuleEntity
import pro.nitron.zencall.data.local.entity.CallerCategoryEntity
import pro.nitron.zencall.data.local.entity.CategoryNumberEntity
import pro.nitron.zencall.data.prefs.FirewallPreferences
import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallMode
import pro.nitron.zencall.domain.model.CallRule
import pro.nitron.zencall.domain.model.CallerCategory
import pro.nitron.zencall.domain.util.CallNumberNormalizer
import java.util.UUID

interface FirewallRepository {
    // Mode operations
    fun getAllModes(): Flow<List<CallMode>>
    suspend fun getActiveMode(): CallMode?
    fun getActiveModeFlow(): Flow<CallMode?>
    suspend fun setActiveModeId(modeId: String)
    suspend fun createMode(name: String, defaultAction: CallAction): CallMode
    suspend fun updateMode(mode: CallMode)
    suspend fun deleteMode(modeId: String)

    // Category operations (Built-in + Custom user created)
    fun getAllCategories(): Flow<List<CallerCategory>>
    suspend fun createCustomCategory(name: String, description: String? = null, colorHex: String? = null): CallerCategory
    suspend fun deleteCategory(categoryId: String)

    // Rule operations
    fun getRulesForMode(modeId: String): Flow<List<CallRule>>
    suspend fun setRule(modeId: String, categoryId: String, action: CallAction, priority: Int)
    suspend fun removeRule(modeId: String, categoryId: String)

    // Number mapping operations
    fun getNumbersForCategory(categoryId: String): Flow<List<CategoryNumberEntity>>
    suspend fun addNumberToCategory(categoryId: String, rawNumber: String, displayName: String? = null)
    suspend fun removeNumberFromCategory(categoryId: String, normalizedNumber: String)

    // High performance screening lookup (<5ms budget)
    suspend fun getFastScreeningSnapshot(): ScreeningSnapshot
}

data class ScreeningSnapshot(
    val isFirewallEnabled: Boolean,
    val activeMode: CallMode?,
    val numbersToCategoriesMap: Map<String, Set<String>> // normalizedNumber -> set of categoryIds
)

class FirewallRepositoryImpl(
    private val context: Context,
    private val database: ZenCallDatabase = ZenCallDatabase.getInstance(context),
    private val preferences: FirewallPreferences = FirewallPreferences(context)
) : FirewallRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val normalizer = CallNumberNormalizer(defaultCountryIso = "IR", defaultCountryPrefix = "98")

    // In-memory hot cache for instant CallScreening evaluations (<1ms)
    private val _cachedScreeningSnapshot = MutableStateFlow(
        ScreeningSnapshot(
            isFirewallEnabled = true,
            activeMode = null,
            numbersToCategoriesMap = emptyMap()
        )
    )

    init {
        repositoryScope.launch {
            refreshHotCache()
        }
    }

    private suspend fun refreshHotCache() {
        val enabled = preferences.isFirewallEnabled.first()
        val activeModeId = preferences.getActiveModeId() ?: "mode_personal"
        val modeEntity = database.modeDao().getModeById(activeModeId)
            ?: database.modeDao().getAllModesFlow().first().firstOrNull()

        val activeMode = if (modeEntity != null) {
            val rules = database.ruleDao().getRulesForModeSync(modeEntity.id).map {
                CallRule(it.id, it.modeId, it.categoryId, it.action, it.priority)
            }
            CallMode(
                id = modeEntity.id,
                name = modeEntity.name,
                isEnabled = modeEntity.isEnabled && enabled,
                defaultAction = modeEntity.defaultAction,
                rules = rules
            )
        } else null

        // Cache all category numbers into an instant in-memory lookup map
        val allNumbers = database.numberDao().getAllNumbersSync()
        val mapping = mutableMapOf<String, MutableSet<String>>()
        for (item in allNumbers) {
            mapping.getOrPut(item.normalizedNumber) { mutableSetOf() }.add(item.categoryId)
        }

        _cachedScreeningSnapshot.value = ScreeningSnapshot(
            isFirewallEnabled = enabled,
            activeMode = activeMode,
            numbersToCategoriesMap = mapping
        )
    }

    override fun getAllModes(): Flow<List<CallMode>> {
        return database.modeDao().getAllModesFlow().map { modeList ->
            modeList.map { entity ->
                val rules = database.ruleDao().getRulesForModeSync(entity.id).map {
                    CallRule(it.id, it.modeId, it.categoryId, it.action, it.priority)
                }
                CallMode(
                    id = entity.id,
                    name = entity.name,
                    isEnabled = entity.isEnabled,
                    defaultAction = entity.defaultAction,
                    rules = rules
                )
            }
        }
    }

    override suspend fun getActiveMode(): CallMode? {
        val activeId = preferences.getActiveModeId() ?: "mode_personal"
        val entity = database.modeDao().getModeById(activeId) ?: return null
        val rules = database.ruleDao().getRulesForModeSync(entity.id).map {
            CallRule(it.id, it.modeId, it.categoryId, it.action, it.priority)
        }
        return CallMode(
            id = entity.id,
            name = entity.name,
            isEnabled = entity.isEnabled,
            defaultAction = entity.defaultAction,
            rules = rules
        )
    }

    override fun getActiveModeFlow(): Flow<CallMode?> {
        return preferences.activeModeIdFlow.map { activeId ->
            val targetId = activeId ?: "mode_personal"
            val entity = database.modeDao().getModeById(targetId) ?: return@map null
            val rules = database.ruleDao().getRulesForModeSync(entity.id).map {
                CallRule(it.id, it.modeId, it.categoryId, it.action, it.priority)
            }
            CallMode(
                id = entity.id,
                name = entity.name,
                isEnabled = entity.isEnabled,
                defaultAction = entity.defaultAction,
                rules = rules
            )
        }
    }

    override suspend fun setActiveModeId(modeId: String) {
        preferences.setActiveModeId(modeId)
        refreshHotCache()
    }

    override suspend fun createMode(name: String, defaultAction: CallAction): CallMode {
        val id = "mode_" + UUID.randomUUID().toString().take(8)
        val entity = CallModeEntity(
            id = id,
            name = name,
            isEnabled = true,
            defaultAction = defaultAction
        )
        database.modeDao().insertOrUpdateMode(entity)
        refreshHotCache()
        return CallMode(id = id, name = name, defaultAction = defaultAction)
    }

    override suspend fun updateMode(mode: CallMode) {
        val entity = CallModeEntity(
            id = mode.id,
            name = mode.name,
            isEnabled = mode.isEnabled,
            defaultAction = mode.defaultAction,
            updatedAt = System.currentTimeMillis()
        )
        database.modeDao().insertOrUpdateMode(entity)
        refreshHotCache()
    }

    override suspend fun deleteMode(modeId: String) {
        val entity = database.modeDao().getModeById(modeId) ?: return
        database.modeDao().deleteMode(entity)
        refreshHotCache()
    }

    override fun getAllCategories(): Flow<List<CallerCategory>> {
        return database.categoryDao().getAllCategoriesFlow().map { entities ->
            entities.map {
                CallerCategory(
                    id = it.id,
                    name = it.name,
                    isSystemDefault = it.isSystemDefault,
                    description = it.description,
                    colorHex = it.colorHex,
                    createdAt = it.createdAt
                )
            }
        }
    }

    override suspend fun createCustomCategory(name: String, description: String?, colorHex: String?): CallerCategory {
        val id = "cat_" + UUID.randomUUID().toString().take(8)
        val entity = CallerCategoryEntity(
            id = id,
            name = name,
            isSystemDefault = false,
            description = description,
            colorHex = colorHex
        )
        database.categoryDao().insertOrUpdateCategory(entity)
        refreshHotCache()
        return CallerCategory(
            id = id,
            name = name,
            isSystemDefault = false,
            description = description,
            colorHex = colorHex
        )
    }

    override suspend fun deleteCategory(categoryId: String) {
        val entity = database.categoryDao().getCategoryById(categoryId) ?: return
        if (entity.isSystemDefault) {
            // System categories cannot be deleted
            return
        }
        database.categoryDao().deleteCategory(entity)
        refreshHotCache()
    }

    override fun getRulesForMode(modeId: String): Flow<List<CallRule>> {
        return database.ruleDao().getRulesForModeFlow(modeId).map { list ->
            list.map { CallRule(it.id, it.modeId, it.categoryId, it.action, it.priority) }
        }
    }

    override suspend fun setRule(modeId: String, categoryId: String, action: CallAction, priority: Int) {
        val id = "rule_${modeId}_$categoryId"
        val entity = CallRuleEntity(
            id = id,
            modeId = modeId,
            categoryId = categoryId,
            action = action,
            priority = priority
        )
        database.ruleDao().insertOrUpdateRule(entity)
        refreshHotCache()
    }

    override suspend fun removeRule(modeId: String, categoryId: String) {
        database.ruleDao().deleteRule(modeId, categoryId)
        refreshHotCache()
    }

    override fun getNumbersForCategory(categoryId: String): Flow<List<CategoryNumberEntity>> {
        return database.numberDao().getNumbersForCategoryFlow(categoryId)
    }

    override suspend fun addNumberToCategory(categoryId: String, rawNumber: String, displayName: String?) {
        val normalized = normalizer.normalize(rawNumber) ?: return
        val entity = CategoryNumberEntity(
            categoryId = categoryId,
            rawNumber = rawNumber,
            normalizedNumber = normalized,
            displayName = displayName
        )
        database.numberDao().insertNumber(entity)
        refreshHotCache()
    }

    override suspend fun removeNumberFromCategory(categoryId: String, normalizedNumber: String) {
        val norm = normalizer.normalize(normalizedNumber) ?: normalizedNumber
        database.numberDao().removeNumber(categoryId, norm)
        refreshHotCache()
    }

    override suspend fun getFastScreeningSnapshot(): ScreeningSnapshot {
        return _cachedScreeningSnapshot.value
    }
}
