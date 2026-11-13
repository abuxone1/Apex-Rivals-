package com.example.model

data class TrackPoint(
  val x: Float, // normalized 0..1
  val y: Float, // normalized 0..1
  val speedSuggestedKmh: Float,
  val sector: Int,
  val turnName: String? = null
)

data class Track(
  val id: String,
  val name: String,
  val location: String,
  val totalLengthMeters: Int,
  val turns: Int,
  val sector1EndProgress: Float = 0.33f,
  val sector2EndProgress: Float = 0.68f,
  val referenceLapTimeMs: Long,
  val pathPoints: List<TrackPoint>,
  val isCityMode: Boolean = false,
  val cityFeatures: String = ""
)

val TRACK_NEO_TOKYO_NIGHT = Track(
  id = "track_neo_tokyo",
  name = "Neo Tokyo Night City",
  location = "Shibuya & Odaiba (Rain & Neon)",
  totalLengthMeters = 5640,
  turns = 16,
  sector1EndProgress = 0.32f,
  sector2EndProgress = 0.66f,
  referenceLapTimeMs = 84600L, // 1:24.600
  isCityMode = true,
  cityFeatures = "Wet Neon Reflections • Rainbow Bridge • Underground Tunnels • 16-Car Grid",
  pathPoints = listOf(
    TrackPoint(0.15f, 0.85f, 325f, 1, "Rainbow Bridge Expressway"),
    TrackPoint(0.42f, 0.85f, 345f, 1, "Shibuya Neon Underpass"),
    TrackPoint(0.70f, 0.82f, 215f, 1, "Shibaura Ramp"),
    TrackPoint(0.82f, 0.68f, 155f, 1, "Tokyo Tower Chicane Right"),
    TrackPoint(0.80f, 0.48f, 240f, 2, "Underground Tunnel Bore"),
    TrackPoint(0.72f, 0.32f, 195f, 2, "Ginza Neon Curve"),
    TrackPoint(0.55f, 0.22f, 290f, 2, "Elevated Expressway Flyover"),
    TrackPoint(0.32f, 0.18f, 135f, 2, "Harumi Hairpin"),
    TrackPoint(0.18f, 0.32f, 280f, 3, "Bayside Linkway"),
    TrackPoint(0.12f, 0.58f, 210f, 3, "Odaiba Chicane Left-Right"),
    TrackPoint(0.15f, 0.85f, 325f, 3, "Start / Finish Straight")
  )
)

val TRACK_MARINA_BAY_NIGHT = Track(
  id = "track_marina_bay",
  name = "Marina Bay Night GP",
  location = "Downtown Waterfront (Night Street GP)",
  totalLengthMeters = 5063,
  turns = 19,
  sector1EndProgress = 0.34f,
  sector2EndProgress = 0.69f,
  referenceLapTimeMs = 92400L, // 1:32.400
  isCityMode = true,
  cityFeatures = "Illuminated Grandstands • Glowing Brake Rotors • City Skyline • Street Apexes",
  pathPoints = listOf(
    TrackPoint(0.20f, 0.85f, 310f, 1, "Pit Straight & Grandstands"),
    TrackPoint(0.50f, 0.85f, 315f, 1, "Sheares Curve Approach"),
    TrackPoint(0.72f, 0.80f, 120f, 1, "Turn 1-2-3 Complex"),
    TrackPoint(0.82f, 0.62f, 270f, 1, "Republic Boulevard"),
    TrackPoint(0.78f, 0.45f, 110f, 2, "Turn 7 Right Hand 90°"),
    TrackPoint(0.68f, 0.32f, 255f, 2, "St Andrew's Road Fast Link"),
    TrackPoint(0.48f, 0.24f, 140f, 2, "Padang Chicane"),
    TrackPoint(0.30f, 0.28f, 130f, 2, "Anderson Bridge"),
    TrackPoint(0.22f, 0.42f, 105f, 3, "Esplanade Hairpin"),
    TrackPoint(0.18f, 0.62f, 220f, 3, "Floating Stadium Waterfront"),
    TrackPoint(0.20f, 0.85f, 310f, 3, "Start / Finish")
  )
)

val TRACK_MIAMI_HIGHWAY = Track(
  id = "track_miami_highway",
  name = "Miami Ocean Causeway",
  location = "Miami Bayside & Ocean Drive",
  totalLengthMeters = 6180,
  turns = 12,
  sector1EndProgress = 0.30f,
  sector2EndProgress = 0.65f,
  referenceLapTimeMs = 88500L, // 1:28.500
  isCityMode = true,
  cityFeatures = "Open City Causeway • Wide Drift Corners • Sunset Palm Boulevards • Meetups",
  pathPoints = listOf(
    TrackPoint(0.15f, 0.82f, 335f, 1, "MacArthur Causeway Bridge"),
    TrackPoint(0.45f, 0.82f, 350f, 1, "High Speed Causeway"),
    TrackPoint(0.75f, 0.78f, 220f, 1, "South Beach Ramp"),
    TrackPoint(0.85f, 0.60f, 160f, 1, "Ocean Drive Palm Sweeper"),
    TrackPoint(0.80f, 0.42f, 260f, 2, "Collins Avenue Straight"),
    TrackPoint(0.68f, 0.28f, 175f, 2, "Venetian Islands Curve"),
    TrackPoint(0.48f, 0.22f, 290f, 2, "Bayside Fast S-Bends"),
    TrackPoint(0.28f, 0.25f, 145f, 2, "Downtown Brickell Hairpin"),
    TrackPoint(0.18f, 0.42f, 275f, 3, "Bayfront Overpass Link"),
    TrackPoint(0.12f, 0.65f, 230f, 3, "PortMiami Chicane"),
    TrackPoint(0.15f, 0.82f, 335f, 3, "Finish Line")
  )
)

