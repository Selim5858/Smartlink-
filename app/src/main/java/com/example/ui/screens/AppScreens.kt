package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LinkButton
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationHost(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeAdButton by viewModel.activeAdButton.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val pendingRedirectUrl by viewModel.pendingRedirectUrl.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Error list toasts / general message handling
    LaunchedEffect(userMessage) {
        userMessage?.let {
            // Display alert/message beautifully - handled in UI overlay or standard snackbar
        }
    }

    // Launch external browser when target link is resolved
    LaunchedEffect(pendingRedirectUrl) {
        pendingRedirectUrl?.let { url ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                // If it fails, fallback onto search
                try {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$url"))
                    context.startActivity(fallbackIntent)
                } catch (ex: Exception) {}
            } finally {
                viewModel.clearRedirectUrl()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            MainViewModel.Screen.MainDashboard -> MainDashboardScreen(viewModel)
            MainViewModel.Screen.AdminCenter -> AdminCenterScreen(viewModel)
            MainViewModel.Screen.AuthPage -> AuthPageScreen(viewModel)
        }

        // Animated Popup Alert for Toast information
        AnimatedVisibility(
            visible = userMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable { viewModel.clearUserMessage() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Bilgi",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = userMessage ?: "",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearUserMessage() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Active Pop-up Advertisement Dialog
        activeAdButton?.let { button ->
            AdPopupDialog(
                button = button,
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(viewModel: MainViewModel) {
    val buttons by viewModel.buttons.collectAsStateWithLifecycle()
    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
    var showAdminPinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LinkGecis Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Popup Reklam & Hedef Link Geçidi",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    if (loggedInUser != null) {
                        Text(
                            text = "👤 ${loggedInUser!!.username}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Çıkış Yap", tint = Color.Red)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.navigateTo(MainViewModel.Screen.AuthPage) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Kayıt Ol / Giriş", fontSize = 12.sp, color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Info Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Nedir",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Nasıl Çalışır?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Aşağıdaki geçiş butonlarından birine tıklayın. Açılan sponsor popup reklamındaki sayacın dolmasını bekleyin ve daha sonra hedef bağlantıya ulaşın.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Aktif Geçiş Butonları",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (buttons.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Boş Liste",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Eklenmiş Buton Yok",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Yönetici henüz bir geçiş butonu eklemedi. Lütfen admin panelinden buton yükleyin.",
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(buttons) { button ->
                            GradientButtonCard(
                                button = button,
                                onClick = { viewModel.clickActionButton(button) }
                            )
                        }
                    }
                }

                // Footer Area with Admin Login
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showAdminPinDialog = true },
                        modifier = Modifier.testTag("admin_gate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Yönetici Girişi",
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Yönetici Kontrol Paneli Girişi",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showAdminPinDialog) {
        AdminPinDialog(
            onDismiss = { showAdminPinDialog = false },
            onConfirm = { pinCode ->
                showAdminPinDialog = false
                val approved = viewModel.authenticateAdmin(pinCode)
                if (approved) {
                    viewModel.navigateTo(MainViewModel.Screen.AdminCenter)
                }
            }
        )
    }
}

@Composable
fun GradientButtonCard(button: LinkButton, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("action_button_${button.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Geçiş Yap",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = button.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "İstatistik",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sayaç: ${button.countdownSeconds} saniye  •  Toplam Geçiş: ${button.clickCount}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Aç",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AdminPinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pinValue by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Güvenlik",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Yönetici Şifresi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lütfen admin panelini yönetmek için şifreyi giriniz. (Varsayılan: 1234)",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pinValue,
                    onValueChange = { if (it.length <= 8) pinValue = it },
                    label = { Text("Şifre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Close else Icons.Default.List
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, contentDescription = "Şifreyi Göster")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_pin_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İptal")
                    }
                    Button(
                        onClick = { onConfirm(pinValue) },
                        modifier = Modifier.weight(1f).testTag("admin_pin_submit")
                    ) {
                        Text("Onayla")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AdPopupDialog(
    button: LinkButton,
    viewModel: MainViewModel
) {
    val countdown by viewModel.countdown.collectAsStateWithLifecycle()
    val isAdSkippable by viewModel.isAdSkippable.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Infinite animation for flashing visual highlight
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash_alpha"
    )

    Dialog(onDismissRequest = {
        // Prevent dismissal unless skippable or manually canceled
    }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(
                width = 2.dp,
                color = if (isAdSkippable) {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = borderAlpha)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)
                }
            ),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("ad_popup_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SPONSORLU TANITIM / REKLAM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simulated ad icon based on seed keywords
                val adIcon = when (button.popupImageUrl) {
                    "watch" -> Icons.Default.Settings
                    "ticket" -> Icons.Default.Build
                    "rocket" -> Icons.Default.PlayArrow
                    else -> Icons.Default.Star
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = adIcon,
                        contentDescription = "Reklam Görseli",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ad Title
                Text(
                    text = button.popupTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ad Message Content
                Text(
                    text = button.popupMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Sponsor link button (makes it a realistic flow)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + button.popupTitle))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Sponsor Butonu",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fırsatı Web Sitesinde İncele",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Countdown Timer Progress Circle
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isAdSkippable) {
                        CircularProgressIndicator(
                            progress = { countdown.toFloat() / button.countdownSeconds.toFloat() },
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "$countdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Hazır",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Canceled/Go Back
                    OutlinedButton(
                        onClick = { viewModel.closeAdPopup(proceedToTarget = false) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kapat")
                    }

                    // Skip Ad and Proceed (Disabled during countdown)
                    Button(
                        onClick = { viewModel.closeAdPopup(proceedToTarget = true) },
                        enabled = isAdSkippable,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("skip_ad_button")
                    ) {
                        Text(
                            text = if (isAdSkippable) "BAĞLANTIYA GİT ✔️" else "REKLAMI GEÇ ($countdown)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isAdSkippable) Color.White else Color.Gray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCenterScreen(viewModel: MainViewModel) {
    val buttons by viewModel.buttons.collectAsStateWithLifecycle()
    val registerCount by viewModel.registeredUsersCount.collectAsStateWithLifecycle()

    var label by remember { mutableStateOf("") }
    var targetUrl by remember { mutableStateOf("") }
    var popupTitle by remember { mutableStateOf("") }
    var popupMessage by remember { mutableStateOf("") }
    var countdownStr by remember { mutableStateOf("5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yönetici Paneli Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(MainViewModel.Screen.MainDashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deauthenticateAdmin()
                        viewModel.navigateTo(MainViewModel.Screen.MainDashboard)
                    }) {
                        Icon(Icons.Default.Lock, contentDescription = "Çıkış Yap", tint = Color.Red)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats block
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Toplam Buton", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Text("${buttons.size}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Kayıtlı Kullanıcılar", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Text("$registerCount", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            // Create New Button form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Yeni Geçiş Butonu Oluştur",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Buton Metni (Örn: Premium Film Sunucusu)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_label")
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = targetUrl,
                            onValueChange = { targetUrl = it },
                            label = { Text("Hedef Link / URL (Örn: www.movie.com)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_target_url")
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = popupTitle,
                            onValueChange = { popupTitle = it },
                            label = { Text("Popup Reklam Başlığı") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_popup_title")
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = popupMessage,
                            onValueChange = { popupMessage = it },
                            label = { Text("Popup Reklam İçeriği / Açıklaması") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("input_popup_msg")
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = countdownStr,
                            onValueChange = { countdownStr = it },
                            label = { Text("Sayaç Bekleme Süresi (Saniye)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("input_countdown")
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val seconds = countdownStr.toIntOrNull() ?: 5
                                viewModel.addButton(
                                    label = label,
                                    targetUrl = targetUrl,
                                    popupTitle = popupTitle,
                                    popupMessage = popupMessage,
                                    countdownSeconds = seconds
                                )
                                // Clear inputs on submit
                                label = ""
                                targetUrl = ""
                                popupTitle = ""
                                popupMessage = ""
                                countdownStr = "5"
                            },
                            modifier = Modifier.fillMaxWidth().testTag("submit_new_button")
                        ) {
                            Text("Yeni Butonu Ekle ve Kaydet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Button List Manager
            item {
                Text(
                    text = "Mevcut Butonları Yönet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (buttons.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "Yönetilecek aktif buton bulunmuyor.",
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(buttons) { button ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(button.label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Hedef: ${button.targetUrl}", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Popup: ${button.popupTitle} (${button.countdownSeconds} sn)", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { viewModel.deleteButton(button.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthPageScreen(viewModel: MainViewModel) {
    var isRegisterState by remember { mutableStateOf(true) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRegisterState) "Hesap Oluştur" else "Kullanıcı Girişi") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(MainViewModel.Screen.MainDashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large visual Icon
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profil",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switch
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row {
                        Tab(
                            selected = isRegisterState,
                            onClick = { isRegisterState = true },
                            text = { Text("Kayıt Ol", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                        Tab(
                            selected = !isRegisterState,
                            onClick = { isRegisterState = false },
                            text = { Text("Giriş Yap", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Kullanıcı Adı") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_username")
                        )

                        if (isRegisterState) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("E-Posta Adresi") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth().testTag("auth_email")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Şifre") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("auth_password")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (isRegisterState) {
                                    viewModel.registerNewUser(username, email, password)
                                } else {
                                    viewModel.loginUser(username, password)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("auth_submit")
                        ) {
                            Text(if (isRegisterState) "Kayıt Ol ve Tamamla" else "Güvenli Giriş Yap", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Guest shortcut button!
                TextButton(
                    onClick = { viewModel.navigateTo(MainViewModel.Screen.MainDashboard) },
                    modifier = Modifier.testTag("auth_guest_bypass")
                ) {
                    Text(
                        text = "Kaydolmadan Misafir Olarak Devam Et ➔",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
