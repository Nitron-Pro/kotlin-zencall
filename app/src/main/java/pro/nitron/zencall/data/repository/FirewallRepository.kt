package pro.nitron.zencall.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pro.nitron.zencall.data.local.ZenCallDatabase
import pro.nitron.zencall.data.local.entity.CallModeEntity
import pro.nitron.zencall.data.local.entity.CallRuleEntity
import pro.nitron.zencall.data.local.entity.CallerCategoryEntity
import pro.nitron.zencall.data.local.entity.CategoryNumberEntity
import pro.nitron.zencall.data.prefs.FirewallPreferences
import pro.nitron.zencall.domain.engine.ModeResolver
import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallMode
import pro.nitron.zencall.domain.model.CallRule
import pro.nitron.zencall.domain.model.CallerCategory
import pro.nitron.zencall.domain.model.ModeControl
import pro.nitron.zencall.domain.model.WeeklySchedule
import pro.nitron.zencall.domain.util.CallNumberNormalizer
import java.util.UUID

interface FirewallRepository {
    fun getAllModes(): Flow<List<CallMode>>
    suspend fun getActiveMode(): CallMode?
    fun getActiveModeFlow(): Flow<CallMode?>
    suspend fun setActiveModeId(modeId: String)
    suspend fun setModeControl(control: ModeControl)
    suspend fun createMode(name: String, defaultAction: CallAction = CallAction.REJECT): CallMode
    suspend fun updateMode(mode: CallMode)
    suspend fun deleteMode(modeId: String)

    fun getAllCategories(): Flow<List<CallerCategory>>
    suspend fun createCustomCategory(name: String, description: String? = null, colorHex: String? = null): CallerCategory
    suspend fun deleteCategory(categoryId: String)

    fun getRulesForMode(modeId: String): Flow<List<CallRule>>
    suspend fun setRule(
        modeId: String,
        categoryId: String,
        action: CallAction,
        priority: Int = 0,
        schedule: WeeklySchedule = WeeklySchedule(isEnabled = false)
    )
    suspend fun setCategoryAction(modeId: String, categoryId: String, action: CallAction?)
    suspend fun setCategoryWhitelisted(modeId: String, categoryId: String, whitelisted: Boolean)
    suspend fun removeRule(modeId: String, categoryId: String)

    fun getNumbersForCategory(categoryId: String): Flow<List<CategoryNumberEntity>>
    suspend fun addNumberToCategory(categoryId: String, rawNumber: String, displayName: String? = null)
    suspend fun removeNumberFromCategory(categoryId: String, normalizedNumber: String)
    suspend fun deleteNumberById(id: Long)

    fun getScreeningLogs(limit: Int = 100): Flow<List<pro.nitron.zencall.data.local.entity.ScreeningLogEntity>>
    suspend fun recordScreeningLog(
        rawNumber: String?,
        normalizedNumber: String?,
        callerName: String?,
        action: CallAction,
        reason: String,
        modeName: String?,
        latencyMs: Long
    )
    suspend fun clearLogs()
    suspend fun deleteLogById(id: Long)

    suspend fun getFastScreeningSnapshot(): ScreeningSnapshot
}

data class ScreeningSnapshot(
    val isFirewallEnabled: Boolean,
    val activeMode: CallMode?,
    val allModes: List<CallMode> = emptyList(),
    val modeControl: ModeControl = ModeControl.AUTO,
    val manualModeId: String? = null,
    val numbersToCategoriesMap: Map<String, Set<String>>
)

