package pro.nitron.zencall.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pro.nitron.zencall.data.local.dao.CategoryDao
import pro.nitron.zencall.data.local.dao.ModeDao
import pro.nitron.zencall.data.local.dao.NumberDao
import pro.nitron.zencall.data.local.dao.RuleDao
import pro.nitron.zencall.data.local.entity.CallModeEntity
import pro.nitron.zencall.data.local.entity.CallRuleEntity
import pro.nitron.zencall.data.local.entity.CallerCategoryEntity
import pro.nitron.zencall.data.local.entity.CategoryNumberEntity
import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallerCategory
import pro.nitron.zencall.domain.model.SystemCategories

@Database(
    entities = [
        CallModeEntity::class,
        CallerCategoryEntity::class,
        CallRuleEntity::class,
        CategoryNumberEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ZenCallDatabase : RoomDatabase() {

    abstract fun modeDao(): ModeDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ruleDao(): RuleDao
    abstract fun numberDao(): NumberDao

    companion object {
        @Volatile
        private var INSTANCE: ZenCallDatabase? = null

        fun getInstance(context: Context): ZenCallDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZenCallDatabase::class.java,
                    "zencall_database.db"
                )
                    .addCallback(DatabasePrepopulateCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabasePrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedDefaultData(database)
                }
            }
        }

        private suspend fun seedDefaultData(database: ZenCallDatabase) {
            // 1. Seed Built-in System Categories
            val defaultCategoryEntities = CallerCategory.defaultCategories.map {
                CallerCategoryEntity(
                    id = it.id,
                    name = it.name,
                    isSystemDefault = it.isSystemDefault,
                    description = it.description,
                    colorHex = it.colorHex
                )
            }
            database.categoryDao().insertCategories(defaultCategoryEntities)

            // 2. Seed Default Modes: Personal, Work, Sleep
            val personalMode = CallModeEntity(
                id = "mode_personal",
                name = "Personal",
                isEnabled = true,
                defaultAction = CallAction.SILENCE
            )
            val workMode = CallModeEntity(
                id = "mode_work",
                name = "Work",
                isEnabled = true,
                defaultAction = CallAction.REJECT
            )
            val sleepMode = CallModeEntity(
                id = "mode_sleep",
                name = "Sleep",
                isEnabled = true,
                defaultAction = CallAction.SILENCE
            )
            database.modeDao().insertModes(listOf(personalMode, workMode, sleepMode))

            // 3. Seed Default Rules for Personal Mode
            val personalRules = listOf(
                CallRuleEntity("rule_p_allow", "mode_personal", SystemCategories.ALLOWED_NUMBER, CallAction.ALLOW, 100),
                CallRuleEntity("rule_p_block", "mode_personal", SystemCategories.BLOCKED_NUMBER, CallAction.REJECT, 90),
                CallRuleEntity("rule_p_fav", "mode_personal", SystemCategories.FAVORITE, CallAction.ALLOW, 50),
                CallRuleEntity("rule_p_contact", "mode_personal", SystemCategories.CONTACT, CallAction.ALLOW, 40),
                CallRuleEntity("rule_p_unknown", "mode_personal", SystemCategories.UNKNOWN, CallAction.SILENCE, 10)
            )

            // 4. Seed Default Rules for Work Mode
            val workRules = listOf(
                CallRuleEntity("rule_w_allow", "mode_work", SystemCategories.ALLOWED_NUMBER, CallAction.ALLOW, 100),
                CallRuleEntity("rule_w_block", "mode_work", SystemCategories.BLOCKED_NUMBER, CallAction.REJECT, 90),
                CallRuleEntity("rule_w_fav", "mode_work", SystemCategories.FAVORITE, CallAction.ALLOW, 50),
                CallRuleEntity("rule_w_contact", "mode_work", SystemCategories.CONTACT, CallAction.ALLOW, 40),
                CallRuleEntity("rule_w_unknown", "mode_work", SystemCategories.UNKNOWN, CallAction.REJECT, 10)
            )

            // 5. Seed Default Rules for Sleep Mode (Only VIP/Favorites Allowed)
            val sleepRules = listOf(
                CallRuleEntity("rule_s_allow", "mode_sleep", SystemCategories.ALLOWED_NUMBER, CallAction.ALLOW, 100),
                CallRuleEntity("rule_s_block", "mode_sleep", SystemCategories.BLOCKED_NUMBER, CallAction.REJECT, 90),
                CallRuleEntity("rule_s_fav", "mode_sleep", SystemCategories.FAVORITE, CallAction.ALLOW, 50),
                CallRuleEntity("rule_s_contact", "mode_sleep", SystemCategories.CONTACT, CallAction.SILENCE, 40),
                CallRuleEntity("rule_s_unknown", "mode_sleep", SystemCategories.UNKNOWN, CallAction.SILENCE, 10)
            )

            database.ruleDao().insertRules(personalRules + workRules + sleepRules)
        }
    }
}
