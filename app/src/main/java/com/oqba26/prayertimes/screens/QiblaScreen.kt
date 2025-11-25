@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oqba26.prayertimes.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.oqba26.prayertimes.utils.QiblaUtils
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// رنگ‌های هدر، هماهنگ با بقیه صفحات
private val QTopBarLightColor = Color(0xFF0E7490)
private val QTopBarDarkColor = Color(0xFF4F378B)
private val QOnTopBarLightColor = Color.White
private val QOnTopBarDarkColor = Color(0xFFEADDFF)

/**
 * صفحه قبله‌نما
 */
@Composable
fun QiblaScreen(
    isDarkThemeActive: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // وضعیت مجوز مکان به‌صورت state واقعی
    var hasLocationPermission by remember { mutableStateOf(hasLocation(context)) }
    var isLocationOn by remember { mutableStateOf(isLocationServiceEnabled(context)) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var qiblaBearing by remember { mutableStateOf<Float?>(null) }
    var azimuth by remember { mutableFloatStateOf(0f) } // زاویه گوشی نسبت به شمال (درجه)

    // لانچر درخواست مجوز مکان
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // اگر هر کدام از مجوزها داده شد، true
        hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                hasLocation(context)
    }

    // فقط یک بار در شروع، وضعیت مکان را چک کن
    LaunchedEffect(Unit) {
        isLocationOn = isLocationServiceEnabled(context)
    }

    // هر چند ثانیه، وضعیت روشن بودن مکان را به‌روزرسانی کن
    LaunchedEffect(Unit) {
        while (true) {
            isLocationOn = isLocationServiceEnabled(context)
            delay(5_000)
        }
    }

    // گرفتن آخرین موقعیت و محاسبه زاویه قبله (فقط اگر مجوز و مکان فعال باشد)
    LaunchedEffect(hasLocationPermission, isLocationOn) {
        if (hasLocationPermission && isLocationOn) {
            getCurrentLocation(context) { location ->
                userLocation = location
                location?.let { loc ->
                    qiblaBearing = QiblaUtils.bearingToQibla(loc.latitude, loc.longitude)
                }
            }
        } else {
            qiblaBearing = null
        }
    }

    // سنسور جهت: Rotation Vector یا Accelerometer+MagneticField به عنوان fallback
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    DisposableEffect(Unit) {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationSensor == null && (accelSensor == null || magSensor == null)) {
            // هیچ ترکیب مفیدی موجود نیست
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                private val rotationMatrix = FloatArray(9)
                private val orientation = FloatArray(3)
                private val accelValues = FloatArray(3)
                private val magValues = FloatArray(3)
                private var hasAccel = false
                private var hasMag = false

                override fun onSensorChanged(event: android.hardware.SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            val azimuthRad = orientation[0].toDouble()
                            val azimuthDeg = (Math.toDegrees(azimuthRad) + 360.0) % 360.0
                            azimuth = azimuthDeg.toFloat()
                        }

                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(event.values, 0, accelValues, 0, accelValues.size)
                            hasAccel = true
                            if (hasMag) {
                                if (SensorManager.getRotationMatrix(
                                        rotationMatrix,
                                        null,
                                        accelValues,
                                        magValues
                                    )
                                ) {
                                    SensorManager.getOrientation(rotationMatrix, orientation)
                                    val azimuthRad = orientation[0].toDouble()
                                    val azimuthDeg = (Math.toDegrees(azimuthRad) + 360.0) % 360.0
                                    azimuth = azimuthDeg.toFloat()
                                }
                            }
                        }

                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, magValues, 0, magValues.size)
                            hasMag = true
                            if (hasAccel) {
                                if (SensorManager.getRotationMatrix(
                                        rotationMatrix,
                                        null,
                                        accelValues,
                                        magValues
                                    )
                                ) {
                                    SensorManager.getOrientation(rotationMatrix, orientation)
                                    val azimuthRad = orientation[0].toDouble()
                                    val azimuthDeg = (Math.toDegrees(azimuthRad) + 360.0) % 360.0
                                    azimuth = azimuthDeg.toFloat()
                                }
                            }
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            if (rotationSensor != null) {
                sensorManager.registerListener(
                    listener,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
            } else {
                sensorManager.registerListener(
                    listener,
                    accelSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
                sensorManager.registerListener(
                    listener,
                    magSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    Scaffold(
        topBar = {
            val bg = if (isDarkThemeActive) QTopBarDarkColor else QTopBarLightColor
            val fg = if (isDarkThemeActive) QOnTopBarDarkColor else QOnTopBarLightColor

            TopAppBar(
                title = { Text("قبله‌نما", color = fg) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = fg
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bg,
                    titleContentColor = fg,
                    navigationIconContentColor = fg
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val bearing = qiblaBearing

                // زاویهٔ شمال نسبت به بالای صفحه (فقط بر اساس چرخش گوشی)
                val northAngle = ((-azimuth + 360f) % 360f)

                // زاویهٔ قبله نسبت به بالای صفحه (اگر موقعیت در دسترس باشد)
                val qiblaAngle = if (bearing != null) {
                    ((bearing - azimuth + 360f) % 360f)
                } else null

                CompassView(
                    northAngle = northAngle,
                    qiblaAngle = qiblaAngle,
                    modifier = Modifier.size(260.dp)
                )

                Text(
                    text = when {
                        !hasLocationPermission ->
                            "برای نمایش قبله، باید به برنامه مجوز دسترسی به مکان بدهید."

                        !isLocationOn ->
                            "خدمات مکان (GPS یا شبکه) روی دستگاه خاموش است. برای تعیین قبله، مکان را فعال کنید."

                        userLocation == null ->
                            "در حال دریافت موقعیت..."

                        qiblaBearing == null ->
                            "زاویه قبله در دسترس نیست."

                        else -> {
                            val angle = String.format(
                                Locale.getDefault(),
                                "%.0f",
                                qiblaAngle ?: 0f
                            )
                            "سمت قبله حدوداً $angle درجه از شمال عقربه‌نماست."
                        }
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // دکمه درخواست مجوز مکان (فقط وقتی مجوز نداریم)
                if (!hasLocationPermission) {
                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("اعطای مجوز مکان")
                    }
                }

                // دکمه رفتن به تنظیمات مکان وقتی GPS/Network خاموش است
                if (hasLocationPermission && !isLocationOn) {
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("فعال‌سازی مکان")
                    }
                }

                if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Text(
                        text = "گوشی را به‌آرامی به صورت عدد ۸ انگلیسی حرکت دهید تا قطب‌نما کالیبره شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
}

private fun hasLocation(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        CancellationTokenSource().token
    ).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            onResult(task.result)
        } else {
            onResult(null)
        }
    }
}

private fun isLocationServiceEnabled(context: Context): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

@Composable
private fun CompassView(
    northAngle: Float,          // زاویه شمال نسبت به بالای صفحه
    qiblaAngle: Float?,         // زاویه قبله نسبت به بالای صفحه (اختیاری)
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sizePx = min(size.width, size.height)
        val radius = sizePx / 2f * 0.85f
        val center = Offset(size.width / 2f, size.height / 2f)

        // دایره اصلی
        drawCircle(
            color = Color(0xFF0D47A1),
            center = center,
            radius = radius,
            style = Stroke(width = 6f)
        )

        // فلش شمال (آبی) - همیشه با چرخاندن گوشی حرکت می‌کند
        run {
            val angleRad = Math.toRadians((northAngle - 90f).toDouble())
            val end = Offset(
                x = center.x + (cos(angleRad) * radius).toFloat(),
                y = center.y + (sin(angleRad) * radius).toFloat()
            )
            drawLine(
                color = Color(0xFF1976D2),
                start = center,
                end = end,
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFF1976D2),
                radius = 10f,
                center = end
            )
        }

        // فلش قبله (قرمز) - اگر موقعیت و قبله در دسترس باشد
        if (qiblaAngle != null) {
            val angleRad = Math.toRadians((qiblaAngle - 90f).toDouble())
            val end = Offset(
                x = center.x + (cos(angleRad) * radius).toFloat(),
                y = center.y + (sin(angleRad) * radius).toFloat()
            )
            drawLine(
                color = Color(0xFFD32F2F),
                start = center,
                end = end,
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFFD32F2F),
                radius = 12f,
                center = end
            )
        }
    }
}
