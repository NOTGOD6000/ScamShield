package com.scamshield.app

import android.provider.Settings
import android.Manifest
import android.content.Intent
import androidx.compose.ui.graphics.StrokeCap
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.*
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.scamshield.app.ui.theme.ScamShieldTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScamShieldTheme {
                AppNavigation()
            }
        }
    }
}

/* ---------------- NAVIGATION ---------------- */

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(navController, startDestination = "splash") {

        composable("splash") { SplashScreen(navController) }
        composable("permission") { PermissionScreen(navController) }
        composable("dashboard") { DashboardScreen(navController) }
        composable("sms") { SmsScreen() }
        composable("manual") { ManualScanScreen() }
        composable("ocr") { OcrScanScreen() }
    }
}

/* ---------------- SPLASH ---------------- */

@Composable
fun SplashScreen(navController: NavController) {

    val context = LocalContext.current

    val isSmsGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) {
        delay(1800)

        if (isSmsGranted) {
            navController.navigate("dashboard") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("permission") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B1120),
                        Color(0xFF0F172A)
                    )
                )
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = "ScamShield",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Real-time Threat Protection",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            CircularProgressIndicator(
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Checking permissions...",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


/* ---------------- PERMISSION ---------------- */

@Composable
fun PermissionScreen(navController: NavController) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 🔁 Reactive states
    var isSmsGranted by remember { mutableStateOf(false) }
    var isNotificationPermissionGranted by remember { mutableStateOf(true) }
    var isNotificationListenerEnabled by remember { mutableStateOf(false) }

    // 🔄 Function to re-check all permissions
    fun checkPermissions() {

        isSmsGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED

        isNotificationPermissionGranted =
            if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true

        isNotificationListenerEnabled =
            Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )?.contains(context.packageName) == true
    }

    // Run once on launch
    LaunchedEffect(Unit) {
        checkPermissions()
    }

    // 🔁 Re-check when coming back from Settings screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission launchers
    val smsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            checkPermissions()
        }

    val notificationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            checkPermissions()
        }

    val allGranted =
        isSmsGranted &&
                isNotificationPermissionGranted &&
                isNotificationListenerEnabled

    // 🚀 Auto navigate when everything granted
    LaunchedEffect(allGranted) {
        if (allGranted) {
            navController.navigate("dashboard") {
                popUpTo("permission") { inclusive = true }
            }
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            "Permissions Setup",
            style = MaterialTheme.typography.headlineMedium
        )

        if (!isSmsGranted) {
            Button(
                onClick = {
                    smsLauncher.launch(Manifest.permission.READ_SMS)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow SMS Access")
            }
        }

        if (Build.VERSION.SDK_INT >= 33 && !isNotificationPermissionGranted) {
            Button(
                onClick = {
                    notificationLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow Notification Permission")
            }
        }

        if (!isNotificationListenerEnabled) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Live Notification Monitoring")
            }
        }

        if (allGranted) {
            Text(
                "All permissions granted ✔",
                color = Color(0xFF4CAF50)
            )
        }
    }
}


/* ---------------- DASHBOARD ---------------- */

@Composable
fun DashboardScreen(navController: NavController) {

    val context = LocalContext.current

    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val formatter =
                SimpleDateFormat("EEE • dd MMM yyyy • HH:mm:ss", Locale.getDefault())
            currentTime = formatter.format(Date())
            delay(1000)
        }
    }

    val isSmsGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

    val isNotificationListenerEnabled =
        Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )?.contains(context.packageName) == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1120), Color(0xFF0F172A))
                )
            )
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // ===== Professional Header =====
        Column {

            Text(
                text = "ScamShield",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Real-time Threat Protection",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = currentTime,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(Modifier.height(4.dp))


        // ===== Protection Status Card =====
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFF111827)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val protectionActive = isSmsGranted && isNotificationListenerEnabled

                val animatedColor by animateColorAsState(
                    targetValue = if (protectionActive)
                        Color(0xFF4CAF50)
                    else
                        Color.Red,
                    label = ""
                )

                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = animatedColor,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    if (protectionActive) "Full Protection Active"
                    else "Protection Incomplete",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(20.dp))

                HorizontalDivider(
                    color = Color(0xFF1F2937),
                    thickness = 1.dp
                )


                Spacer(Modifier.height(16.dp))

                StatusRow("SMS Access", isSmsGranted)
                Spacer(Modifier.height(8.dp))
                StatusRow(
                    title = "Live Notification Monitoring",
                    enabled = isNotificationListenerEnabled,
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }
        }



        Text(
            "Security Tools",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )

        // ===== Action Buttons =====

        DashboardButton(
            title = "SMS Protection",
            subtitle = "Scan and monitor all SMS messages",
            icon = Icons.Default.Message,
            onClick = { navController.navigate("sms") }
        )

        DashboardButton(
            title = "OCR Scanner",
            subtitle = "Scan suspicious screenshots",
            icon = Icons.Default.CameraAlt,
            onClick = { navController.navigate("ocr") }
        )

        DashboardButton(
            title = "Manual Scan",
            subtitle = "Paste and analyze text manually",
            icon = Icons.Default.Edit,
            onClick = { navController.navigate("manual") }
        )

    }
}
@Composable
fun DashboardButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF1E293B)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {

                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
@Composable
fun AppHeader() {

    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val formatter =
                SimpleDateFormat("EEE • dd MMM yyyy • HH:mm:ss", Locale.getDefault())
            currentTime = formatter.format(Date())
            delay(1000)
        }
    }

    Column {

        Text(
            text = "ScamShield",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Real-time Threat Protection",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = currentTime,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun StatusRow(
    title: String,
    enabled: Boolean,
    onClick: (() -> Unit)? = null
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .then(
                if (onClick != null)
                    Modifier.clickable { onClick() }
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = if (enabled) "Active" else "Disabled",
            color = if (enabled) Color(0xFF4CAF50) else Color.Red,
            maxLines = 1
        )
    }
}




