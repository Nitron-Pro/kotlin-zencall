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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import pro.nitron.zencall.domain.model.CallerCategory
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
                                text = "v0.1.0 • Room & DataStore Active",
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
                            text = if (isFirewallEnabled) "در حال محافظت با اولویت <5ms" else "موتور متوقف شده است",
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

            // User Custom & System Categories Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دسته‌بندی‌های تماس (Caller Categories)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showCreateCategoryDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "افزودن دسته‌بندی دلخواه",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 8.dp),
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
                                    if (category.isSystemDefault) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(پیش‌فرض سیستم)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(سفارشی کاربر)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ZenShieldGreen
                                        )
                                    }
                                }
                                category.description?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                                        contentDescription = "حذف گروه سفارشی",
                                        tint = MaterialTheme.colorScheme.error
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

    // Dialog for creating user's custom CallerCategory
    if (showCreateCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            title = { Text(text = "ایجاد گروه/دسته‌بندی جدید تماس", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "می‌توانید گروه‌های دلخواه مثل «همکاران شرکت»، «پیک و رانندگان» یا «خانواده درجه ۱» ایجاد کنید و برای آن‌ها قوانین دلخواه تنظیم نمایید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("نام دسته (مثال: همکاران)") },
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