class FirewallRepositoryImpl(
    private val context: Context,
    private val database: ZenCallDatabase = ZenCallDatabase.getInstance(context),
    private val preferences: FirewallPreferences = FirewallPreferences(context)
) : FirewallRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val normalizer = CallNumberNormalizer(defaultCountryIso = "IR", defaultCountryPrefix = "98")

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
        repositoryScope.launch {
            preferences.isFirewallEnabled.collect {
                refreshHotCache()
            }
        }
    }

    private fun parseDays(csv: String): Set<Int> {
        return csv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
            .ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
    }

    private fun mapRuleEntityToDomain(entity: CallRuleEntity): CallRule {
        val schedule = WeeklySchedule(
            isEnabled = entity.isScheduleEnabled,
            daysOfWeek = parseDays(entity.scheduleDaysCsv),
            startMinuteOfDay = entity.startMinuteOfDay,
            endMinuteOfDay = entity.endMinuteOfDay,
            outsideScheduleAction = entity.outsideScheduleAction
        )
        return CallRule(
            id = entity.id,
            modeId = entity.modeId,
            categoryId = entity.categoryId,
            action = entity.action,
            priority = entity.priority,
            schedule = schedule
        )
    }

    private fun mapModeEntity(entity: CallModeEntity, rules: List<CallRule>): CallMode {
        return CallMode(
            id = entity.id,
            name = entity.name,
            isEnabled = entity.isEnabled,
            isSystemDefault = entity.isSystemDefault,
            defaultAction = entity.defaultAction,
            schedulePriority = entity.schedulePriority,
            schedule = WeeklySchedule(
                isEnabled = entity.isScheduleEnabled,
                daysOfWeek = parseDays(entity.scheduleDaysCsv),
                startMinuteOfDay = entity.startMinuteOfDay,
                endMinuteOfDay = entity.endMinuteOfDay
            ),
            rules = rules,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun toEntity(mode: CallMode): CallModeEntity {
        return CallModeEntity(
            id = mode.id,
            name = mode.name,
            isEnabled = mode.isEnabled,
            isSystemDefault = mode.isSystemDefault,
            defaultAction = mode.defaultAction,
            schedulePriority = mode.schedulePriority,
            isScheduleEnabled = mode.schedule.isEnabled,
            scheduleDaysCsv = mode.schedule.daysOfWeek.joinToString(","),
            startMinuteOfDay = mode.schedule.startMinuteOfDay,
            endMinuteOfDay = mode.schedule.endMinuteOfDay,
            createdAt = mode.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun loadAllModes(): List<CallMode> {
        val entities = database.modeDao().getAllModesFlow().first()
        val allRules = database.ruleDao().getAllRulesFlow().first()
        val rulesByMode = allRules.groupBy { it.modeId }
        return entities.map { entity ->
            val rules = (rulesByMode[entity.id] ?: emptyList()).map { mapRuleEntityToDomain(it) }
            mapModeEntity(entity, rules)
        }
    }

    private suspend fun refreshHotCache() {
        ZenCallDatabase.seedDefaultDataIfEmpty(database)
        val enabled = preferences.isFirewallEnabled.first()
        val control = preferences.getModeControl()
        val manualId = preferences.getActiveModeId()
        val allModes = loadAllModes()
        val resolved = ModeResolver.resolve(allModes, control, manualId)

        val allNumbers = database.numberDao().getAllNumbersSync()
        val mapping = mutableMapOf<String, MutableSet<String>>()
        for (item in allNumbers) {
            mapping.getOrPut(item.normalizedNumber) { mutableSetOf() }.add(item.categoryId)
        }

        _cachedScreeningSnapshot.value = ScreeningSnapshot(
            isFirewallEnabled = enabled,
            activeMode = resolved?.copy(isEnabled = resolved.isEnabled && enabled),
            allModes = allModes.map { it.copy(isEnabled = it.isEnabled && enabled) },
            modeControl = control,
            manualModeId = manualId,
            numbersToCategoriesMap = mapping
        )
    }

    override fun getAllModes(): Flow<List<CallMode>> {
        return combine(
            database.modeDao().getAllModesFlow(),
            database.ruleDao().getAllRulesFlow()
        ) { modeList, allRules ->
            val rulesByMode = allRules.groupBy { it.modeId }
            modeList.map { entity ->
                val rules = (rulesByMode[entity.id] ?: emptyList()).map { mapRuleEntityToDomain(it) }
                mapModeEntity(entity, rules)
            }
        }
    }

    override suspend fun getActiveMode(): CallMode? {
        val modes = loadAllModes()
        return ModeResolver.resolve(modes, preferences.getModeControl(), preferences.getActiveModeId())
    }

    override fun getActiveModeFlow(): Flow<CallMode?> {
        return combine(
            getAllModes(),
            preferences.modeControl,
            preferences.activeModeIdFlow
        ) { modes, control, manualId ->
            ModeResolver.resolve(modes, control, manualId)
        }
    }

    override suspend fun setActiveModeId(modeId: String) {
        preferences.setModeControl(ModeControl.MANUAL)
        preferences.setActiveModeId(modeId)
        refreshHotCache()
    }

    override suspend fun setModeControl(control: ModeControl) {
        preferences.setModeControl(control)
        refreshHotCache()
    }

    override suspend fun createMode(name: String, defaultAction: CallAction): CallMode {
        val id = "mode_" + UUID.randomUUID().toString().take(8)
        val entity = CallModeEntity(
            id = id,
            name = name,
            isEnabled = true,
            isSystemDefault = false,
            defaultAction = defaultAction
        )
        database.modeDao().insertOrUpdateMode(entity)
        refreshHotCache()
        return mapModeEntity(entity, emptyList())
    }

    override suspend fun updateMode(mode: CallMode) {
        database.modeDao().insertOrUpdateMode(toEntity(mode))
        refreshHotCache()
    }

    override suspend fun deleteMode(modeId: String) {
        val entity = database.modeDao().getModeById(modeId) ?: return
        if (entity.isSystemDefault || entity.id == ModeResolver.DEFAULT_MODE_ID) return
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
        if (entity.isSystemDefault) return
        database.categoryDao().deleteCategory(entity)
        refreshHotCache()
    }

    override fun getRulesForMode(modeId: String): Flow<List<CallRule>> {
        return database.ruleDao().getRulesForModeFlow(modeId).map { list ->
            list.map { mapRuleEntityToDomain(it) }
        }
    }

    override suspend fun setRule(
        modeId: String,
        categoryId: String,
        action: CallAction,
        priority: Int,
        schedule: WeeklySchedule
    ) {
        val entity = CallRuleEntity(
            id = "rule_${modeId}_$categoryId",
            modeId = modeId,
            categoryId = categoryId,
            action = action,
            priority = priority,
            isScheduleEnabled = false
        )
        database.ruleDao().insertOrUpdateRule(entity)
        refreshHotCache()
    }

    override suspend fun setCategoryAction(modeId: String, categoryId: String, action: CallAction?) {
        if (action == null) {
            removeRule(modeId, categoryId)
        } else {
            setRule(modeId, categoryId, action, 50)
        }
    }

    override suspend fun setCategoryWhitelisted(modeId: String, categoryId: String, whitelisted: Boolean) {
        if (whitelisted) {
            setRule(modeId, categoryId, CallAction.ALLOW, 50)
        } else {
            removeRule(modeId, categoryId)
        }
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

    override suspend fun deleteNumberById(id: Long) {
        database.numberDao().deleteNumberById(id)
        refreshHotCache()
    }

    override fun getScreeningLogs(limit: Int): Flow<List<pro.nitron.zencall.data.local.entity.ScreeningLogEntity>> {
        return database.screeningLogDao().getRecentLogsFlow(limit)
    }

    override suspend fun recordScreeningLog(
        rawNumber: String?,
        normalizedNumber: String?,
        callerName: String?,
        action: CallAction,
        reason: String,
        modeName: String?,
        latencyMs: Long
    ) {
        val entity = pro.nitron.zencall.data.local.entity.ScreeningLogEntity(
            rawNumber = rawNumber,
            normalizedNumber = normalizedNumber,
            callerName = callerName,
            action = action,
            reason = reason,
            modeName = modeName,
            latencyMs = latencyMs
        )
        database.screeningLogDao().insertLog(entity)
    }

    override suspend fun clearLogs() {
        database.screeningLogDao().clearLogs()
    }

    override suspend fun deleteLogById(id: Long) {
        database.screeningLogDao().deleteLogById(id)
    }

    override suspend fun getFastScreeningSnapshot(): ScreeningSnapshot {
        refreshHotCache()
        return _cachedScreeningSnapshot.value
    }
}