/* ---------------- SMS SCREEN ---------------- */

@Composable
fun SmsScreen() {

    val context = LocalContext.current
    val detector = remember { ScamDetector() }

    var smsList by remember { mutableStateOf<List<SmsMessageData>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val scanner = SmsScanner(context)
            smsList = scanner.getAllSms() // ALL messages
        } catch (_: Exception) {
            smsList = emptyList()
        }
        loading = false
    }

    val analyzed = smsList.map {
        val result = detector.analyze(it.body, it.address)
        Triple(it, result.score, result)
    }

    val threatCount = analyzed.count { it.second >= 70 }
    val safeCount = analyzed.size - threatCount
    val riskPercent =
        if (analyzed.isNotEmpty())
            (threatCount * 100) / analyzed.size
        else 0



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1120), Color(0xFF0F172A))
                )
            )
            .statusBarsPadding()
            .padding(16.dp)
    ) {




        Text(
            "SMS Protection Center",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Spacer(Modifier.height(16.dp))
        // ======== Stats Card ========
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Column(Modifier.padding(16.dp)) {

                Text("Overview",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(8.dp))

                val riskProgress = (riskPercent / 100f).coerceIn(0f, 1f)

                val animatedRisk by animateFloatAsState(
                    targetValue = riskProgress,
                    label = ""
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(160.dp)) {

                        val strokeWidth = 22f

                        // 🔴 Risk portion
                        drawArc(
                            color = Color.Red,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedRisk,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 🟢 Safe portion (remaining)
                        drawArc(
                            color = Color(0xFF4CAF50),
                            startAngle = -90f + (360f * animatedRisk),
                            sweepAngle = 360f * (1f - animatedRisk),
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Text(
                            "$riskPercent%",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            "Risk Level",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }



                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total", color = Color.Gray)
                        Text("${analyzed.size}", color = Color.White)
                    }
                    Column {
                        Text("Threats", color = Color.Gray)
                        Text("$threatCount", color = Color.Red)
                    }
                    Column {
                        Text("Safe", color = Color.Gray)
                        Text("$safeCount", color = Color(0xFF4CAF50))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(analyzed) { (sms, score, result) ->

                    val formatter =
                        SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())

                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF1E293B)
                        )
                    ) {

                        Column(Modifier.padding(16.dp)) {

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    sms.address,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                val animatedColor by animateColorAsState(
                                    targetValue = getColor(score),
                                    label = ""
                                )

                                Text(
                                    "$score",
                                    color = animatedColor
                                )

                            }

                            Spacer(Modifier.height(4.dp))

                            Text(
                                formatter.format(Date(sms.date)),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                sms.body,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (score >= 70) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    result.label,
                                    color = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



/* ---------------- MANUAL + OCR SCREEN ---------------- */

@Composable
fun ManualScanScreen() {

    val detector = remember { ScamDetector() }

    var message by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("No scan yet") }
    var resultColor by remember { mutableStateOf(Color.Gray) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1120), Color(0xFF0F172A))
                )
            )
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        AppHeader()

        Spacer(Modifier.height(8.dp))

        Text(
            "Manual Threat Scanner",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )


        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Paste suspicious message") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val result = detector.analyze(message)
                resultText = formatResult(result)
                resultColor = getColor(result.score)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Text")
        }



        Text(resultText, color = resultColor)
    }
}

/* ---------------- HELPERS ---------------- */

fun formatResult(result: ScanResult): String {
    return buildString {
        append("${result.label}\n")
        append("Score: ${result.score}\n\n")
        result.reasons.forEach {
            append("• $it\n")
        }
    }
}
@Composable
fun OcrScanScreen() {

    val context = LocalContext.current
    val detector = remember { ScamDetector() }

    var resultText by remember { mutableStateOf("No scan yet") }
    var resultColor by remember { mutableStateOf(Color.Gray) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        uri?.let {
            val image = InputImage.fromFilePath(context, it)
            val recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val result = detector.analyze(visionText.text)
                    resultText = formatResult(result)
                    resultColor = getColor(result.score)
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1120), Color(0xFF0F172A))
                )
            )
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        AppHeader()

        Spacer(Modifier.height(8.dp))

        Text(
            "OCR Threat Scanner",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )


        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Column(Modifier.padding(16.dp)) {

                Button(
                    onClick = { imageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Image")
                }

                imageUri?.let {
                    Spacer(Modifier.height(12.dp))
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Text(
                resultText,
                color = resultColor,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

fun getColor(score: Int): Color {
    return when {
        score >= 100 -> Color.Red
        score >= 70 -> Color(0xFFFFA500)
        else -> Color(0xFF4CAF50)
    }
}



