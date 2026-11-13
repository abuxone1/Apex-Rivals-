package com.example.game.landscape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.InteractiveRacingEngine
import com.example.model.VehicleType
import java.util.Locale

private val PanelGraphite = Color(0xFF0E121A)
private val PanelSurface = Color(0xFF171D28)
private val PanelBorder = Color(0xFF2B3548)
private val RacingRedAccent = Color(0xFFFF1744)
private val TelemetryCyan = Color(0xFF00E5FF)
private val TelemetryGreen = Color(0xFF00E676)
private val TelemetryAmber = Color(0xFFFFD600)
private val SilverText = Color(0xFFECEFF1)
private val MutedSilver = Color(0xFF90A4AE)

/**
 * Interactive in-game Editor & Debug Tool (Requirement 9) for tweaking procedural seed,
 * biome, scenery density, time of day, weather, and vehicle track profile with live regeneration.
 */
@Composable
fun LandscapeEditorOverlay(
  engine: InteractiveRacingEngine,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val controller = engine.landscapeController
  val config by controller.config.collectAsState()
  val telemetry by controller.telemetry.collectAsState()

  Box(
    modifier = modifier
      .widthIn(max = 460.dp)
      .fillMaxWidth(0.92f)
      .clip(RoundedCornerShape(14.dp))
      .background(PanelGraphite.copy(alpha = 0.96f))
      .border(1.5.dp, RacingRedAccent.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
      .padding(14.dp)
      .testTag("landscape_editor_panel")
  ) {
    Column(
      modifier = Modifier.verticalScroll(rememberScrollState()),
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
            imageVector = Icons.Default.Terrain,
            contentDescription = "Procedural Landscape Editor",
            tint = RacingRedAccent,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "PROCEDURAL LANDSCAPE STUDIO",
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              color = SilverText,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.8.sp
            )
            Text(
              text = "SEEDED HEIGHTMAP • BIOMES • INSTANCED LOD STREAMING",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = TelemetryCyan,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(48.dp)
            .testTag("landscape_editor_close_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close Landscape Editor",
            tint = SilverText
          )
        }
      }

      // Live Streaming, LOD & Surface Physics Telemetry Strip
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(PanelSurface)
          .border(1.dp, PanelBorder, RoundedCornerShape(8.dp))
          .padding(8.dp)
          .testTag("landscape_telemetry_readout"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "CHUNKS: ${telemetry.activeChunkIndices.joinToString(",")}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TelemetryCyan,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "BATCHES: ${telemetry.activeBatchCount} | ZONE: ${telemetry.currentFeatureZone.name}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = SilverText,
            fontFamily = FontFamily.Monospace
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "LOD0: ${telemetry.lod0Count} • LOD1: ${telemetry.lod1Count} • LOD2: ${telemetry.lod2Count} • CULLED: ${telemetry.culledCount}",
            fontSize = 8.5.sp,
            color = MutedSilver,
            fontFamily = FontFamily.Monospace
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "SURFACE: ${telemetry.currentSurface.name} (${String.format(Locale.US, "%.2fx", telemetry.effectiveSurfaceGrip)} GRIP)",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = if (telemetry.currentSurface == SurfaceType.ROAD) TelemetryGreen else TelemetryAmber,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = String.format(
              Locale.US,
              "ELEV: %+.1fm | BANK: %+.1f°",
              telemetry.currentElevationMeters,
              telemetry.currentBankingDegrees
            ),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = SilverText,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // 1. Seed Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "PROCEDURAL SEED",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MutedSilver,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "#${config.seed}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = TelemetryAmber,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.testTag("landscape_seed_value")
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(
            modifier = Modifier
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(PanelSurface)
              .border(1.dp, PanelBorder, RoundedCornerShape(8.dp))
              .clickable {
                engine.updateLandscapeSeed(config.seed + 1337L)
              }
              .padding(horizontal = 12.dp)
              .testTag("landscape_seed_step_btn"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "NEXT SEED",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              color = SilverText,
              fontFamily = FontFamily.Monospace
            )
          }

          Box(
            modifier = Modifier
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(TelemetryCyan.copy(alpha = 0.18f))
              .border(1.dp, TelemetryCyan, RoundedCornerShape(8.dp))
              .clickable {
                val randomSeed = (100000L..99999999L).random()
                engine.updateLandscapeSeed(randomSeed)
              }
              .padding(horizontal = 12.dp)
              .testTag("landscape_seed_random_btn"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "RANDOMIZE",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              color = TelemetryCyan,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      // 2. Switchable Biomes
      Column {
        Text(
          text = "BIOME PRESET (GROUND, VEGETATION, ROCKS & SKYBOX)",
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = MutedSilver,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          BiomeType.entries.forEach { biome ->
            val selected = config.biome == biome
            Box(
              modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) RacingRedAccent else PanelSurface)
                .border(
                  1.dp,
                  if (selected) RacingRedAccent else PanelBorder,
                  RoundedCornerShape(8.dp)
                )
                .clickable { engine.setLandscapeBiome(biome) }
                .padding(horizontal = 10.dp)
                .testTag("biome_chip_${biome.name}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${biome.badgeIcon} ${biome.shortLabel}",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                color = if (selected) Color.White else SilverText,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }

      // 3. Scenery Placement Density Slider
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "INSTANCED SCENERY DENSITY",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MutedSilver,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = String.format(Locale.US, "%.2fx", config.sceneryDensity),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = TelemetryCyan,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.testTag("landscape_density_label")
          )
        }
        Slider(
          value = config.sceneryDensity,
          onValueChange = { engine.setLandscapeDensity(it) },
          valueRange = 0.25f..2.0f,
          steps = 6,
          colors = SliderDefaults.colors(
            thumbColor = RacingRedAccent,
            activeTrackColor = RacingRedAccent,
            inactiveTrackColor = PanelBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("landscape_density_slider")
        )
      }

      // 4. Atmosphere: Time of Day & Weather
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "TIME OF DAY",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = MutedSilver,
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TimeOfDay.entries.forEach { tod ->
              val selected = config.timeOfDay == tod
              Box(
                modifier = Modifier
                  .height(48.dp)
                  .weight(1f)
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (selected) TelemetryAmber else PanelSurface)
                  .border(1.dp, if (selected) TelemetryAmber else PanelBorder, RoundedCornerShape(6.dp))
                  .clickable { engine.setLandscapeTimeOfDay(tod) }
                  .testTag("tod_chip_${tod.name}"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = tod.name,
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Black,
                  color = if (selected) PanelGraphite else SilverText,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }
      }

      Column {
        Text(
          text = "ATMOSPHERIC WEATHER & VISIBILITY",
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Bold,
          color = MutedSilver,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          WeatherCondition.entries.forEach { weather ->
            val selected = config.weather == weather
            Box(
              modifier = Modifier
                .height(48.dp)
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (selected) TelemetryCyan else PanelSurface)
                .border(1.dp, if (selected) TelemetryCyan else PanelBorder, RoundedCornerShape(6.dp))
                .clickable { engine.setLandscapeWeatherCondition(weather) }
                .testTag("landscape_weather_chip_${weather.name}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = weather.name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = if (selected) PanelGraphite else SilverText,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }

      // 5. Vehicle-Specific Run-Off & Edge Geometry Mode (CAR vs MOTORBIKE)
      Column {
        Text(
          text = "VEHICLE TRACK GEOMETRY PROFILE",
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Bold,
          color = MutedSilver,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          VehicleType.entries.forEach { vType ->
            val selected = config.vehicleType == vType
            val label = if (vType == VehicleType.CAR) {
              "CAR (WIDE RUN-OFF)"
            } else {
              "MOTO (BANKED + LOW EDGE)"
            }
            Box(
              modifier = Modifier
                .height(48.dp)
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (selected) TelemetryGreen else PanelSurface)
                .border(1.dp, if (selected) TelemetryGreen else PanelBorder, RoundedCornerShape(6.dp))
                .clickable { engine.setLandscapeVehicleProfile(vType) }
                .testTag("landscape_vehicle_chip_${vType.name}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                color = if (selected) PanelGraphite else SilverText,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }

      // 6. Regenerate Action Button
      Button(
        onClick = { engine.regenerateLandscape() },
        colors = ButtonDefaults.buttonColors(containerColor = RacingRedAccent),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("landscape_regenerate_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "REGENERATE PROCEDURAL LANDSCAPE",
          fontSize = 11.sp,
          fontWeight = FontWeight.Black,
          color = Color.White,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}
