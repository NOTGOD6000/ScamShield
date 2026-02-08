package com.scamshield.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.scamshield.app.ui.theme.ScamShieldTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScamShieldTheme {
                ScamShieldUI()
            }
        }


    }
}

@Composable
fun ScamShieldUI() {

    val context = LocalContext.current
    val detector = remember { ScamDetector() }

    var message by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("No scan yet") }
    var resultColor by remember { mutableStateOf(Color.Black) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // 🔔 Notification permission (Android 13+)
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1120),
                        Color(0xFF0F172A)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    )
    {
        Column {
            Text(
                text = "ScamShield",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Threat Analysis Engine",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }


        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)

            ) {
                Text("Permissions", style = MaterialTheme.typography.titleMedium)

                // 🔔 Open Notification Access Settings
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)

                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Notification Access")
                }

                // 🔔 Request POST_NOTIFICATIONS (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        onClick = {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Notifications Permission")
                    }
                }

            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text("Quick Scan", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Paste suspicious message") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { imageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan Image (OCR)")
                }

                imageUri?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text("Scan Result", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(8.dp))

                val animatedColor by animateColorAsState(targetValue = resultColor, label = "")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(animatedColor)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = resultColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = resultText,
                        color = resultColor,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

}


fun formatResult(result: ScanResult): String {
    return buildString {
        append("${result.label}\n")
        append("Score: ${result.score}\n\n")
        result.reasons.forEach {
            append("• $it\n")
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