val TRACK_MONZA_APEX = Track(
  id = "track_monza",
  name = "Monza Speed Autodrome",
  location = "Monza, Italy",
  totalLengthMeters = 5793,
  turns = 11,
  sector1EndProgress = 0.32f,
  sector2EndProgress = 0.67f,
  referenceLapTimeMs = 78400L, // 1:18.400
  pathPoints = listOf(
    TrackPoint(0.15f, 0.85f, 320f, 1, "Main Straight"),
    TrackPoint(0.40f, 0.85f, 335f, 1),
    TrackPoint(0.65f, 0.85f, 310f, 1, "Prima Variante Braking"),
    TrackPoint(0.72f, 0.78f, 95f, 1, "T1 Chicane Right"),
    TrackPoint(0.70f, 0.70f, 110f, 1, "T2 Chicane Left"),
    TrackPoint(0.75f, 0.58f, 240f, 1, "Curva Grande"),
    TrackPoint(0.85f, 0.45f, 260f, 1),
    TrackPoint(0.85f, 0.32f, 130f, 2, "Variante della Roggia"),
    TrackPoint(0.80f, 0.28f, 140f, 2),
    TrackPoint(0.70f, 0.25f, 210f, 2, "Lesmo 1"),
    TrackPoint(0.55f, 0.20f, 180f, 2, "Lesmo 2"),
    TrackPoint(0.35f, 0.22f, 290f, 2, "Curva del Serraglio"),
    TrackPoint(0.20f, 0.30f, 160f, 3, "Variante Ascari 1"),
    TrackPoint(0.18f, 0.42f, 190f, 3, "Variante Ascari 2"),
    TrackPoint(0.15f, 0.55f, 280f, 3, "Back Straight"),
    TrackPoint(0.12f, 0.72f, 215f, 3, "Curva Parabolica Apex"),
    TrackPoint(0.15f, 0.85f, 320f, 3, "Start / Finish")
  )
)

val TRACK_SUZUKA_APEX = Track(
  id = "track_suzuka",
  name = "Suzuka Grand GP",
  location = "Mie Prefecture, Japan",
  totalLengthMeters = 5807,
  turns = 18,
  sector1EndProgress = 0.35f,
  sector2EndProgress = 0.70f,
  referenceLapTimeMs = 89200L, // 1:29.200
  pathPoints = listOf(
    TrackPoint(0.20f, 0.80f, 290f, 1, "Start Line"),
    TrackPoint(0.35f, 0.80f, 180f, 1, "First Curve"),
    TrackPoint(0.48f, 0.72f, 170f, 1, "S-Curves 1"),
    TrackPoint(0.42f, 0.62f, 175f, 1, "S-Curves 2"),
    TrackPoint(0.50f, 0.52f, 165f, 1, "Dunlop Curve"),
    TrackPoint(0.65f, 0.48f, 140f, 2, "Degner 1"),
    TrackPoint(0.72f, 0.42f, 110f, 2, "Degner 2"),
    TrackPoint(0.70f, 0.32f, 85f, 2, "Hairpin Corner"),
    TrackPoint(0.60f, 0.25f, 240f, 2, "Spoon Curve Entry"),
    TrackPoint(0.45f, 0.20f, 190f, 2, "Spoon Apex"),
    TrackPoint(0.25f, 0.35f, 310f, 3, "Back Straight 130R"),
    TrackPoint(0.15f, 0.52f, 295f, 3, "130R Apex"),
    TrackPoint(0.12f, 0.68f, 75f, 3, "Casio Triangle Chicane"),
    TrackPoint(0.20f, 0.80f, 290f, 3, "Final Straight")
  )
)

val TRACK_TOKYO_EXPRESSWAY = Track(
  id = "track_tokyo",
  name = "Tokyo Midnight Expressway",
  location = "Tokyo, Japan (Night)",
  totalLengthMeters = 6420,
  turns = 14,
  sector1EndProgress = 0.30f,
  sector2EndProgress = 0.65f,
  referenceLapTimeMs = 94100L, // 1:34.100
  isCityMode = true,
  cityFeatures = "Elevated Viaducts • Tunnel Straightaway • Urban Skyline",
  pathPoints = listOf(
    TrackPoint(0.15f, 0.85f, 330f, 1, "Rainbow Bridge"),
    TrackPoint(0.45f, 0.85f, 340f, 1),
    TrackPoint(0.75f, 0.80f, 210f, 1, "Shibaura Ramp"),
    TrackPoint(0.85f, 0.65f, 160f, 1, "Tunnel Entry"),
    TrackPoint(0.82f, 0.45f, 250f, 2, "Underground Tunnel"),
    TrackPoint(0.70f, 0.30f, 190f, 2, "Ginza Curve"),
    TrackPoint(0.50f, 0.25f, 280f, 2, "Elevated Viaduct"),
    TrackPoint(0.30f, 0.20f, 140f, 2, "Harumi Hairpin"),
    TrackPoint(0.18f, 0.35f, 270f, 3, "Bayside Link"),
    TrackPoint(0.12f, 0.60f, 220f, 3, "Odaiba Chicane"),
    TrackPoint(0.15f, 0.85f, 330f, 3, "Finish Line")
  )
)

val AVAILABLE_TRACKS = listOf(
  TRACK_NEO_TOKYO_NIGHT,
  TRACK_MARINA_BAY_NIGHT,
  TRACK_MIAMI_HIGHWAY,
  TRACK_MONZA_APEX,
  TRACK_SUZUKA_APEX,
  TRACK_TOKYO_EXPRESSWAY
)
