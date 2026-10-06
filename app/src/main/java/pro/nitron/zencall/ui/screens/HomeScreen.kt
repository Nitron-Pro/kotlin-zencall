package pro.nitron.zencall.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import pro.nitron.zencall.data.prefs.FirewallMode
import pro.nitron.zencall.data.prefs.FirewallPreferences
import pro.nitron.zencall.ui.theme.ZenCardDark
import pro.nitron.zencall.ui.theme.ZenNavy
import pro.nitron.zencall.ui.theme.ZenShieldGreen
import pro.nitron.zencall.ui.theme.ZenShieldRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { FirewallPreferences(context) }

    val isFirewallEnabled by preferences.isFirewallEnabled.collectAsState(initial = true)
    val currentMode by preferences.firewallMode.collectAsState(initial = FirewallMode.BLOCK_UNKNOWN)
    val blockPrivate by preferences.blockPrivateNumbers.collectAsState(initial = true)
    val silentDrop by preferences.silentDrop.collectAsState(initial = true)

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
                                text = "v0.1.0 • Core Screening Active",
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
                    .size(160.dp)
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
                    modifier = Modifier.size(120.dp)
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
                text = when {
                    !isFirewallEnabled -> "تمام تماس‌های ورودی آزاد هستند"
                    currentMode == FirewallMode.BLOCK_UNKNOWN -> "رد خودکار تمام شماره‌های ناشناس (خارج از مخاطبین)"
                    currentMode == FirewallMode.BLOCK_BLACKLIST -> "مسدودسازی شماره‌های لیست سیاه"
                    currentMode == FirewallMode.BLOCK_ALL -> "سکوت کامل — مسدودسازی تمام تماس‌ها"
                    else -> "پایش هوشمند تماس‌های ورودی"
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
                            text = if (isFirewallEnabled) "در حال محافظت و رهگیری تماس" else "موتور متوقف شده است",
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

            Spacer(modifier = Modifier.height(16.dp))

            // Firewall Modes Section
            Text(
                text = "حالت‌های غربالگری و فیلترینگ",
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

                    ModeRow(
                        title = "مسدودسازی ناشناس‌ها (توصیه شده)",
                        subtitle = "فقط مخاطبین شما مجاز به برقراری تماس هستند",
                        isSelected = currentMode == FirewallMode.BLOCK_UNKNOWN,
                        onClick = {
                            coroutineScope.launch {
                                preferences.setFirewallMode(FirewallMode.BLOCK_UNKNOWN)
                            }
                        }
                    )

                    ModeRow(
                        title = "فقط لیست سیاه (Blacklist)",
                        subtitle = "همه زنگ می‌خورند به جز شماره‌های مسدودشده",
                        isSelected = currentMode == FirewallMode.BLOCK_BLACKLIST,
                        onClick = {
                            coroutineScope.launch {
                                preferences.setFirewallMode(FirewallMode.BLOCK_BLACKLIST)
                            }
                        }
                    )

                    ModeRow(
                        title = "سکوت مطلق (Block All)",
                        subtitle = "هیچ تماسی وصل نمی‌شود (حالت مطالعه/جلسه)",
                        isSelected = currentMode == FirewallMode.BLOCK_ALL,
                        onClick = {
                            coroutineScope.launch {
                                preferences.setFirewallMode(FirewallMode.BLOCK_ALL)
                            }
                        }
                    )

                    ModeRow(
                        title = "عبور آزاد (Allow All)",
                        subtitle = "فیلتر اعمال نشود و همه تماس‌ها متصل شوند",
                        isSelected = currentMode == FirewallMode.ALLOW_ALL,
                        onClick = {
                            coroutineScope.launch {
                                preferences.setFirewallMode(FirewallMode.ALLOW_ALL)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extra Toggles
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مسدودسازی شماره‌های مخفی/Private",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "تماس‌هایی که شماره آن‌ها Unknown یا No Caller ID است",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = blockPrivate,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    preferences.setBlockPrivateNumbers(checked)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ModeRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
