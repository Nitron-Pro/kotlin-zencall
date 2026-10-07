package pro.nitron.zencall.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pro.nitron.zencall.domain.model.CallAction

@Entity(tableName = "call_modes")
data class CallModeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isEnabled: Boolean = true,
    val defaultAction: CallAction = CallAction.SILENCE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "caller_categories")
data class CallerCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isSystemDefault: Boolean = false,
    val description: String? = null,
    val colorHex: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "call_rules",
    foreignKeys = [
        ForeignKey(
            entity = CallModeEntity::class,
            parentColumns = ["id"],
            childColumns = ["modeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CallerCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["modeId"]),
        Index(value = ["categoryId"]),
        Index(value = ["modeId", "categoryId"], unique = true)
    ]
)
data class CallRuleEntity(
    @PrimaryKey val id: String,
    val modeId: String,
    val categoryId: String,
    val action: CallAction,
    val priority: Int = 0,
    // Weekly scheduling columns
    val isScheduleEnabled: Boolean = false,
    val scheduleDaysCsv: String = "1,2,3,4,5,6,7", // comma separated day values 1..7
    val startMinuteOfDay: Int = 0, // 0..1439
    val endMinuteOfDay: Int = 1439, // 0..1439
    val outsideScheduleAction: CallAction? = null
)

@Entity(
    tableName = "category_numbers",
    foreignKeys = [
        ForeignKey(
            entity = CallerCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["normalizedNumber"]),
        Index(value = ["categoryId", "normalizedNumber"], unique = true)
    ]
)
data class CategoryNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: String,
    val rawNumber: String,
    val normalizedNumber: String,
    val displayName: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
