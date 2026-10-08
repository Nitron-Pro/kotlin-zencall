package pro.nitron.zencall.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pro.nitron.zencall.R
import pro.nitron.zencall.data.prefs.FirewallPreferences
import pro.nitron.zencall.data.repository.FirewallRepository
import pro.nitron.zencall.data.repository.FirewallRepositoryImpl
import pro.nitron.zencall.domain.model.CallAction
import pro.nitron.zencall.domain.model.CallMode
import pro.nitron.zencall.domain.model.CallerCategory
import pro.nitron.zencall.domain.model.ModeControl
import pro.nitron.zencall.domain.model.SystemCategories
import pro.nitron.zencall.domain.model.WeeklySchedule
import pro.nitron.zencall.ui.theme.ZenShieldGreen
import pro.nitron.zencall.ui.theme.ZenShieldRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository: FirewallRepository = remember { FirewallRepositoryImpl(context) }
    val preferences = remember { FirewallPreferences(context) }

    val isFirewallEnabled by preferences.isFirewallEnabled.collectAsState(initial = true)
    val modeControl by preferences.modeControl.collectAsState(initial = ModeControl.AUTO)
    val modes by repository.getAllModes().collectAsState(initial = emptyList())
    val activeMode by repository.getActiveModeFlow().collectAsState(initial = null)
    val categories by repository.getAllCategories().collectAsState(initial = emptyList())

    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryDesc by remember { mutableStateOf("") }
    var showCreateModeDialog by remember { mutableStateOf(false) }
    var newModeName by remember { mutableStateOf("") }

    var scheduleTargetMode by remember { mutableStateOf<CallMode?>(null) }
    var isScheduleEnabled by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf(6, 7, 1, 2, 3, 4)) }
    var startHourStr by remember { mutableStateOf("08:00") }
    var endHourStr by remember { mutableStateOf("17:00") }
    var whitelistTargetCategory by remember { mutableStateOf<CallerCategory?>(null) }

    // Dialog state for configuring policy across modes for a category
    var policyTargetCategory by remember { mutableStateOf<CallerCategory?>(null) }

    // Dialog state for adding a phone number to a category
    var numberTargetCategory by remember { mutableStateOf<CallerCategory?>(null) }
    var newNumberInput by remember { mutableStateOf("") }
    var newNumberName by remember { mutableStateOf("") }

    // Dialog state for viewing numbers of a category
    var viewCategoryNumbersTarget by remember { mutableStateOf<CallerCategory?>(null) }

    // Dialog state for Call Screening History Logs
    var showLogsDialog by remember { mutableStateOf(false) }

    // Dialog state for choosing recent call log to add
    var showRecentCallsDialogForCategory by remember { mutableStateOf<CallerCategory?>(null) }
    var recentCallsList by remember { mutableStateOf<List<Pair<String, String?>>>(emptyList()) }

    // Contact Picker Launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            val contentResolver = context.contentResolver
            var contactId: String? = null
            var displayName: String? = null

            // Query contact name and ID
            contentResolver.query(
                contactUri,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (idIdx != -1) contactId = cursor.getString(idIdx)
                    if (nameIdx != -1) displayName = cursor.getString(nameIdx)
                }
            }

            if (displayName != null) {
                newNumberName = displayName ?: ""
            }

            // Query primary phone number for contact
            if (contactId != null) {
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numIdx != -1) {
                            val rawNum = phoneCursor.getString(numIdx)
                            if (!rawNum.isNullOrBlank()) {
                                newNumberInput = rawNum.replace(" ", "").replace("-", "")
                            }
                        }
                    }
                }
            }
        }
    }

    // RoleManager State for Call Screening
    var isCallScreeningRoleHeld by remember { mutableStateOf(false) }

    fun checkRoleHeld() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            isCallScreeningRoleHeld = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            isCallScreeningRoleHeld = true
        }
    }

    LaunchedEffect(Unit) {
        checkRoleHeld()
        repository.getActiveMode() // Triggers database seed and loads default modes/categories
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        checkRoleHeld()
    }

    fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleLauncher.launch(intent)
            }
        }
    }

    val glowColor by animateColorAsState(
        targetValue = if (isFirewallEnabled && isCallScreeningRoleHeld) ZenShieldGreen else ZenShieldRed,
        label = "GlowColor"
    )

    fun loadRecentCallLogs(): List<Pair<String, String?>> {
        val list = mutableListOf<Pair<String, String?>>()
        val seen = mutableSetOf<String>()
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 50"
            )
            cursor?.use {
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                while (it.moveToNext()) {
                    val raw = if (numIdx != -1) it.getString(numIdx) else null
                    val name = if (nameIdx != -1) it.getString(nameIdx) else null
                    if (!raw.isNullOrBlank()) {
                        val cleaned = raw.replace(" ", "").replace("-", "")
                        if (seen.add(cleaned)) {
                            list.add(Pair(cleaned, name?.ifBlank { null }))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_zencall_logo),
                            contentDescription = "ZenCall Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "فایروال هوشمند تماس",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showLogsDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "تاریخچه مسدودسازی و تماس‌ها",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Role Permission Warning Banner
            if (!isCallScreeningRoleHeld) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مجوز اصلی مسدودسازی تماس نیاز است",
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "برای مسدودسازی تماس‌های مزاحم بدون زنگ خوردن گوشی، باید ZenCall به عنوان فیلتر تماس تایید شود.",
                            color = Color(0xFFFEF3C7),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { requestCallScreeningRole() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "فعال‌سازی مجوز غربالگری (Screening Role)",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Central Zen Hero Widget
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_zencall_logo),
                    contentDescription = "ZenCall 3D Logo",
                    modifier = Modifier.size(115.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isFirewallEnabled) "فایروال فعال است" else "فایروال غیرفعال است",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isFirewallEnabled) ZenShieldGreen else ZenShieldRed
            )

            Text(
                text = if (isFirewallEnabled) {
                    "حالت فعال: ${activeMode?.name ?: "Personal"}"
                } else {
                    "تمام تماس‌های ورودی بدون فیلتر وصل می‌شوند"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Master Switch Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "موتور اصلی فایروال (Core Engine)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isFirewallEnabled) "در حال پایش هوشمند بر اساس زمانبندی" else "موتور متوقف شده است",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isFirewallEnabled,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                preferences.setFirewallEnabled(checked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ZenShieldGreen,
                            checkedTrackColor = ZenShieldGreen.copy(alpha = 0.35f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Call History / Screening Logs Banner Button
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        println("DEBUG_ZENCALL: showLogsDialog clicked")
                        showLogsDialog = true
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "تاریخچه و لاگ فیلتر تماس‌ها",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "مشاهده تماس‌های ورودی و تصمیم فایروال به همراه افزودن سریع",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Modes Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "حالت‌های کاری تماس",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (modeControl == ModeControl.AUTO) "کنترل خودکار (طبق زمانبندی)" else "کنترل دستی (انتخاب کاربر)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = modeControl == ModeControl.AUTO,
                        onClick = {
                            coroutineScope.launch {
                                preferences.setModeControl(
                                    if (modeControl == ModeControl.AUTO) ModeControl.MANUAL else ModeControl.AUTO
                                )
                            }
                        },
                        label = {
                            Text(if (modeControl == ModeControl.AUTO) "خودکار ⚡" else "دستی 🖐️", fontSize = 12.sp)
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { showCreateModeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزودن حالت جدید",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (modes.isEmpty()) {
                        Text(
                            text = "در حال بارگذاری حالت‌های کاری...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    val currentActiveId = activeMode?.id ?: "mode_default"
                    modes.forEach { mode ->
                        val isSelected = mode.id == currentActiveId
                        val hasSchedule = mode.schedule.isEnabled
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        repository.setActiveModeId(mode.id)
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    coroutineScope.launch {
                                        repository.setActiveModeId(mode.id)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mode.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    if (mode.isSystemDefault) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(پیش‌فرض پایه)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (hasSchedule) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "زمانبندی دارد",
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                val startH = String.format("%02d:%02d", mode.schedule.startMinuteOfDay / 60, mode.schedule.startMinuteOfDay % 60)
                                val endH = String.format("%02d:%02d", mode.schedule.endMinuteOfDay / 60, mode.schedule.endMinuteOfDay % 60)
                                val schedText = if (hasSchedule) "⏰ $startH تا $endH" else "فاقد زمانبندی خودکار"
                                Text(
                                    text = "پیش‌فرض: ${mode.defaultAction.name} • $schedText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Schedule button (for non-default modes)
                            if (!mode.isSystemDefault) {
                                IconButton(
                                    onClick = {
                                        scheduleTargetMode = mode
                                        isScheduleEnabled = mode.schedule.isEnabled
                                        selectedDays = mode.schedule.daysOfWeek
                                        val sMin = mode.schedule.startMinuteOfDay
                                        val eMin = mode.schedule.endMinuteOfDay
                                        startHourStr = String.format("%02d:%02d", sMin / 60, sMin % 60)
                                        endHourStr = String.format("%02d:%02d", eMin / 60, eMin % 60)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "تنظیم زمانبندی حالت",
                                        tint = if (hasSchedule) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // Delete button (only custom modes)
                            if (!mode.isSystemDefault) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.deleteMode(mode.id)
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف حالت",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User Custom & System Categories Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "دسته‌بندی‌ها و رفتار در حالت فعال",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تعیین اکشن هر گروه در حالت «${activeMode?.name ?: "پایه"}»",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showCreateCategoryDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "افزودن دسته‌بندی دلخواه",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (categories.isEmpty()) {
                        Text(
                            text = "در حال بارگذاری دسته‌بندی‌ها...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    categories.forEach { category ->
                        val currentModeId = activeMode?.id ?: "mode_default"
                        val activeModeRules by repository.getRulesForMode(currentModeId).collectAsState(initial = emptyList())
                        val groupAction = activeModeRules.firstOrNull { it.categoryId == category.id }?.action

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (category.holdsNumbers) Icons.Default.Folder else Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = if (category.isSystemDefault) MaterialTheme.colorScheme.primary else ZenShieldGreen,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable(enabled = category.holdsNumbers) { viewCategoryNumbersTarget = category }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = category.holdsNumbers) { viewCategoryNumbersTarget = category }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (category.isBlockList) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(مسدود قطعی)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ZenShieldRed
                                            )
                                        }
                                    }
                                }

                                // Settings button to configure this category across all modes
                                if (!category.isBlockList) {
                                    IconButton(
                                        onClick = { policyTargetCategory = category },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "تنظیم در تمام حالت‌ها",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // If category holds numbers, allow adding and viewing numbers
                                if (category.holdsNumbers) {
                                    IconButton(
                                        onClick = { numberTargetCategory = category },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "افزودن شماره",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            recentCallsList = loadRecentCallLogs()
                                            showRecentCallsDialogForCategory = category
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "افزودن از تاریخچه تماس‌ها",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Delete button (only for custom user categories)
                                if (!category.isSystemDefault) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.deleteCategory(category.id)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف گروه",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Row 2: 3-state Action Selector for Current Active Mode
                            if (!category.isBlockList) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 32.dp, top = 2.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "در «${activeMode?.name ?: "پایه"}»:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    AssistChip(
                                        onClick = {
                                            coroutineScope.launch {
                                                val next = if (groupAction == CallAction.ALLOW) null else CallAction.ALLOW
                                                repository.setCategoryAction(currentModeId, category.id, next)
                                            }
                                        },
                                        label = { Text("مجاز", fontSize = 11.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (groupAction == CallAction.ALLOW) ZenShieldGreen.copy(alpha = 0.25f) else Color.Transparent,
                                            labelColor = if (groupAction == CallAction.ALLOW) ZenShieldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(
                                            enabled = true,
                                            borderColor = if (groupAction == CallAction.ALLOW) ZenShieldGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    )

                                    AssistChip(
                                        onClick = {
                                            coroutineScope.launch {
                                                val next = if (groupAction == CallAction.SILENCE) null else CallAction.SILENCE
                                                repository.setCategoryAction(currentModeId, category.id, next)
                                            }
                                        },
                                        label = { Text("سایلنت", fontSize = 11.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (groupAction == CallAction.SILENCE) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color.Transparent,
                                            labelColor = if (groupAction == CallAction.SILENCE) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(
                                            enabled = true,
                                            borderColor = if (groupAction == CallAction.SILENCE) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    )

                                    AssistChip(
                                        onClick = {
                                            coroutineScope.launch {
                                                val next = if (groupAction == CallAction.REJECT) null else CallAction.REJECT
                                                repository.setCategoryAction(currentModeId, category.id, next)
                                            }
                                        },
                                        label = { Text("بلاک", fontSize = 11.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (groupAction == CallAction.REJECT) ZenShieldRed.copy(alpha = 0.25f) else Color.Transparent,
                                            labelColor = if (groupAction == CallAction.REJECT) ZenShieldRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(
                                            enabled = true,
                                            borderColor = if (groupAction == CallAction.REJECT) ZenShieldRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialog 1: Add Number to Category
    if (numberTargetCategory != null) {
        val cat = numberTargetCategory!!
        AlertDialog(
            onDismissRequest = { numberTargetCategory = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("افزودن شماره به «${cat.name}»", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "شماره را وارد کنید (مثال: 09121112233 یا با کد کشور)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newNumberInput,
                        onValueChange = { newNumberInput = it },
                        label = { Text("شماره تلفن") },
                        trailingIcon = {
                            IconButton(onClick = { contactPickerLauncher.launch(null) }) {
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = "انتخاب از مخاطبین",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newNumberName,
                        onValueChange = { newNumberName = it },
                        label = { Text("نام مخاطب (اختیاری)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { contactPickerLauncher.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("دفترچه تلفن", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                recentCallsList = loadRecentCallLogs()
                                showRecentCallsDialogForCategory = cat
                                numberTargetCategory = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("لیست تماس‌ها", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNumberInput.isNotBlank()) {
                            coroutineScope.launch {
                                repository.addNumberToCategory(
                                    categoryId = cat.id,
                                    rawNumber = newNumberInput.trim(),
                                    displayName = newNumberName.trim().ifBlank { null }
                                )
                                newNumberInput = ""
                                newNumberName = ""
                                numberTargetCategory = null
                            }
                        }
                    }
                ) {
                    Text("ثبت شماره")
                }
            },
            dismissButton = {
                TextButton(onClick = { numberTargetCategory = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog: Configure Category Policy Across All Modes
    if (policyTargetCategory != null) {
        val cat = policyTargetCategory!!
        AlertDialog(
            onDismissRequest = { policyTargetCategory = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("تنظیم رفتار گروه «${cat.name}»", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("تعیین عملکرد تماس این دسته در هر حالت کاری", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (modes.isEmpty()) {
                        Text("حالتی یافت نشد.", style = MaterialTheme.typography.bodySmall)
                    }

                    modes.forEach { mode ->
                        val currentModeRules by repository.getRulesForMode(mode.id).collectAsState(initial = emptyList())
                        val action = currentModeRules.firstOrNull { it.categoryId == cat.id }?.action
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = mode.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (mode.isSystemDefault) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "(پایه)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    val statusText = when (action) {
                                        CallAction.ALLOW -> "✅ مجاز"
                                        CallAction.SILENCE -> "🔕 سایلنت"
                                        CallAction.REJECT -> "⛔ مسدود"
                                        null -> "ارث‌بری: ${mode.defaultAction.name}"
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (action) {
                                            CallAction.ALLOW -> ZenShieldGreen
                                            CallAction.SILENCE -> Color(0xFFF59E0B)
                                            CallAction.REJECT -> ZenShieldRed
                                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AssistChip(
                                        onClick = {
                                            coroutineScope.launch {
                                                val next = if (action == CallAction.ALLOW) null else CallAction.ALLOW
                                                repository.setCategoryAction(mode.id, cat.id, next)
                                            }
                                        },
                                        label = { Text("مجاز", fontSize = 11.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (action == CallAction.ALLOW) ZenShieldGreen.copy(alpha = 0.25f) else Color.Transparent,
                                            labelColor = if (action == CallAction.ALLOW) ZenShieldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(
                                            enabled = true,
                                            borderColor = if (action == CallAction.ALLOW) ZenShieldGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    )

                                    AssistChip(
                                        onClick = {
                                            coroutineScope.launch {
                                                val next = if (action == CallAction.SILENCE) null else CallAction.SILENCE
                                                repository.setCategoryAction(mode.id, cat.id, next)
                                            }
                                        },
                                        label = { Text("سایلنت", fontSize = 11.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (action == CallAction.SILENCE) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color.Transparent,
                                            labelColor = if (action == CallAction.SILENCE) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(
                                            enabled = true,
                                            borderColor = if (action == CallAction.SILENCE) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    )

                                    AssistChip(
                                        onClick = {
                                            coroutineScope.launch {
                                                val next = if (action == CallAction.REJECT) null else CallAction.REJECT
                                                repository.setCategoryAction(mode.id, cat.id, next)
                                            }
                                        },
                                        label = { Text("بلاک", fontSize = 11.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (action == CallAction.REJECT) ZenShieldRed.copy(alpha = 0.25f) else Color.Transparent,
                                            labelColor = if (action == CallAction.REJECT) ZenShieldRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(
                                            enabled = true,
                                            borderColor = if (action == CallAction.REJECT) ZenShieldRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { policyTargetCategory = null }) {
                    Text("بستن")
                }
            }
        )
    }

    // Dialog 2: Mode Schedule Configuration
    if (scheduleTargetMode != null) {
        val targetMode = scheduleTargetMode!!
        AlertDialog(
            onDismissRequest = { scheduleTargetMode = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("زمانبندی خودکار حالت «${targetMode.name}»", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("فعال‌سازی زمانبندی خودکار", fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = isScheduleEnabled,
                            onCheckedChange = { isScheduleEnabled = it }
                        )
                    }

                    if (isScheduleEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "روزهای فعال:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val dayNames = listOf(
                            6 to "شنبه",
                            7 to "یکشنبه",
                            1 to "دوشنبه",
                            2 to "سه‌شنبه",
                            3 to "چهارشنبه",
                            4 to "پنجشنبه",
                            5 to "جمعه"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            dayNames.take(4).forEach { (dayVal, label) ->
                                val isDaySelected = dayVal in selectedDays
                                FilterChip(
                                    selected = isDaySelected,
                                    onClick = {
                                        selectedDays = if (isDaySelected) selectedDays - dayVal else selectedDays + dayVal
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            dayNames.drop(4).forEach { (dayVal, label) ->
                                val isDaySelected = dayVal in selectedDays
                                FilterChip(
                                    selected = isDaySelected,
                                    onClick = {
                                        selectedDays = if (isDaySelected) selectedDays - dayVal else selectedDays + dayVal
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startHourStr,
                                onValueChange = { startHourStr = it },
                                label = { Text("از ساعت (08:00)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = endHourStr,
                                onValueChange = { endHourStr = it },
                                label = { Text("تا ساعت (17:00)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "💡 در این بازه، در حالت خودکار (AUTO) این حالت فعال خواهد بود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        fun parseMin(str: String, default: Int): Int {
                            val parts = str.split(":")
                            if (parts.size == 2) {
                                val h = parts[0].trim().toIntOrNull() ?: 0
                                val m = parts[1].trim().toIntOrNull() ?: 0
                                return (h * 60 + m).coerceIn(0, 1439)
                            }
                            return default
                        }

                        val sMin = parseMin(startHourStr, 8 * 60)
                        val eMin = parseMin(endHourStr, 17 * 60)

                        val updatedSchedule = WeeklySchedule(
                            isEnabled = isScheduleEnabled,
                            daysOfWeek = selectedDays,
                            startMinuteOfDay = sMin,
                            endMinuteOfDay = eMin
                        )

                        coroutineScope.launch {
                            repository.updateMode(targetMode.copy(schedule = updatedSchedule))
                            scheduleTargetMode = null
                        }
                    }
                ) {
                    Text("ذخیره زمانبندی")
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleTargetMode = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog: Create Mode Dialog
    if (showCreateModeDialog) {
        var modeDefaultAction by remember { mutableStateOf(CallAction.REJECT) }
        AlertDialog(
            onDismissRequest = { showCreateModeDialog = false },
            title = { Text(text = "ایجاد حالت کاری جدید", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newModeName,
                        onValueChange = { newModeName = it },
                        label = { Text("نام حالت (مثال: جلسه کاری، استراحت)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("رفتار پیش‌فرض برای تماس‌های متفرقه:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = modeDefaultAction == CallAction.REJECT,
                            onClick = { modeDefaultAction = CallAction.REJECT },
                            label = { Text("مسدودسازی") }
                        )
                        FilterChip(
                            selected = modeDefaultAction == CallAction.SILENCE,
                            onClick = { modeDefaultAction = CallAction.SILENCE },
                            label = { Text("بی‌صدا") }
                        )
                        FilterChip(
                            selected = modeDefaultAction == CallAction.ALLOW,
                            onClick = { modeDefaultAction = CallAction.ALLOW },
                            label = { Text("آزاد") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newModeName.isNotBlank()) {
                            coroutineScope.launch {
                                repository.createMode(
                                    name = newModeName.trim(),
                                    defaultAction = modeDefaultAction
                                )
                                newModeName = ""
                                showCreateModeDialog = false
                            }
                        }
                    }
                ) {
                    Text("ایجاد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateModeDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog 3: Create Category Dialog
    if (showCreateCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            title = { Text(text = "ایجاد گروه/دسته‌بندی جدید تماس", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "می‌توانید گروه‌های دلخواه مثل «همکاران شرکت»، «پیک و رانندگان» یا «خانواده» ایجاد کنید و برای آن‌ها زمانبندی ساعتی و روزانه بگذارید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("نام دسته (مثال: همکاران شرکت)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCategoryDesc,
                        onValueChange = { newCategoryDesc = it },
                        label = { Text("توضیحات اختیاری") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            coroutineScope.launch {
                                repository.createCustomCategory(
                                    name = newCategoryName.trim(),
                                    description = newCategoryDesc.trim().ifBlank { null }
                                )
                                newCategoryName = ""
                                newCategoryDesc = ""
                                showCreateCategoryDialog = false
                            }
                        }
                    }
                ) {
                    Text("ایجاد گروه")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCategoryDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog 4: Recent Calls Picker Dialog
    if (showRecentCallsDialogForCategory != null) {
        val cat = showRecentCallsDialogForCategory!!
        AlertDialog(
            onDismissRequest = { showRecentCallsDialogForCategory = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("انتخاب از تماس‌های اخیر", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (recentCallsList.isEmpty()) {
                        Text(
                            text = "تماسی در تاریخچه گوشی یافت نشد یا دسترسی تماس داده نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "شماره مورد نظر را لمس کنید تا انتخاب شود:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        recentCallsList.forEach { (number, contactName) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        newNumberInput = number
                                        newNumberName = contactName ?: ""
                                        numberTargetCategory = cat
                                        showRecentCallsDialogForCategory = null
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contactName ?: number,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (contactName != null) {
                                        Text(
                                            text = number,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "انتخاب",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRecentCallsDialogForCategory = null }) {
                    Text("بستن")
                }
            }
        )
    }

    // Dialog 5: View & Manage Category Numbers
    if (viewCategoryNumbersTarget != null) {
        val cat = viewCategoryNumbersTarget!!
        val numbersFlow by repository.getNumbersForCategory(cat.id).collectAsState(initial = emptyList())

        AlertDialog(
            onDismissRequest = { viewCategoryNumbersTarget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("شماره‌های «${cat.name}»", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (numbersFlow.isEmpty()) {
                        Text(
                            text = "هنوز شماره‌ای به این دسته اضافه نشده است.\nمی‌توانید با زدن آیکون گوشی تلفن شماره جدید اضافه کنید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        numbersFlow.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayName ?: item.rawNumber,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (item.displayName != null) {
                                        Text(
                                            text = item.rawNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "نرمال‌شده: ${item.normalizedNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.deleteNumberById(item.id)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف شماره",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetCat = cat
                        viewCategoryNumbersTarget = null
                        numberTargetCategory = targetCat
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("افزودن شماره")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewCategoryNumbersTarget = null }) {
                    Text("بستن")
                }
            }
        )
    }

    // Dialog 6: Screening History Logs
    if (showLogsDialog) {
        val logs by repository.getScreeningLogs(100).collectAsState(initial = emptyList())
        var numberToAddToCategoryFromLog by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showLogsDialog = false
                numberToAddToCategoryFromLog = null
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تاریخچه فیلتر تماس‌ها", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    repository.clearLogs()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "پاک کردن تاریخچه",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "هنوز تماسی پردازش نشده است.\nبه محض دریافت تماس ورودی، نتیجه تصمیم فایروال با جزئیات دقیق اینجا ثبت می‌شود.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        Text(
                            text = "💡 نکته: برای افزودن سریع هر شماره به گروه‌ها، روی شماره لمس کنید.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        logs.forEach { logItem ->
                            val isRejected = logItem.action == CallAction.REJECT
                            val isSilenced = logItem.action == CallAction.SILENCE
                            val badgeColor = if (isRejected) ZenShieldRed else if (isSilenced) Color(0xFFF59E0B) else ZenShieldGreen

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val num = logItem.rawNumber ?: logItem.normalizedNumber
                                        if (!num.isNullOrBlank()) {
                                            numberToAddToCategoryFromLog = num
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = logItem.callerName ?: logItem.rawNumber ?: "ناشناس/Private",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = logItem.action.name,
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }

                                        if (logItem.callerName != null && logItem.rawNumber != null) {
                                            Text(
                                                text = logItem.rawNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = "دلیل: ${logItem.reason} (${logItem.latencyMs}ms)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.deleteLogById(logItem.id)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف لاگ",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sub-dialog / Selector when a number from log is tapped to be added to a category
                    if (numberToAddToCategoryFromLog != null) {
                        val num = numberToAddToCategoryFromLog!!
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "افزودن شماره $num به کدام گروه؟",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        categories.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        coroutineScope.launch {
                                            repository.addNumberToCategory(
                                                categoryId = cat.id,
                                                rawNumber = num,
                                                displayName = null
                                            )
                                            numberToAddToCategoryFromLog = null
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = ZenShieldGreen, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLogsDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }
}
