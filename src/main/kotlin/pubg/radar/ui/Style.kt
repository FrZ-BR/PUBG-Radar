package pubg.radar.ui

import com.badlogic.gdx.graphics.Color
import pubg.radar.gridWidth
import pubg.radar.mapWidth

const val initialWindowWidth = 1000f
const val windowToMapUnit = mapWidth / initialWindowWidth

const val runSpeed = 6.3 * 100 //6.3m/s
const val unit = gridWidth / 8
const val unit2 = unit / 10
//1m=100
const val playerRadius = 3600f
const val vehicle2Width = 400f
const val vehicle4Width = 800f
const val vehicle6Width = 1600f
const val directionRadius = 16000f
const val airDropRadius = 4000f
const val corpseRadius = 400f
const val itemRadius = 300f
const val visionRadius = mapWidth / 8
const val fov = 60f

const val aimLineWidth = 1000f
const val aimLineRange = 50000f
const val aimCircleRadius = 200f
const val aimTimeThreshold = 1000
const val attackLineDuration = 2000
const val attackMeLineDuration = 20000
const val pinRadius = 3000f

val teamColor = Color(1f, 1f, 0f, 1f)  //yellow
val safeDirectionColor = Color(0.22f, 0.52f, 0.95f, 0.3f)  //blue
val visionColor = Color(1f, 1f, 1f, 0.1f)  //white
val corpseColor = Color(1f, 0.49f, 0f, 0.9f)  //orange
val carColor = Color(0.12f, 0.56f, 1f, 0.9f)  //blue
val bikeColor = Color(0.5f, 0.7f, 1f, 0.9f)  //blue
val boatColor = Color(1f, 0.49f, 0f, 0.9f)  //orange
val planeColor = Color(0.93f, 0.90f, 0f, 1.0f)  //yellow
val parachuteColor = Color(0.8f, 0.8f, 0.8f, 1f)  //gray-white
val playerColor = Color.RED!!

val normalItemColor = Color(0.87f, 0.01f, 1.0f, 0.99f)  //
val rare4xColor = Color(0.31f,	0.51f,	0.71f, 0.99f)  //dodgerblue
val rare8xColor = Color(0.7f, 1.0f, 1.0f, 0.99f)  //Cyan

val rareBagColor = Color(0.01f, 0.01f, 0.7f, 0.99f)  //blue
val rareHelmetColor = Color(1.0f, 0.01f, 1.0f, 0.99f)  //magenta
val rareArmorColor = Color(0.63f, 0.13f, 0.94f, 0.99f)  //purple

val rareSniperColor = Color(1.0f, 0.19f, 0.19f, 0.99f)  //firebrick
val rareRifleColor = Color(0.54f, 0.27f, 0.14f, 0.99f)  //sienna4
val rareRifle556Color = Color(0.54f, 0.27f, 0.14f, 0.99f)  //sienna4
val rareRifle762Color = Color(0.86f, 0.43f, 0.24f, 0.99f)  //sienna2

val rareARAttachColor = Color(0.80f, 0.73f, 0.59f, 0.99f)  //wheat3
val rareSRAttachColor = Color(1.00f, 0.68f, 0.73f, 0.99f)  //LightPink1

val healItemColor = Color(0.16f, 0.86f, 0.16f, 0.99f)  //green
val drinkItemColor = Color(0.20f, 0.63f, 0.20f, 0.99f)  //limeGreen

val sightColor = Color(1f, 1f, 1f, 0.5f)

val aimLineColor = Color(0f, 0f, 1f, 1f)
val attackLineColor = Color(1.0f, 0f, 0f, 1f)
val pinColor = Color(1f, 1f, 0f, 1f)
val redZoneColor = Color(1f, 0f, 0f, 0.2f)
val safeZoneColor = Color(1f, 1f, 1f, 0.9f)

val hubColor = Color(1f, 1f, 1f, 1f)