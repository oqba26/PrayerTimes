@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.oqba26.prayertimes.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.oqba26.prayertimes.utils.QiblaUtils
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin


private val QTopBarLightColor = Color(0xFF0E7490)
private val QTopBarDarkColor = Color(0xFF4F378B)
private val QOnTopBarLightColor = Color.White
private val QOnTopBarDarkColor = Color(0xFFEADDFF)


@Composable
fun QiblaScreen(
    isDarkThemeActive: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var hasLocationPermission by remember { mutableStateOf(hasLocation(context)) }
    var isLocationOn by remember { mutableStateOf(isLocationServiceEnabled(context)) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var qiblaBearing by remember { mutableStateOf<Float?>(null) }
    var azimuth by remember { mutableFloatStateOf(0f) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                hasLocation(context)
    }

    LaunchedEffect(Unit) {
        isLocationOn = isLocationServiceEnabled(context)
    }

    LaunchedEffect(Unit) {
        while (true) {
            isLocationOn = isLocationServiceEnabled(context)
            delay(5_000)
        }
    }

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

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    DisposableEffect(Unit) {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)
            private val accelValues = FloatArray(3)
            private val magValues = FloatArray(3)
            private var hasAccel = false
            private var hasMag = false

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val azimuthRad = orientation[0].toDouble()
                        azimuth = ((Math.toDegrees(azimuthRad) + 360) % 360).toFloat()
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accelValues, 0, accelValues.size)
                        hasAccel = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magValues, 0, magValues.size)
                        hasMag = true
                    }
                }

                if (rotationSensor == null && hasAccel && hasMag) {
                    val rotationMatrix = FloatArray(9)
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, accelValues, magValues)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val azimuthRad = orientation[0].toDouble()
                        azimuth = ((Math.toDegrees(azimuthRad) + 360) % 360).toFloat()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val usedSensor = rotationSensor ?: accelSensor
        val samplingPeriod = SensorManager.SENSOR_DELAY_UI

        sensorManager.registerListener(listener, usedSensor, samplingPeriod)
        if (rotationSensor == null) {
            sensorManager.registerListener(listener, magSensor, samplingPeriod)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {

                CompassView(
                    azimuth = azimuth,
                    qiblaBearing = qiblaBearing,
                    modifier = Modifier.size(300.dp)
                )

                Text(
                    text = when {
                        !hasLocationPermission ->
                            "برای نمایش قبله، باید به برنامه مجوز دسترسی به مکان بدهید."

                        !isLocationOn ->
                            "خدمات مکان (GPS) روی دستگاه خاموش است. برای تعیین قبله، آن را فعال کنید."

                        userLocation == null ->
                            "در حال دریافت موقعیت مکانی..."

                        qiblaBearing == null ->
                            "زاویه قبله در دسترس نیست. لطفاً از اتصال به اینترنت مطمئن شوید."

                        else ->
                            "گوشی را بچرخانید تا نشانگر سبز قبله، زیر نشانگر آبی در بالای قطب‌نما قرار گیرد."
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("اعطای مجوز مکان", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                if (hasLocationPermission && !isLocationOn) {
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("فعال‌سازی مکان", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Text(
                        text = "برای افزایش دقت، گوشی را به صورت عدد ۸ انگلیسی در هوا حرکت دهید تا قطب‌نما کالیبره شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

private fun hasLocation(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == android.content.pm.PackageManager.PERMISSION_GRANTED || coarse == android.content.pm.PackageManager.PERMISSION_GRANTED
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
    return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

@Composable
private fun CompassView(
    azimuth: Float,
    qiblaBearing: Float?,
    modifier: Modifier = Modifier
) {
    val textPaint = remember {
        Paint().apply {
            color = Color.Black.toArgb()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val radius = size.minDimension / 2f * 0.9f
        val center = this.center

        // 1. Outer Ring
        drawCircle(
            color = Color(0xFF6D84FF),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.1f)
        )

        // 2. Inner background
        drawCircle(
            color = Color.White,
            radius = radius * 0.9f,
            center = center
        )
        drawCircle(
            color = Color.LightGray,
            radius = radius * 0.9f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )

        // 3. Rotating part
        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(-azimuth, center.x, center.y)

        // 3.1. Ticks and Numbers
        val majorTickLength = radius * 0.1f
        val minorTickLength = majorTickLength / 2

        for (i in 0 until 360 step 2) {
            val angleRad = Math.toRadians(i.toDouble())
            val isMajorTick = i % 30 == 0

            val tickLength = if (isMajorTick) majorTickLength else minorTickLength
            val start = Offset(
                x = (center.x + (radius * 0.9f - tickLength) * cos(angleRad)).toFloat(),
                y = (center.y + (radius * 0.9f - tickLength) * sin(angleRad)).toFloat()
            )
            val end = Offset(
                x = (center.x + radius * 0.9f * cos(angleRad)).toFloat(),
                y = (center.y + radius * 0.9f * sin(angleRad)).toFloat()
            )
            drawLine(Color.Black, start, end, strokeWidth = if (isMajorTick) 5f else 2f)

            if (i % 30 == 0 && i != 0) {
                textPaint.textSize = 14.sp.toPx()
                val textRadius = radius * 0.75f
                val textX = center.x + textRadius * cos(angleRad).toFloat()
                val textY = center.y + textRadius * sin(angleRad).toFloat()

                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.translate(textX, textY)
                drawContext.canvas.nativeCanvas.rotate(i.toFloat() + 90f)
                drawContext.canvas.nativeCanvas.drawText(i.toString(), 0f, 0f, textPaint)
                drawContext.canvas.nativeCanvas.restore()
            }
        }

        // 3.2. Cardinal Points (N, S, E, W)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 24.sp.toPx()
        val cardinalTextRadius = radius * 0.6f
        val cardinals = mapOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
        cardinals.forEach { (text, angle) ->
            val angleRad = Math.toRadians(angle.toDouble() - 90)
            val x = center.x + cardinalTextRadius * cos(angleRad).toFloat()
            val y = center.y + cardinalTextRadius * sin(angleRad).toFloat()
            textPaint.color = if (text == "N") Color.Red.toArgb() else Color.Black.toArgb()
            drawContext.canvas.nativeCanvas.drawText(text, x, y + textPaint.textSize / 3, textPaint)
        }

        // 3.3. Main North/South Needle
        val needlePath = Path().apply {
            moveTo(center.x, center.y - radius * 0.5f) // Top point (North)
            lineTo(center.x - radius * 0.1f, center.y) // Bottom-left
            lineTo(center.x + radius * 0.1f, center.y) // Bottom-right
            close()
        }

        drawPath(path = needlePath, color = Color(0xFFD32F2F)) // Red part

        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(180f, center.x, center.y)
        drawPath(path = needlePath, color = Color(0xFF4242FF)) // Blue part
        drawContext.canvas.nativeCanvas.restore()

        // 3.4. Qibla Indicator
        if (qiblaBearing != null) {
            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.rotate(qiblaBearing, center.x, center.y)
            val qiblaIndicatorPath = Path().apply {
                val qiblaY = center.y - radius * 0.85f
                moveTo(center.x - 15f, qiblaY - 15f)
                lineTo(center.x + 15f, qiblaY - 15f)
                lineTo(center.x, qiblaY)
                close()
            }
            drawPath(qiblaIndicatorPath, color = Color(0xFF4CAF50))
            drawContext.canvas.nativeCanvas.restore()
        }

        drawContext.canvas.nativeCanvas.restore() // Restore from the main rotation

        // 4. Central Circle
        drawCircle(color = Color.LightGray, radius = radius * 0.15f, center = center)
        drawCircle(color = Color.White, radius = radius * 0.12f, center = center)

        // 5. Static Top Indicator
        drawCircle(
            color = Color(0xFF4242FF),
            radius = radius * 0.05f,
            center = Offset(center.x, center.y - radius * 0.95f)
        )
    }
}
