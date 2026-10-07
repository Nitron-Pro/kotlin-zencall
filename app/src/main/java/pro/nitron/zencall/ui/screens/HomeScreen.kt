package pro.nitron.zencall.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import pro.nitron.zencall.domain.model.CallerCategory
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
    val modes by repository.getAllModes().collectAsState(initial = emptyList())
    val activeMode by repository.getActiveModeFlow().collectAsState(initial = null)
    val categories by repository.getAllCategories().collectAsState(initial = emptyList())

    // Dialog state for creating custom category
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryDesc by remember { mutableStateOf("") }

    // Dialog state for configuring weekly schedule for a category in active mode
    var scheduleTargetCategory by remember { mutableStateOf<CallerCategory?>(null) }
    var isScheduleEnabled by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6)) } // Shanbeh to Panjshanbeh (or standard Mon-Sat)
    var startHourStr by remember { mutableStateOf("08:00") }
    var endHourStr by remember { mutableStateOf("17:00") }
    var selectedAction by remember { mutableStateOf(CallAction.ALLOW) }
    var outsideAction by remember { mutableStateOf(CallAction.REJECT) }

    // Dialog state for adding a phone number to a category
    var numberTargetCategory by remember { mutableStateOf<CallerCategory?>(null) }
    var newNumberInput by remember { mutableStateOf("") }
    var newNumberName by remember { mutableStateOf("") }

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
                                text = "v0.1.0 • Weekly Schedules & Custom Groups",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

            Spacer(modifier = Modifier.height(20.dp))

            // Modes Section
            Text(
                text = "حالت‌های کاری تماس (Call Modes)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val currentActiveId = activeMode?.id ?: "mode_personal"
                    modes.forEach { mode ->
                        val isSelected = mode.id == currentActiveId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        repository.setActiveModeId(mode.id)
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "رفتار پیش‌فرض: ${mode.defaultAction.name} • ${mode.rules.size} قانون",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User Custom & System Categories Section (with Scheduling Action)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "دسته‌بندی‌ها و زمانبندی هفتگی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تعیین ساعت و روزهای مجاز/بلاک بودن هر گروه",
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
                Column(modifier = Modifier.padding(12.dp)) {
                    val activeRules = activeMode?.rules ?: emptyList()

                    categories.forEach { category ->
                        val matchingRule = activeRules.firstOrNull { it.categoryId == category.id }
                        val hasSchedule = matchingRule?.schedule?.isEnabled == true

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (category.isSystemDefault) MaterialTheme.colorScheme.primary else ZenShieldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (hasSchedule) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "زمانبندی فعال",
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (hasSchedule) {
                                    val startH = String.format("%02d:%02d", matchingRule!!.schedule.startMinuteOfDay / 60, matchingRule.schedule.startMinuteOfDay % 60)
                                    val endH = String.format("%02d:%02d", matchingRule.schedule.endMinuteOfDay / 60, matchingRule.schedule.endMinuteOfDay % 60)
                                    Text(
                                        text = "⏰ ساعات مجاز: $startH تا $endH (خارج ساعت: ${matchingRule.schedule.outsideScheduleAction ?: CallAction.REJECT})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    category.description?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Add Number Button
                            IconButton(onClick = { numberTargetCategory = category }) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "افزودن شماره",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Configure Schedule Button
                            IconButton(onClick = {
                                scheduleTargetCategory = category
                                if (matchingRule != null && matchingRule.schedule.isEnabled) {
                                    isScheduleEnabled = true
                                    selectedDays = matchingRule.schedule.daysOfWeek
                                    val sMin = matchingRule.schedule.startMinuteOfDay
                                    val eMin = matchingRule.schedule.endMinuteOfDay
                                    startHourStr = String.format("%02d:%02d", sMin / 60, sMin % 60)
                                    endHourStr = String.format("%02d:%02d", eMin / 60, eMin % 60)
                                    selectedAction = matchingRule.action
                                    outsideAction = matchingRule.schedule.outsideScheduleAction ?: CallAction.REJECT
                                } else {
                                    isScheduleEnabled = false
                                    selectedDays = setOf(1, 2, 3, 4, 5, 6)
                                    startHourStr = "08:00"
                                    endHourStr = "17:00"
                                    selectedAction = CallAction.ALLOW
                                    outsideAction = CallAction.REJECT
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "تنظیم زمانبندی",
                                    tint = if (hasSchedule) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Delete button (only for non-system)
                            if (!category.isSystemDefault) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.deleteCategory(category.id)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف گروه",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
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
            title = { Text("افزودن شماره به «${cat.name}»", fontWeight = FontWeight.Bold) },
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newNumberName,
                        onValueChange = { newNumberName = it },
                        label = { Text("نام مخاطب (اختیاری)") },
                        modifier = Modifier.fillMaxWidth()
                    )
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

    // Dialog 2: Weekly Schedule Configuration
    if (scheduleTargetCategory != null) {
        val cat = scheduleTargetCategory!!
        AlertDialog(
            onDismissRequest = { scheduleTargetCategory = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("زمانبندی گروه «${cat.name}»", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("فعال‌سازی زمانبندی ساعتی/هفتگی", fontWeight = FontWeight.SemiBold)
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

                        // Day chips: 6 (Shanbeh) .. 5 (Jomeh) or 1..7
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
                            text = "در این بازه زمانی شماره‌ها:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "✅ مجاز (ALLOW) — زنگ می‌خورد",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZenShieldGreen,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "خارج از این ساعات یا روزهای دیگر:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "⛔ مسدود (REJECT) — بلاک خودکار",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZenShieldRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activeModeId = activeMode?.id ?: "mode_personal"

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

                        val schedule = WeeklySchedule(
                            isEnabled = isScheduleEnabled,
                            daysOfWeek = selectedDays,
                            startMinuteOfDay = sMin,
                            endMinuteOfDay = eMin,
                            outsideScheduleAction = if (isScheduleEnabled) outsideAction else null
                        )

                        coroutineScope.launch {
                            repository.setRule(
                                modeId = activeModeId,
                                categoryId = cat.id,
                                action = selectedAction,
                                priority = 20, // High priority for scheduled group
                                schedule = schedule
                            )
                            scheduleTargetCategory = null
                        }
                    }
                ) {
                    Text("ذخیره زمانبندی")
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleTargetCategory = null }) {
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
}
