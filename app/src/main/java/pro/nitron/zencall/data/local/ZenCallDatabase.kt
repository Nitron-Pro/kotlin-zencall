package pro.nitron.zencall.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pro.nitron.zencall.data.local.dao.CategoryDao
import pro.nitron.zencall.data.local.dao.ModeDao
import pro.nitron.zencall.data.local.dao.NumberDao
import pro.nitron.zencall.data.local.dao.RuleDao
import pro.nitron.zencall.data.local.dao.ScreeningLogDao
import pro.nitron.zencall.data.local.entity.CallModeEntity
import pro.nitron.zencall.data.local.entity.CallRuleEntity
import pro.nitron.zencall.data.local.entity.CallerCategoryEntity
import pro.nitron.zencall.data.local.entity.CategoryNumberEntity
import pro.nitron.zencall.data.local.entity.ScreeningLogEntity
import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallerCategory
import pro.nitron.zencall.domain.model.SystemCategories

@Database(
    entities = [
        CallModeEntity::class,
        CallerCategoryEntity::class,
        CallRuleEntity::class,
        CategoryNumberEntity::class,
        ScreeningLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ZenCallDatabase : RoomDatabase() {

    abstract fun modeDao(): ModeDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ruleDao(): RuleDao
    abstract fun numberDao(): NumberDao
    abstract fun screeningLogDao(): ScreeningLogDao

    companion object {
        @Volatile
        private var INSTANCE: ZenCallDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_modes ADD COLUMN isSystemDefault INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE call_modes ADD COLUMN schedulePriority INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE call_modes ADD COLUMN isScheduleEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE call_modes ADD COLUMN scheduleDaysCsv TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7'")
                db.execSQL("ALTER TABLE call_modes ADD COLUMN startMinuteOfDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE call_modes ADD COLUMN endMinuteOfDay INTEGER NOT NULL DEFAULT 1439")
                db.execSQL("UPDATE call_modes SET isSystemDefault = 1 WHERE id = 'mode_personal'")
                db.execSQL("UPDATE call_modes SET name = 'روزمره', schedulePriority = 0, isScheduleEnabled = 0 WHERE id = 'mode_personal'")
                db.execSQL("UPDATE call_modes SET name = 'کاری', schedulePriority = 20, isScheduleEnabled = 1, scheduleDaysCsv = '6,7,1,2,3,4', startMinuteOfDay = 480, endMinuteOfDay = 1020 WHERE id = 'mode_work'")
                db.execSQL("UPDATE call_modes SET name = 'خواب', schedulePriority = 30, isScheduleEnabled = 1, scheduleDaysCsv = '1,2,3,4,5,6,7', startMinuteOfDay = 1320, endMinuteOfDay = 420 WHERE id = 'mode_sleep'")
                db.execSQL("UPDATE caller_categories SET name = 'مجاز' WHERE id = 'ALLOWED_NUMBER'")
                db.execSQL("UPDATE caller_categories SET name = 'مسدود' WHERE id = 'BLOCKED_NUMBER'")
                db.execSQL("UPDATE caller_categories SET name = 'VIP' WHERE id = 'FAVORITE'")
                db.execSQL("UPDATE caller_categories SET name = 'مخاطبین' WHERE id = 'CONTACT'")
                db.execSQL("UPDATE caller_categories SET name = 'ناشناس' WHERE id = 'UNKNOWN'")
                db.execSQL(
                    "INSERT OR IGNORE INTO caller_categories (id, name, isSystemDefault, description, colorHex, createdAt) VALUES ('PRIVATE', 'مخفی', 1, 'بدون شناسه تماس', NULL, ${System.currentTimeMillis()})"
                )
            }
        }

        fun getInstance(context: Context): ZenCallDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZenCallDatabase::class.java,
                    "zencall_database.db"
                )
                    .addCallback(DatabasePrepopulateCallback())
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                CoroutineScope(Dispatchers.IO).launch {
                    seedDefaultDataIfEmpty(instance)
                }

                instance
            }
        }

        suspend fun seedDefaultDataIfEmpty(database: ZenCallDatabase) {
            ensurePrivateCategory(database)
            shortenCategoryNames(database)

            val existingModes = database.modeDao().getModeById("mode_personal")
            if (existingModes != null) {
                if (!existingModes.isSystemDefault) {
                    database.modeDao().insertOrUpdateMode(existingModes.copy(isSystemDefault = true, name = "روزمره"))
                }
                return
            }

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

            val personalMode = CallModeEntity(
                id = "mode_personal",
                name = "روزمره",
                isEnabled = true,
                isSystemDefault = true,
                defaultAction = CallAction.REJECT,
                schedulePriority = 0,
                isScheduleEnabled = false
            )
            val workMode = CallModeEntity(
                id = "mode_work",
                name = "کاری",
                isEnabled = true,
                isSystemDefault = false,
                defaultAction = CallAction.REJECT,
                schedulePriority = 20,
                isScheduleEnabled = true,
                scheduleDaysCsv = "6,7,1,2,3,4",
                startMinuteOfDay = 8 * 60,
                endMinuteOfDay = 17 * 60
            )
            val sleepMode = CallModeEntity(
                id = "mode_sleep",
                name = "خواب",
                isEnabled = true,
                isSystemDefault = false,
                defaultAction = CallAction.REJECT,
                schedulePriority = 30,
                isScheduleEnabled = true,
                scheduleDaysCsv = "1,2,3,4,5,6,7",
                startMinuteOfDay = 22 * 60,
                endMinuteOfDay = 7 * 60
            )
            database.modeDao().insertModes(listOf(personalMode, workMode, sleepMode))

            val personalRules = listOf(
                CallRuleEntity("rule_p_allow", "mode_personal", SystemCategories.ALLOWED_NUMBER, CallAction.ALLOW, 100),
                CallRuleEntity("rule_p_fav", "mode_personal", SystemCategories.FAVORITE, CallAction.ALLOW, 50),
                CallRuleEntity("rule_p_contact", "mode_personal", SystemCategories.CONTACT, CallAction.ALLOW, 40)
            )
            val workRules = listOf(
                CallRuleEntity("rule_w_allow", "mode_work", SystemCategories.ALLOWED_NUMBER, CallAction.ALLOW, 100),
                CallRuleEntity("rule_w_fav", "mode_work", SystemCategories.FAVORITE, CallAction.ALLOW, 50)
            )
            val sleepRules = listOf(
                CallRuleEntity("rule_s_allow", "mode_sleep", SystemCategories.ALLOWED_NUMBER, CallAction.ALLOW, 100),
                CallRuleEntity("rule_s_fav", "mode_sleep", SystemCategories.FAVORITE, CallAction.ALLOW, 50)
            )
            database.ruleDao().insertRules(personalRules + workRules + sleepRules)
        }

        private suspend fun ensurePrivateCategory(database: ZenCallDatabase) {
            if (database.categoryDao().getCategoryById(SystemCategories.PRIVATE) == null) {
                database.categoryDao().insertOrUpdateCategory(
                    CallerCategoryEntity(
                        id = SystemCategories.PRIVATE,
                        name = "مخفی",
                        isSystemDefault = true,
                        description = "بدون شناسه تماس"
                    )
                )
            }
        }

        private suspend fun shortenCategoryNames(database: ZenCallDatabase) {
            val map = mapOf(
                SystemCategories.ALLOWED_NUMBER to "مجاز",
                SystemCategories.BLOCKED_NUMBER to "مسدود",
                SystemCategories.FAVORITE to "VIP",
                SystemCategories.CONTACT to "مخاطبین",
                SystemCategories.UNKNOWN to "ناشناس",
                SystemCategories.PRIVATE to "مخفی"
            )
            for ((id, name) in map) {
                val existing = database.categoryDao().getCategoryById(id) ?: continue
                if (existing.name != name) {
                    database.categoryDao().insertOrUpdateCategory(existing.copy(name = name))
                }
            }
        }
    }

    private class DatabasePrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedDefaultDataIfEmpty(database)
                }
            }
        }
    }
}
