package com.aistudio.apexrivals.tr8x

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val RacingRed = Color(0xFFE50924)
private val DarkBackground = Color(0xFF080D18)
private val RoadColor = Color(0xFF30343D)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = RacingRed,
                    background = DarkBackground
                )
            ) {
                ApexRivalsGame()
            }
        }
    }
}

@Composable
fun ApexRivalsGame() {

    var speed by remember { mutableFloatStateOf(0f) }
    var distance by remember { mutableFloatStateOf(0f) }
    var playerX by remember { mutableFloatStateOf(0.5f) }
    var roadOffset by remember { mutableFloatStateOf(0f) }
    var paused by remember { mutableStateOf(false) }

    // Prototype game loop.
    // Replace with your engine's update loop if one already exists.
    LaunchedEffect(paused) {
        var previousFrame = 0L

        while (!paused) {
            withFrameNanos { frameTime ->
                if (previousFrame != 0L) {
                    val dt = (
                        (frameTime - previousFrame) / 1_000_000_000f
                    ).coerceIn(0f, 0.05f)

                    // Natural deceleration
                    speed = (speed - 7f * dt).coerceAtLeast(0f)

                    // Speed is represented in km/h.
                    distance += (speed / 3.6f) * dt

                    roadOffset = (roadOffset + speed * dt * 0.012f) % 1f
                }

                previousFrame = frameTime
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(12.dp)
    ) {

        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "APEX RIVALS",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )

            Button(
                onClick = { paused = !paused },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RacingRed
                )
            ) {
                Text(if (paused) "RESUME" else "PAUSE")
            }
        }

        Spacer(Modifier.height(12.dp))

        // RACE INFORMATION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column {
                Text("SPEED", color = Color.LightGray)

                Text(
                    "${speed.roundToInt()} KM/H",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("DISTANCE", color = Color.LightGray)

                Text(
                    "${distance.roundToInt()} M",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // RACING ENVIRONMENT
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF17291D)),
            contentAlignment = Alignment.Center
        ) {

            Canvas(modifier = Modifier.fillMaxSize()) {

                val w = size.width
                val h = size.height

                val roadLeft = w * 0.12f
                val roadWidth = w * 0.76f

                // Grass background
                drawRect(Color(0xFF254A30))

                // Track
                drawRect(
                    color = RoadColor,
                    topLeft = Offset(roadLeft, 0f),
                    size = Size(roadWidth, h)
                )

                // Track edges
                drawRect(
                    color = Color.White,
                    topLeft = Offset(roadLeft, 0f),
                    size = Size(6f, h)
                )

                drawRect(
                    color = Color.White,
                    topLeft = Offset(roadLeft + roadWidth - 6f, 0f),
                    size = Size(6f, h)
                )

                // Animated lane markings
                val dashLength = 48f
                val gap = 55f
                val cycle = dashLength + gap

                for (lane in 1..2) {

                    val laneX = roadLeft + roadWidth * lane / 3f

                    var y = roadOffset * cycle - cycle

                    while (y < h) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.8f),
                            topLeft = Offset(laneX, y),
                            size = Size(5f, dashLength)
                        )
                        y += cycle
                    }
                }

                // Player vehicle
                val carWidth = roadWidth * 0.19f
                val carHeight = carWidth * 1.75f

                val availableWidth = roadWidth - carWidth - 20f

                val carX = roadLeft + 10f + availableWidth * playerX
                val carY = h * 0.72f

                // Car shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(carX + 6f, carY + 9f),
                    size = Size(carWidth, carHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
                )

                // Vehicle body
                drawRoundRect(
                    color = RacingRed,
                    topLeft = Offset(carX, carY),
                    size = Size(carWidth, carHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
                )

                // Windshield
                drawRoundRect(
                    color = Color(0xFF79BBD6),
                    topLeft = Offset(
                        carX + carWidth * 0.15f,
                        carY + carHeight * 0.2f
                    ),
                    size = Size(
                        carWidth * 0.7f,
                        carHeight * 0.22f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f)
                )

                // Racing stripe
                drawRect(
                    color = Color.White,
                    topLeft = Offset(
                        carX + carWidth * 0.47f,
                        carY
                    ),
                    size = Size(carWidth * 0.06f, carHeight)
                )

                // Rear lights
                drawRect(
                    color = Color(0xFFFFC400),
                    topLeft = Offset(carX + 5f, carY + carHeight - 10f),
                    size = Size(carWidth * 0.2f, 5f)
                )

                drawRect(
                    color = Color(0xFFFFC400),
                    topLeft = Offset(
                        carX + carWidth * 0.75f,
                        carY + carHeight - 10f
                    ),
                    size = Size(carWidth * 0.2f, 5f)
                )
            }

            if (paused) {
                Text(
                    "GAME PAUSED",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // STEERING CONTROLS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                modifier = Modifier.weight(1f),
                enabled = !paused,
                onClick = {
                    playerX = (playerX - 0.09f).coerceIn(0f, 1f)
                }
            ) {
                Text("◀ LEFT")
            }

            Button(
                modifier = Modifier.weight(1f),
                enabled = !paused,
                onClick = {
                    playerX = (playerX + 0.09f).coerceIn(0f, 1f)
                }
            ) {
                Text("RIGHT ▶")
            }
        }

        Spacer(Modifier.height(8.dp))

        // ACCELERATION / BRAKING
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                modifier = Modifier.weight(1f),
                enabled = !paused,
                onClick = {
                    speed = (speed - 25f).coerceAtLeast(0f)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF555555)
                )
            ) {
                Text("BRAKE")
            }

            Button(
                modifier = Modifier.weight(1f),
                enabled = !paused,
                onClick = {
                    speed = (speed + 15f).coerceAtMost(300f)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RacingRed
                )
            ) {
                Text("ACCELERATE")
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                speed = 0f
                distance = 0f
                playerX = 0.5f
                roadOffset = 0f
                paused = false
            }
        ) {
            Text("RESTART RACE")
        }
    }
}
