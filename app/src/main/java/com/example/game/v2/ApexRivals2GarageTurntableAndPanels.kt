package com.example.game.v2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.sound.GameSoundManager
import com.example.model.Vehicle
import com.example.model.VehicleType
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RedlineRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * PROMPT 3: Interactive 3D Rotating Vehicle Garage Turntable & Upgrade Studio.
 * Allows 360° vehicle rotation, visual preview before applying Paint, Wheels, Spoilers, and Body Kits,
 * and performance upgrades for Engine, Turbo, Brakes, and Tires with live stat bars.
 */
@Composable
fun Garage3DTurntablePreviewCard(
  vehicle: Vehicle,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  LaunchedEffect(Unit) {
    ApexRivals2CareerManager.initialize(context)
    ApexRivals2SettingsRepository.initialize(context)
  }

  val careerState by ApexRivals2CareerManager.state.collectAsState()
  val savedLoadout = remember(vehicle.id, careerState.loadoutsByVehicleId) {
    ApexRivals2CareerManager.getLoadoutForVehicle(vehicle)
  }

  // Preview state (can be inspected before applying)
  var previewPaintHex by remember(vehicle.id, savedLoadout.paintColorHex) {
    mutableStateOf(savedLoadout.paintColorHex)
  }
  var previewWheel by remember(vehicle.id, savedLoadout.wheelRimStyle) {
    mutableStateOf(savedLoadout.wheelRimStyle)
  }
  var previewSpoiler by remember(vehicle.id, savedLoadout.spoilerStyle) {
    mutableStateOf(savedLoadout.spoilerStyle)
  }
  var previewBodyKit by remember(vehicle.id, savedLoadout.bodyKitSpec) {
    mutableStateOf(savedLoadout.bodyKitSpec)
  }

  var activeTab by remember { mutableIntStateOf(0) } // 0=Paint, 1=Wheels, 2=Spoiler, 3=BodyKit, 4=Upgrades
  var manualYawDeg by remember { mutableFloatStateOf(28f) }
  var isAutoRotate by remember { mutableStateOf(true) }
  var statusFeedback by remember { mutableStateOf<String?>(null) }

  val infiniteTransition = rememberInfiniteTransition(label = "turntableSpin")
  val spinOffsetDeg by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 14000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "spinOffsetDeg"
  )

  val effectiveYawDeg = if (isAutoRotate) (manualYawDeg + spinOffsetDeg) % 360f else manualYawDeg

  val previewLoadout = savedLoadout.copy(
    paintColorHex = previewPaintHex,
    wheelRimStyle = previewWheel,
    spoilerStyle = previewSpoiler,
    bodyKitSpec = previewBodyKit
  )
  val liveStats = remember(vehicle, previewLoadout) {
    ApexRivals2CareerManager.computeUpgradedStats(vehicle, previewLoadout)
  }

  val hasUnappliedVisualChanges = previewPaintHex != savedLoadout.paintColorHex ||
    previewWheel != savedLoadout.wheelRimStyle ||
    previewSpoiler != savedLoadout.spoilerStyle ||
    previewBodyKit != savedLoadout.bodyKitSpec

  Card(
    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.65f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("garage_3d_turntable_card")
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.DirectionsCarIconFallback(),
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = "APEX 2.0 • 3D SHOWROOM TURNTABLE & UPGRADE BAY",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = TextPrimary,
              letterSpacing = 0.6.sp
            )
            Text(
              text = "${vehicle.name} • Lv.${liveStats.totalUpgradeLevel}/${liveStats.maxUpgradeLevel} • CR ${careerState.creditsBalance}",
              fontSize = 9.sp,
              color = NeonAmber,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isAutoRotate) NeonCyan.copy(alpha = 0.2f) else CarbonBlack)
            .border(1.dp, if (isAutoRotate) NeonCyan else CarbonBorder, RoundedCornerShape(6.dp))
            .clickable { isAutoRotate = !isAutoRotate }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("turntable_autorotate_toggle")
        ) {
          Text(
            text = if (isAutoRotate) "360° SPIN: ON" else "360° SPIN: MANUAL",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = if (isAutoRotate) NeonCyan else TextSecondary,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // 3D Interactive Turntable Canvas (Drag horizontally to rotate 360°)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(175.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(
            Brush.verticalGradient(
              colors = listOf(Color(0xFF070A12), Color(0xFF121826), Color(0xFF090C14))
            )
          )
          .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
          .pointerInput(Unit) {
            detectHorizontalDragGestures { _, dragAmount ->
              isAutoRotate = false
              manualYawDeg = (manualYawDeg + dragAmount * 0.65f + 360f) % 360f
            }
          }
          .testTag("garage_3d_turntable_canvas")
      ) {
        Canvas(modifier = Modifier.matchParentSize()) {
          val w = size.width
          val h = size.height
          val cx = w * 0.5f
          val cy = h * 0.62f

          val yawRad = Math.toRadians(effectiveYawDeg.toDouble())
          val cosYaw = cos(yawRad).toFloat()
          val sinYaw = sin(yawRad).toFloat()

          // 1. Overhead Studio Softbox Lighting Cone
          drawRect(
            brush = Brush.radialGradient(
              colors = listOf(
                Color(previewPaintHex).copy(alpha = 0.22f),
                NeonCyan.copy(alpha = 0.08f),
                Color.Transparent
              ),
              center = Offset(cx, h * 0.25f),
              radius = w * 0.55f
            ),
            size = size
          )

          // 2. Carbon-Fiber Illuminated Turntable Platform & Floor Reflection
          val platformW = w * 0.72f
          val platformH = h * 0.34f
          drawOval(
            color = Color(0xFF05070B),
            topLeft = Offset(cx - platformW * 0.52f, cy - platformH * 0.45f + 8f),
            size = Size(platformW * 1.04f, platformH * 1.05f)
          )
          drawOval(
            brush = Brush.radialGradient(
              colors = listOf(
                Color(0xFF1E2638),
                Color(0xFF101520),
                Color(0xFF080B11)
              ),
              center = Offset(cx, cy),
              radius = platformW * 0.5f
            ),
            topLeft = Offset(cx - platformW * 0.5f, cy - platformH * 0.5f),
            size = Size(platformW, platformH)
          )
          // Outer Neon Cyan Turntable Ring
          drawOval(
            color = NeonCyan.copy(alpha = 0.75f),
            topLeft = Offset(cx - platformW * 0.5f, cy - platformH * 0.5f),
            size = Size(platformW, platformH),
            style = Stroke(width = 2.5f)
          )

          // 3. Projected 3D Vehicle or Motorbike Silhouette on Turntable
          val paintColor = Color(previewPaintHex)
          val rimColor = Color(previewWheel.rimColorHex)
          val bodyWidthScale = (1f + previewBodyKit.widthBonusPx / 100f)
          val carLength = w * 0.40f * bodyWidthScale
          val carHeight = if (vehicle.type == VehicleType.MOTORBIKE) h * 0.26f else h * 0.22f

          // Dynamic floor shadow & metallic reflection under chassis
          drawOval(
            color = Color.Black.copy(alpha = 0.72f),
            topLeft = Offset(cx - carLength * 0.48f, cy - platformH * 0.22f + 10f),
            size = Size(carLength * 0.96f, platformH * 0.44f)
          )
          drawOval(
            color = paintColor.copy(alpha = 0.18f),
            topLeft = Offset(cx - carLength * 0.42f, cy - platformH * 0.16f + 14f),
            size = Size(carLength * 0.84f, platformH * 0.32f)
          )

          // 3D Yaw perspective offset for nose and tail
          val noseX = cx + cosYaw * (carLength * 0.42f)
          val tailX = cx - cosYaw * (carLength * 0.42f)
          val sideDepth = sinYaw * (w * 0.08f)

          // Lower Carbon Splitter & Side Skirts (Body Kit visual)
          if (previewBodyKit != BodyKitSpec.FACTORY_SPEC) {
            drawRoundRect(
              color = Color(0xFF181C24),
              topLeft = Offset(
                minOf(noseX, tailX) - 24f - previewBodyKit.widthBonusPx,
                cy - 6f
              ),
              size = Size(
                kotlin.math.abs(noseX - tailX) + 48f + previewBodyKit.widthBonusPx * 2f,
                14f
              ),
              cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
              color = NeonAmber.copy(alpha = 0.85f),
              topLeft = Offset(
                minOf(noseX, tailX) - 24f - previewBodyKit.widthBonusPx,
                cy + 5f
              ),
              size = Size(
                kotlin.math.abs(noseX - tailX) + 48f + previewBodyKit.widthBonusPx * 2f,
                2.5f
              ),
              cornerRadius = CornerRadius(2f, 2f)
            )
          }

          // Main Sculpted 3D Chassis Body Path
          val bodyLeft = minOf(noseX, tailX) - 22f
          val bodyRight = maxOf(noseX, tailX) + 22f
          val bodyWidth = (bodyRight - bodyLeft).coerceAtLeast(70f)
          val chassisTopY = cy - carHeight

          val chassisPath = Path().apply {
            moveTo(bodyLeft, cy + 2f)
            lineTo(bodyLeft + bodyWidth * 0.08f, chassisTopY + carHeight * 0.42f)
            lineTo(bodyLeft + bodyWidth * 0.28f, chassisTopY)
            lineTo(bodyLeft + bodyWidth * 0.72f, chassisTopY)
            lineTo(bodyRight - bodyWidth * 0.06f, chassisTopY + carHeight * 0.45f)
            lineTo(bodyRight, cy + 2f)
            close()
          }

          drawPath(
            path = chassisPath,
            brush = Brush.verticalGradient(
              colors = listOf(
                paintColor.copy(alpha = 0.98f),
                paintColor.copy(
                  red = (paintColor.red * 0.72f).coerceIn(0f, 1f),
                  green = (paintColor.green * 0.72f).coerceIn(0f, 1f),
                  blue = (paintColor.blue * 0.72f).coerceIn(0f, 1f),
                  alpha = 1f
                )
              )
            )
          )

          // Specular Studio Highlight Ribbon across Roof & Hood
          val cabinPath = Path().apply {
            moveTo(bodyLeft + bodyWidth * 0.22f, chassisTopY + carHeight * 0.40f)
            lineTo(bodyLeft + bodyWidth * 0.33f, chassisTopY + 4f)
            lineTo(bodyLeft + bodyWidth * 0.67f, chassisTopY + 4f)
            lineTo(bodyLeft + bodyWidth * 0.78f, chassisTopY + carHeight * 0.40f)
            close()
          }
          drawPath(
            path = cabinPath,
            color = Color(0xFF0D131F).copy(alpha = 0.88f)
          )

          // Rear Aero Wing / Spoiler Visualization
          if (previewSpoiler != SpoilerWingStyle.FACTORY_LIP && vehicle.type == VehicleType.CAR) {
            val wingX = if (cosYaw >= 0f) bodyLeft + 10f else bodyRight - 28f
            val wingHeight = when (previewSpoiler) {
              SpoilerWingStyle.GT3_SWAN_NECK -> 18f
              SpoilerWingStyle.TIME_ATTACK_DUAL -> 24f
              SpoilerWingStyle.ACTIVE_DRS_CARBON -> 21f
              else -> 10f
            }
            // Endplates / Uprights
            drawLine(
              color = Color(0xFFCFD8DC),
              start = Offset(wingX + 4f, chassisTopY + 10f),
              end = Offset(wingX + 4f, chassisTopY - wingHeight),
              strokeWidth = 3f
            )
            drawLine(
              color = Color(0xFFCFD8DC),
              start = Offset(wingX + 16f, chassisTopY + 10f),
              end = Offset(wingX + 16f, chassisTopY - wingHeight),
              strokeWidth = 3f
            )
            // Main Carbon Wing Foil
            drawRoundRect(
              color = Color(0xFF151922),
              topLeft = Offset(wingX - 4f, chassisTopY - wingHeight - 5f),
              size = Size(28f, 6f),
              cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
              color = paintColor,
              topLeft = Offset(wingX - 4f, chassisTopY - wingHeight - 5f),
              size = Size(28f, 6f),
              cornerRadius = CornerRadius(3f, 3f),
              style = Stroke(width = 1.5f)
            )
          }

          // Front & Rear Wheels with Custom Rim Spokes
          val wheelRadius = 16f
          val frontWheelX = bodyLeft + bodyWidth * 0.22f + sideDepth * 0.15f
          val rearWheelX = bodyLeft + bodyWidth * 0.78f - sideDepth * 0.15f
          val wheelY = cy + 2f

          listOf(frontWheelX, rearWheelX).forEach { wx ->
            // Tire rubber
            drawCircle(
              color = Color(0xFF0B0D12),
              radius = wheelRadius,
              center = Offset(wx, wheelY)
            )
            // Glowing Carbon-Ceramic Brake Rotor
            drawCircle(
              color = Color(0xFFFF3D00).copy(alpha = 0.55f),
              radius = wheelRadius * 0.55f,
              center = Offset(wx, wheelY)
            )
            // Alloy Rim Ring
            drawCircle(
              color = rimColor,
              radius = wheelRadius * 0.72f,
              center = Offset(wx, wheelY),
              style = Stroke(width = 2.5f)
            )
            // Rim Spokes
            val spokes = previewWheel.spokeCount
            for (s in 0 until spokes) {
              val angleRad = Math.toRadians((s * (360.0 / spokes)) + effectiveYawDeg * 2.0)
              val sx = wx + cos(angleRad).toFloat() * (wheelRadius * 0.68f)
              val sy = wheelY + sin(angleRad).toFloat() * (wheelRadius * 0.68f)
              drawLine(
                color = rimColor,
                start = Offset(wx, wheelY),
                end = Offset(sx, sy),
                strokeWidth = 1.6f
              )
            }
          }

          // Headlight & Taillight LED Signatures
          val frontLedX = if (cosYaw >= 0f) bodyRight - 8f else bodyLeft + 4f
          val rearLedX = if (cosYaw >= 0f) bodyLeft + 4f else bodyRight - 8f
          drawCircle(
            color = Color(0xFFE0F7FA),
            radius = 4.5f,
            center = Offset(frontLedX, cy - 8f)
          )
          drawCircle(
            color = Color(0xFFFF1744),
            radius = 4.5f,
            center = Offset(rearLedX, cy - 10f)
          )
        }

        // Overlay HUD inside 3D Turntable
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopStart)
            .padding(8.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(CarbonBlack.copy(alpha = 0.78f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "YAW: ${effectiveYawDeg.toInt()}° • DRAG TO ROTATE",
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = NeonCyan,
              fontFamily = FontFamily.Monospace
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(CarbonBlack.copy(alpha = 0.78f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "VALUATION: CR ${liveStats.basePriceCredits}",
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = NeonAmber,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      // Live Vehicle Performance Telemetry Bars (Max Speed, Acceleration, Handling, Braking)
      Card(
        colors = CardDefaults.cardColors(containerColor = CarbonBlack),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CarbonBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("garage_live_stats_panel")
      ) {
        Column(
          modifier = Modifier.padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          GarageStatProgressRow(
            label = "MAX SPEED",
            valueText = "${liveStats.maxSpeedKmh} KM/H",
            progress = (liveStats.maxSpeedKmh / 410f).coerceIn(0.2f, 1f),
            barColor = NeonCyan
          )
          GarageStatProgressRow(
            label = "ACCELERATION (0-100)",
            valueText = String.format(Locale.US, "%.2fs", liveStats.acceleration0to100Sec),
            progress = ((4.5f - liveStats.acceleration0to100Sec) / 3.2f).coerceIn(0.2f, 1f),
            barColor = ApexGreen
          )
          GarageStatProgressRow(
            label = "HANDLING / APEX GRIP",
            valueText = String.format(Locale.US, "%.2f G", liveStats.handlingCorneringG),
            progress = (liveStats.handlingCorneringG / 4.2f).coerceIn(0.2f, 1f),
            barColor = NeonAmber
          )
          GarageStatProgressRow(
            label = "CARBON BRAKING",
            valueText = "${liveStats.brakingRating} / 100",
            progress = (liveStats.brakingRating / 100f).coerceIn(0.2f, 1f),
            barColor = RedlineRed
          )
        }
      }

      // Category Selector Tabs (Paint, Wheels, Spoilers, Body Kits, Upgrades)
      val tabs = listOf("PAINT", "WHEELS", "SPOILERS", "BODY KITS", "UPGRADES")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        tabs.forEachIndexed { idx, title ->
          val isSelected = activeTab == idx
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) NeonCyan else CarbonBlack)
              .border(1.dp, if (isSelected) NeonCyan else CarbonBorder, RoundedCornerShape(6.dp))
              .clickable { activeTab = idx }
              .padding(vertical = 6.dp)
              .testTag("garage_tab_$idx"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = title,
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = if (isSelected) CarbonBlack else TextPrimary
            )
          }
        }
      }

      // Tab Content
      when (activeTab) {
        0 -> {
          // PAINT PALETTE
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "SELECT MOTORSPORT LIVERY FINISH (LIVE 3D PREVIEW):",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = TextSecondary
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              GaragePaintOption.entries.forEach { paint ->
                val isSelected = previewPaintHex == paint.colorHex
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(paint.colorHex))
                    .border(
                      width = if (isSelected) 2.5.dp else 1.dp,
                      color = if (isSelected) Color.White else CarbonBorder,
                      shape = CircleShape
                    )
                    .clickable { previewPaintHex = paint.colorHex }
                    .testTag("garage_paint_${paint.name}")
                )
              }
            }
          }
        }

        1 -> {
          // WHEELS / RIMS
          Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            WheelRimStyle.entries.forEach { wheel ->
              val isSelected = previewWheel == wheel
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) CarbonSurfaceVariant else CarbonBlack)
                  .border(1.dp, if (isSelected) NeonCyan else CarbonBorder, RoundedCornerShape(8.dp))
                  .clickable { previewWheel = wheel }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
                  .testTag("garage_wheel_${wheel.name}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(wheel.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                  Text(
                    text = "${wheel.spokeCount}-Spoke • ${wheel.weightDeltaKg}kg • +${wheel.handlingBonusG}G Grip",
                    fontSize = 8.sp,
                    color = TextSecondary
                  )
                }
                Text(
                  text = if (wheel.priceCredits == 0) "INCLUDED" else "CR ${wheel.priceCredits}",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  color = NeonAmber,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }

        2 -> {
          // SPOILERS / WINGS
          Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            SpoilerWingStyle.entries.forEach { wing ->
              val isSelected = previewSpoiler == wing
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) CarbonSurfaceVariant else CarbonBlack)
                  .border(1.dp, if (isSelected) NeonCyan else CarbonBorder, RoundedCornerShape(8.dp))
                  .clickable { previewSpoiler = wing }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
                  .testTag("garage_spoiler_${wing.name}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(wing.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                  Text(
                    text = "+${wing.downforceBonusPct}% Downforce • +${wing.handlingBonusG}G Handling",
                    fontSize = 8.sp,
                    color = TextSecondary
                  )
                }
                Text(
                  text = if (wing.priceCredits == 0) "INCLUDED" else "CR ${wing.priceCredits}",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  color = NeonAmber,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }

        3 -> {
          // BODY KITS
          Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            BodyKitSpec.entries.forEach { kit ->
              val isSelected = previewBodyKit == kit
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) CarbonSurfaceVariant else CarbonBlack)
                  .border(1.dp, if (isSelected) NeonCyan else CarbonBorder, RoundedCornerShape(8.dp))
                  .clickable { previewBodyKit = kit }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
                  .testTag("garage_bodykit_${kit.name}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(kit.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                  Text(
                    text = "+${kit.topSpeedBonusKmh} km/h Aero • +${kit.handlingBonusG}G Cornering",
                    fontSize = 8.sp,
                    color = TextSecondary
                  )
                }
                Text(
                  text = if (kit.priceCredits == 0) "INCLUDED" else "CR ${kit.priceCredits}",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  color = NeonAmber,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }

        4 -> {
          // PERFORMANCE UPGRADES (Engine, Turbo, Brakes, Tires)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            UpgradeSubsystem.entries.forEach { sub ->
              val level = when (sub) {
                UpgradeSubsystem.ENGINE -> savedLoadout.engineLevel
                UpgradeSubsystem.TURBO -> savedLoadout.turboLevel
                UpgradeSubsystem.BRAKES -> savedLoadout.brakesLevel
                UpgradeSubsystem.TIRES -> savedLoadout.tiresLevel
              }
              val nextCost = sub.baseCostCredits * level
              val isMax = level >= 5

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(CarbonBlack)
                  .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${sub.displayName} (Lv.$level/5)",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                  Text(
                    text = if (isMax) "MAX STAGE 5 TUNED" else "Upgrade Cost: CR $nextCost",
                    fontSize = 8.sp,
                    color = if (isMax) ApexGreen else NeonAmber,
                    fontFamily = FontFamily.Monospace
                  )
                }

                Button(
                  onClick = {
                    val ok = ApexRivals2CareerManager.upgradeSubsystem(vehicle, sub, context)
                    if (ok) {
                      com.example.data.firebase.ApexCloudSyncCoordinator.onSubsystemUpgraded(
                        context = context,
                        vehicleId = vehicle.id,
                        subsystem = sub.name,
                        targetStage = level + 1
                      )
                    }
                    statusFeedback = if (ok) {
                      "Upgraded ${sub.displayName} to Stage ${level + 1}!"
                    } else {
                      "Insufficient Credits or Max Stage Reached"
                    }
                  },
                  enabled = !isMax && careerState.creditsBalance >= nextCost,
                  colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier
                    .height(30.dp)
                    .testTag("garage_upgrade_${sub.shortCode}")
                ) {
                  Text(
                    text = if (isMax) "MAX" else "UPGRADE",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = CarbonBlack
                  )
                }
              }
            }
          }
        }
      }

      // Apply Visual Customization Bar (Preview before Apply)
      if (hasUnappliedVisualChanges) {
        Button(
          onClick = {
            ApexRivals2CareerManager.applyVisualCustomization(
              vehicle = vehicle,
              paintColorHex = previewPaintHex,
              wheelRimStyle = previewWheel,
              spoilerStyle = previewSpoiler,
              bodyKitSpec = previewBodyKit,
              context = context
            )
            com.example.data.firebase.ApexCloudSyncCoordinator.onCustomizationSaved(context)
            statusFeedback = "Applied custom 3D Garage Loadout to ${vehicle.name}!"
          },
          colors = ButtonDefaults.buttonColors(containerColor = ApexGreen),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("garage_apply_customization_btn")
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CarbonBlack, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "APPLY PREVIEWED LOADOUT TO VEHICLE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = CarbonBlack
          )
        }
      }

      statusFeedback?.let { msg ->
        Text(
          text = msg,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = ApexGreen,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}

@Composable
private fun GarageStatProgressRow(
  label: String,
  valueText: String,
  progress: Float,
  barColor: Color
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
      Text(valueText, fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = barColor, fontFamily = FontFamily.Monospace)
    }
    LinearProgressIndicator(
      progress = { progress },
      color = barColor,
      trackColor = CarbonSurfaceVariant,
      modifier = Modifier
        .fillMaxWidth()
        .height(5.dp)
        .clip(RoundedCornerShape(3.dp))
    )
  }
}

