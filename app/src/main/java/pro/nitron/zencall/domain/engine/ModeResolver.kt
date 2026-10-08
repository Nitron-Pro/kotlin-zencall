package pro.nitron.zencall.domain.engine

import pro.nitron.zencall.domain.model.CallMode
import pro.nitron.zencall.domain.model.ModeControl
import java.time.LocalDateTime

object ModeResolver {
    const val DEFAULT_MODE_ID = "mode_personal"

    fun resolve(
        modes: List<CallMode>,
        control: ModeControl,
        manualModeId: String?,
        now: LocalDateTime = LocalDateTime.now()
    ): CallMode? {
        if (modes.isEmpty()) return null
        val fallback = modes.firstOrNull { it.id == DEFAULT_MODE_ID }
            ?: modes.firstOrNull { it.isSystemDefault }
            ?: modes.first()

        if (control == ModeControl.MANUAL) {
            return modes.firstOrNull { it.id == manualModeId } ?: fallback
        }

        val day = now.dayOfWeek.value
        val minute = now.hour * 60 + now.minute
        return modes
            .filter { it.schedule.isMatching(day, minute) }
            .maxByOrNull { it.schedulePriority }
            ?: fallback
    }
}
