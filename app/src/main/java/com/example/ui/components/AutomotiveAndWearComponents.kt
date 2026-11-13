package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automotive.AutomotiveSafetyState
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
import com.example.wear.WearOsCompanionState

@Composable
fun AutomotiveSafetyCompactBadge(
  safetyState: AutomotiveSafetyState,
  onToggleSimulatedDrive: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val badgeColor = if (safetyState.isVehicleMoving) RedlineRed else ApexGreen
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(badgeColor.copy(alpha = 0.16f))
      .border(1.dp, badgeColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
      .clickable { onToggleSimulatedDrive(!safetyState.isVehicleMoving) }
      .padding(horizontal = 6.dp, vertical = 4.dp)
      .testTag("automotive_safety_compact_badge")
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = if (safetyState.isVehicleMoving) Icons.Default.Lock else Icons.Default.VerifiedUser,
        contentDescription = "Automotive Safety Status",
        tint = badgeColor,
        modifier = Modifier.size(11.dp)
      )
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = if (safetyState.isVehicleMoving) "DRIVING" else "PARKED",
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Black,
        color = badgeColor,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}

@Composable
fun AutomotiveDrivingSafetyLockout(
  safetyState: AutomotiveSafetyState,
  onToggleSimulatedDrive: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack.copy(alpha = 0.94f))
      .testTag("automotive_driving_safety_lockout"),
    contentAlignment = Alignment.Center
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = CarbonSurface),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(2.dp, RedlineRed),
      modifier = Modifier
        .fillMaxWidth(0.86f)
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(RedlineRed.copy(alpha = 0.2f))
            .border(1.5.dp, RedlineRed, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = RedlineRed,
            modifier = Modifier.size(28.dp)
          )
        }
        Text(
          text = "DRIVER DISTRACTION SAFETY LOCKOUT",
          fontSize = 14.sp,
          fontWeight = FontWeight.Black,
          color = RedlineRed,
          textAlign = TextAlign.Center,
          letterSpacing = 0.8.sp
        )
        Text(
          text = "Vehicle motion detected. In compliance with Android Automotive OS safety standards, gameplay and racing audio are paused until the vehicle is parked.",
          fontSize = 11.sp,
          color = TextSecondary,
          textAlign = TextAlign.Center,
          lineHeight = 16.sp
        )
        Button(
          onClick = { onToggleSimulatedDrive(false) },
          colors = ButtonDefaults.buttonColors(containerColor = ApexGreen),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("engage_park_brake_btn")
        ) {
          Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint = CarbonBlack,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "ENGAGE PARK BRAKE (RESUME)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = CarbonBlack
          )
        }
      }
    }
  }
}

@Composable
fun AutomotiveSafetyComplianceCard(
  safetyState: AutomotiveSafetyState,
  onToggleSimulatedDrive: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = CarbonSurfaceVariant),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, if (safetyState.isVehicleMoving) RedlineRed else ApexGreen.copy(alpha = 0.6f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("automotive_safety_compliance_card")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = if (safetyState.isVehicleMoving) RedlineRed else ApexGreen,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "AUTOMOTIVE OS SAFETY GATE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = safetyState.statusMessage,
          fontSize = 8.5.sp,
          color = if (safetyState.isVehicleMoving) RedlineRed else TextSecondary,
          fontFamily = FontFamily.Monospace
        )
      }
      Switch(
        checked = safetyState.isVehicleMoving,
        onCheckedChange = onToggleSimulatedDrive,
        colors = SwitchDefaults.colors(
          checkedThumbColor = Color.White,
          checkedTrackColor = RedlineRed,
          uncheckedThumbColor = ApexGreen,
          uncheckedTrackColor = CarbonBlack
        )
      )
    }
  }
}

@Composable
fun WearOsCompanionCard(
  wearState: WearOsCompanionState,
  onToggleHaptic: () -> Unit,
  onToggleConnection: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = CarbonSurfaceVariant),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("wear_os_companion_card")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Watch,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = "WEAR OS SMARTWATCH TELEMETRY",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Black,
              color = TextPrimary
            )
            Text(
              text = "${wearState.watchModelName} • ${if (wearState.isCompanionConnected) "CONNECTED (${wearState.liveHeartRateBpm} BPM)" else "STANDBY"}",
              fontSize = 8.5.sp,
              color = if (wearState.isCompanionConnected) ApexGreen else TextSecondary,
              fontFamily = FontFamily.Monospace
            )
          }
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (wearState.hapticShiftAlertsEnabled) NeonCyan.copy(alpha = 0.2f) else CarbonBlack)
            .border(1.dp, if (wearState.hapticShiftAlertsEnabled) NeonCyan else CarbonBorder, RoundedCornerShape(6.dp))
            .clickable { onToggleHaptic() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = if (wearState.hapticShiftAlertsEnabled) "WRIST SHIFT HAPTICS: ON" else "HAPTICS: OFF",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (wearState.hapticShiftAlertsEnabled) NeonCyan else TextSecondary
          )
        }
      }
    }
  }
}