@Composable
private fun Icons.Filled.DirectionsCarIconFallback() = Icons.Default.Speed

/**
 * PROMPT 5 & PROMPT 6: Career Championships, Daily Login Rewards, Weekly Challenges & Missions Panel.
 */
@Composable
fun ApexRivals2CareerAndRewardsPanel(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  LaunchedEffect(Unit) {
    ApexRivals2CareerManager.initialize(context)
  }
  val state by ApexRivals2CareerManager.state.collectAsState()
  var rewardBanner by remember { mutableStateOf<String?>(null) }

  Card(
    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.5.dp, NeonAmber.copy(alpha = 0.65f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("apex_v2_career_panel")
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header + Credits & Level Summary
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = "APEX RIVALS 2.0 • CAREER CHAMPIONSHIPS & REWARDS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = TextPrimary
            )
            Text(
              text = "Driver Lv.${state.playerLevel} (${state.totalXp} XP) • Balance: CR ${state.creditsBalance} • Total Earned: CR ${state.totalEarningsCredits}",
              fontSize = 8.5.sp,
              color = NeonCyan,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      // Daily Login Streak Banner
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(CarbonBlack)
          .border(1.dp, if (state.isDailyLoginClaimedToday) CarbonBorder else ApexGreen, RoundedCornerShape(8.dp))
          .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "DAILY LOGIN STREAK • DAY ${state.dailyLoginStreakDays}",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            color = ApexGreen
          )
          Text(
            text = if (state.isDailyLoginClaimedToday) {
              "Today's login bonus claimed! Come back tomorrow for Day ${state.dailyLoginStreakDays + 1}."
            } else {
              "Claim +${2500 + (state.dailyLoginStreakDays + 1) * 500} CR & +${400 + (state.dailyLoginStreakDays + 1) * 100} XP"
            },
            fontSize = 8.sp,
            color = TextSecondary
          )
        }

        Button(
          onClick = {
            if (ApexRivals2CareerManager.claimDailyLoginReward(context)) {
              com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, "daily_login")
              rewardBanner = "Daily Login Reward Claimed!"
            }
          },
          enabled = !state.isDailyLoginClaimedToday,
          colors = ButtonDefaults.buttonColors(containerColor = ApexGreen),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .height(32.dp)
            .testTag("claim_daily_login_btn")
        ) {
          Text(
            text = if (state.isDailyLoginClaimedToday) "CLAIMED" else "CLAIM DAILY",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            color = CarbonBlack
          )
        }
      }

      // 4 Championship Tiers Selector (Rookie, Amateur, Pro, Elite)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        ChampionshipTier.entries.forEach { tier ->
          val isSelected = state.activeChampionshipTier == tier
          val tierColor = Color(tier.badgeColorHex)
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) tierColor.copy(alpha = 0.22f) else CarbonBlack)
              .border(1.dp, if (isSelected) tierColor else CarbonBorder, RoundedCornerShape(6.dp))
              .clickable { ApexRivals2CareerManager.selectChampionshipTier(tier) }
              .padding(vertical = 6.dp, horizontal = 4.dp)
              .testTag("championship_tier_${tier.tierNumber}"),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "TIER ${tier.tierNumber}",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Black,
                color = tierColor,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = tier.displayName.substringBefore(" "),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }

      // Active Championship Events List with Idempotent Claim Protection
      val filteredEvents = ApexRivals2CareerManager.careerEvents.filter {
        it.championshipTier == state.activeChampionshipTier || it.category == CareerEventCategory.DAILY_CHALLENGE
      }

      filteredEvents.forEach { event ->
        val isClaimed = event.eventId in state.claimedEventIds
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CarbonBlack)
            .border(1.dp, if (isClaimed) ApexGreen.copy(alpha = 0.5f) else CarbonBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(NeonCyan.copy(alpha = 0.18f))
                  .padding(horizontal = 5.dp, vertical = 1.dp)
              ) {
                Text(
                  text = event.category.badgeText,
                  fontSize = 7.sp,
                  fontWeight = FontWeight.Black,
                  color = NeonCyan,
                  fontFamily = FontFamily.Monospace
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = event.title,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
            }
            Text(
              text = "${event.environmentName} • ${event.targetObjective}",
              fontSize = 8.sp,
              color = TextSecondary
            )
            Text(
              text = "+${event.rewardCredits} CR • +${event.rewardXp} XP • ${event.unlockRewardLabel}",
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = NeonAmber,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = {
              if (ApexRivals2CareerManager.claimCareerEventReward(event.eventId, context)) {
                com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, event.eventId)
                rewardBanner = "Completed ${event.title}: +${event.rewardCredits} CR!"
              }
            },
            enabled = !isClaimed,
            colors = ButtonDefaults.buttonColors(containerColor = if (isClaimed) CarbonSurfaceVariant else NeonCyan),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
              .height(30.dp)
              .testTag("claim_career_event_${event.eventId}")
          ) {
            Text(
              text = if (isClaimed) "COMPLETED" else "CLAIM WIN",
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = if (isClaimed) ApexGreen else CarbonBlack
            )
          }
        }
      }

      // Weekly Challenges & Racing Missions
      Text(
        text = "RACING MISSIONS & WEEKLY CHALLENGES:",
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        color = NeonCyan,
        letterSpacing = 0.5.sp
      )

      ApexRivals2CareerManager.rewardMissions.forEach { mission ->
        val isClaimed = mission.missionId in state.claimedMissionIds
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CarbonBlack)
            .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${mission.title} (${mission.missionType.replace('_', ' ')})",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "${mission.description} • +${mission.rewardCredits} CR / +${mission.rewardXp} XP",
              fontSize = 7.8.sp,
              color = TextSecondary
            )
          }
          OutlinedButton(
            onClick = {
              if (ApexRivals2CareerManager.claimMissionReward(mission.missionId, context)) {
                com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, mission.missionId)
                rewardBanner = "Mission '${mission.title}' Claimed!"
              }
            },
            enabled = !isClaimed,
            border = BorderStroke(1.dp, if (isClaimed) CarbonBorder else NeonAmber),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
              .height(28.dp)
              .testTag("claim_mission_${mission.missionId}")
          ) {
            Text(
              text = if (isClaimed) "CLAIMED" else "CLAIM",
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = if (isClaimed) TextTertiary else NeonAmber
            )
          }
        }
      }

      rewardBanner?.let { msg ->
        Text(
          text = msg,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = ApexGreen,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}

/**
 * PROMPT 1, PROMPT 2, PROMPT 8, PROMPT 9:
 * Graphics Quality Preset Selector, Control Mode & Sensitivity Tuner, Audio Mixer Sliders,
 * and Staging/Production Diagnostic Panel.
 */
@Composable
fun ApexRivals2ControlAndAudioSettingsCard(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val graphicsPreset by ApexRivals2SettingsRepository.graphicsPreset.collectAsState()
  val controlConfig by ApexRivals2SettingsRepository.controlConfig.collectAsState()
  val audioMixer by ApexRivals2SettingsRepository.audioMixer.collectAsState()
  val backendEnv by ApexRivals2SettingsRepository.backendEnv.collectAsState()
  val soundManager = remember { GameSoundManager.getInstance() }

  Card(
    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, CarbonBorder),
    modifier = modifier
      .fillMaxWidth()
      .testTag("apex_v2_settings_card")
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "APEX 2.0 ENGINE: GRAPHICS, CONTROLS & AUDIO MIXER",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            color = NeonCyan
          )
        }
        Text(
          text = "MEM: ${ApexRivals2SettingsRepository.getMemoryUsageMb()}MB • ~${ApexRivals2SettingsRepository.getEstimatedFps()} FPS",
          fontSize = 8.sp,
          color = ApexGreen,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }

      // 1. Graphics Quality Presets (Low, Medium, High + Auto-Detect)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("GRAPHICS QUALITY PRESET:", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
          Text(
            text = "AUTO-RECOMMEND",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = NeonAmber,
            modifier = Modifier
              .clickable {
                val rec = GraphicsQualityPreset.detectRecommendedPreset(context)
                ApexRivals2SettingsRepository.setGraphicsPreset(rec)
              }
              .testTag("graphics_auto_recommend_btn")
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          GraphicsQualityPreset.entries.forEach { preset ->
            val isSelected = graphicsPreset == preset
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) NeonCyan else CarbonBlack)
                .border(1.dp, if (isSelected) NeonCyan else CarbonBorder, RoundedCornerShape(6.dp))
                .clickable { ApexRivals2SettingsRepository.setGraphicsPreset(preset) }
                .padding(vertical = 5.dp)
                .testTag("graphics_preset_${preset.shortLabel}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = preset.shortLabel,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) CarbonBlack else TextPrimary
              )
            }
          }
        }
      }

      // 2. Driving Control Mode (Touch Buttons, Rotary Steering Wheel, Gyroscope Tilt)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "DRIVING CONTROL MODE & SENSITIVITY (${String.format(Locale.US, "%.1fx", controlConfig.steeringSensitivity)}):",
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          DrivingControlMode.entries.forEach { mode ->
            val isSelected = controlConfig.controlMode == mode
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) NeonAmber else CarbonBlack)
                .border(1.dp, if (isSelected) NeonAmber else CarbonBorder, RoundedCornerShape(6.dp))
                .clickable { ApexRivals2SettingsRepository.setDrivingControlMode(mode) }
                .padding(vertical = 5.dp)
                .testTag("control_mode_${mode.shortLabel}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = mode.shortLabel,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) CarbonBlack else TextPrimary
              )
            }
          }
        }
        Slider(
          value = controlConfig.steeringSensitivity,
          onValueChange = { ApexRivals2SettingsRepository.setSteeringSensitivity(it) },
          valueRange = 0.5f..2.0f,
          colors = SliderDefaults.colors(thumbColor = NeonAmber, activeTrackColor = NeonAmber),
          modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .testTag("steering_sensitivity_slider")
        )
      }

      // 3. Independent Volume Controls (Music, Engine, SFX)
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "AUDIO MIXER • MUSIC ${(audioMixer.musicVolume * 100).toInt()}% • ENGINE ${(audioMixer.engineVolume * 100).toInt()}% • SFX ${(audioMixer.sfxVolume * 100).toInt()}%",
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          color = NeonCyan,
          fontFamily = FontFamily.Monospace
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("MUS", fontSize = 7.5.sp, color = TextSecondary, modifier = Modifier.width(26.dp))
          Slider(
            value = audioMixer.musicVolume,
            onValueChange = {
              ApexRivals2SettingsRepository.updateAudioMixer(musicVolume = it)
              soundManager.setChannelVolumes(it, audioMixer.engineVolume, audioMixer.sfxVolume)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
            modifier = Modifier.weight(1f).height(20.dp)
          )
          Text("ENG", fontSize = 7.5.sp, color = TextSecondary, modifier = Modifier.width(26.dp))
          Slider(
            value = audioMixer.engineVolume,
            onValueChange = {
              ApexRivals2SettingsRepository.updateAudioMixer(engineVolume = it)
              soundManager.setChannelVolumes(audioMixer.musicVolume, it, audioMixer.sfxVolume)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = ApexGreen, activeTrackColor = ApexGreen),
            modifier = Modifier.weight(1f).height(20.dp)
          )
          Text("SFX", fontSize = 7.5.sp, color = TextSecondary, modifier = Modifier.width(24.dp))
          Slider(
            value = audioMixer.sfxVolume,
            onValueChange = {
              ApexRivals2SettingsRepository.updateAudioMixer(sfxVolume = it)
              soundManager.setChannelVolumes(audioMixer.musicVolume, audioMixer.engineVolume, it)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = NeonAmber, activeTrackColor = NeonAmber),
            modifier = Modifier.weight(1f).height(20.dp)
          )
        }
      }

      // 4. Multiplayer Staging vs Production Environment Toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "MULTIPLAYER BACKEND: ${backendEnv.label.uppercase()}",
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary,
          fontFamily = FontFamily.Monospace
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CarbonBlack)
            .border(1.dp, NeonCyan, RoundedCornerShape(4.dp))
            .clickable { ApexRivals2SettingsRepository.toggleBackendEnvironment() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("toggle_backend_env_btn")
        ) {
          Text(
            text = "SWITCH ENV",
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Black,
            color = NeonCyan
          )
        }
      }
    }
  }
}

/**
 * PROMPT 2: Virtual Rotary Steering Wheel & Android SensorManager Gyroscope Tilt Listener
 * for RacingGameScreen.kt, plus Handbrake (E-Brake) Drift Button.
 */
@Composable
fun GyroscopeSteeringEffect(
  enabled: Boolean,
  sensitivity: Float,
  onSteerAngleChanged: (Float) -> Unit
) {
  val context = LocalContext.current
  DisposableEffect(enabled, sensitivity) {
    if (!enabled) {
      onDispose { }
    } else {
      val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
      val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
      val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          val values = event?.values ?: return
          // Landscape / portrait adaptive tilt normalization (-1f .. +1f)
          val rawTilt = (-values[0] / 6.0f).coerceIn(-1f, 1f)
          val deadzoneFiltered = if (kotlin.math.abs(rawTilt) < 0.06f) 0f else rawTilt
          onSteerAngleChanged((deadzoneFiltered * sensitivity).coerceIn(-1f, 1f))
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }
      if (accelSensor != null) {
        sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
      }
      onDispose {
        sensorManager?.unregisterListener(listener)
      }
    }
  }
}

@Composable
fun RotarySteeringWheelControl(
  sensitivity: Float,
  onSteerChanged: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  var wheelAngleDeg by remember { mutableFloatStateOf(0f) }

  Box(
    modifier = modifier
      .size(116.dp)
      .pointerInput(sensitivity) {
        detectHorizontalDragGestures(
          onDragEnd = {
            wheelAngleDeg = 0f
            onSteerChanged(0f)
          },
          onDragCancel = {
            wheelAngleDeg = 0f
            onSteerChanged(0f)
          },
          onHorizontalDrag = { _, dragAmount ->
            wheelAngleDeg = (wheelAngleDeg + dragAmount * 1.4f * sensitivity).coerceIn(-90f, 90f)
            onSteerChanged((wheelAngleDeg / 90f).coerceIn(-1f, 1f))
          }
        )
      }
      .testTag("rotary_steering_wheel_control"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val r = size.minDimension * 0.44f
      val center = Offset(size.width * 0.5f, size.height * 0.5f)
      rotate(degrees = wheelAngleDeg, pivot = center) {
        // Outer Carbon Steering Rim
        drawCircle(
          color = Color(0xFF151922),
          radius = r,
          center = center,
          style = Stroke(width = 14f)
        )
        drawCircle(
          color = NeonCyan,
          radius = r,
          center = center,
          style = Stroke(width = 2.5f)
        )
        // Top 12-o'clock Racing Red Center Stripe
        drawCircle(
          color = RedlineRed,
          radius = 5f,
          center = Offset(center.x, center.y - r)
        )
        // Steering Spokes
        drawLine(
          color = Color(0xFF90A4AE),
          start = Offset(center.x - r, center.y),
          end = Offset(center.x + r, center.y),
          strokeWidth = 7f
        )
        drawLine(
          color = Color(0xFF90A4AE),
          start = center,
          end = Offset(center.x, center.y + r),
          strokeWidth = 7f
        )
        // Center Hub
        drawCircle(
          color = Color(0xFF263238),
          radius = r * 0.28f,
          center = center
        )
      }
    }
    Text(
      text = "${wheelAngleDeg.toInt()}°",
      fontSize = 8.5.sp,
      fontWeight = FontWeight.Black,
      color = NeonCyan,
      fontFamily = FontFamily.Monospace
    )
  }
}

@Composable
fun HandbrakeDriftButton(
  onHandbrakeStateChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  LaunchedEffect(isPressed) {
    onHandbrakeStateChanged(isPressed)
  }

  Box(
    modifier = modifier
      .size(52.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(if (isPressed) NeonAmber else CarbonSurfaceVariant.copy(alpha = 0.88f))
      .border(1.5.dp, NeonAmber, RoundedCornerShape(12.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {}
      )
      .testTag("handbrake_btn"),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "E-BRAKE",
        fontSize = 7.5.sp,
        fontWeight = FontWeight.Black,
        color = if (isPressed) CarbonBlack else NeonAmber
      )
      Text(
        text = "DRIFT",
        fontSize = 7.sp,
        fontWeight = FontWeight.Bold,
        color = if (isPressed) CarbonBlack else TextPrimary
      )
    }
  }
}
