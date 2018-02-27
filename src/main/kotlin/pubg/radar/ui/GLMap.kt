package pubg.radar.ui

import com.badlogic.gdx.*
import com.badlogic.gdx.Input.Buttons.LEFT
import com.badlogic.gdx.Input.Buttons.MIDDLE
import com.badlogic.gdx.Input.Buttons.RIGHT
import com.badlogic.gdx.Input.Keys.NUM_1
import com.badlogic.gdx.Input.Keys.NUM_2
import com.badlogic.gdx.Input.Keys.NUM_3
import com.badlogic.gdx.Input.Keys.NUM_4
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.backends.lwjgl3.*
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.Color.*
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.*
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.*
import com.badlogic.gdx.math.*
import pubg.radar.*
import pubg.radar.deserializer.channel.ActorChannel.Companion.actors
import pubg.radar.deserializer.channel.ActorChannel.Companion.airDropLocation
import pubg.radar.deserializer.channel.ActorChannel.Companion.corpseLocation
import pubg.radar.deserializer.channel.ActorChannel.Companion.droppedItemLocation
import pubg.radar.deserializer.channel.ActorChannel.Companion.visualActors
import pubg.radar.http.PlayerProfile.Companion.completedPlayerInfo
import pubg.radar.http.PlayerProfile.Companion.pendingPlayerInfo
import pubg.radar.http.PlayerProfile.Companion.query
import pubg.radar.sniffer.Sniffer.Companion.localAddr
import pubg.radar.sniffer.Sniffer.Companion.preDirection
import pubg.radar.sniffer.Sniffer.Companion.preSelfCoords
import pubg.radar.sniffer.Sniffer.Companion.selfCoords
import pubg.radar.sniffer.Sniffer.Companion.sniffOption
import pubg.radar.struct.*
import pubg.radar.struct.Archetype.*
import pubg.radar.struct.Archetype.Plane
import pubg.radar.struct.cmd.ActorCMD.actorWithPlayerState
import pubg.radar.struct.cmd.ActorCMD.playerStateToActor
import pubg.radar.struct.cmd.GameStateCMD.ElapsedWarningDuration
import pubg.radar.struct.cmd.GameStateCMD.MatchElapsedMinutes
import pubg.radar.struct.cmd.GameStateCMD.NumAlivePlayers
import pubg.radar.struct.cmd.GameStateCMD.NumAliveTeams
import pubg.radar.struct.cmd.GameStateCMD.PoisonGasWarningPosition
import pubg.radar.struct.cmd.GameStateCMD.PoisonGasWarningRadius
import pubg.radar.struct.cmd.GameStateCMD.RedZonePosition
import pubg.radar.struct.cmd.GameStateCMD.RedZoneRadius
import pubg.radar.struct.cmd.GameStateCMD.SafetyZonePosition
import pubg.radar.struct.cmd.GameStateCMD.SafetyZoneRadius
import pubg.radar.struct.cmd.GameStateCMD.TotalWarningDuration
import pubg.radar.struct.cmd.PlayerStateCMD.attacks
import pubg.radar.struct.cmd.PlayerStateCMD.playerNames
import pubg.radar.struct.cmd.PlayerStateCMD.playerNumKills
import pubg.radar.struct.cmd.PlayerStateCMD.selfID
import pubg.radar.struct.cmd.PlayerStateCMD.teamNumbers
import pubg.radar.util.tuple4
import wumo.pubg.struct.cmd.TeamCMD.team
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

typealias renderInfo = tuple4<Actor, Float, Float, Float>

fun Float.d(n: Int) = String.format("%.${n}f", this)
class GLMap: InputAdapter(), ApplicationListener, GameListener {
  companion object {
    operator fun Vector3.component1(): Float = x
    operator fun Vector3.component2(): Float = y
    operator fun Vector3.component3(): Float = z
    operator fun Vector2.component1(): Float = x
    operator fun Vector2.component2(): Float = y
    
    val spawnErangel = Vector2(795548.3f, 17385.875f)
    val spawnDesert = Vector2(78282f, 731746f)
  }
  
  init {
    register(this)
  }
  
  override fun onGameStart() {
    preSelfCoords.set(if (isErangel) spawnErangel else spawnDesert)
    selfCoords.set(preSelfCoords)
    preDirection.setZero()
  }
  
  override fun onGameOver() {
    camera.zoom = 1 / 4f
    
    aimStartTime.clear()
    attackLineStartTime.clear()
    pinLocation.setZero()
  }
  
  fun show() {
    val config = Lwjgl3ApplicationConfiguration()
    config.setTitle("${sniffOption.name}")
    //config.useOpenGL3(true, 3, 3)
    config.useOpenGL3(false, 3, 2)
    config.setWindowedMode(600, 600)
    config.setResizable(true)
    config.setBackBufferConfig(8, 8, 8, 8, 32, 0, 8)
    Lwjgl3Application(this, config)
  }
  
  lateinit var spriteBatch: SpriteBatch
  lateinit var shapeRenderer: ShapeRenderer
  lateinit var mapErangelTiles: MutableMap<String, MutableMap<String, MutableMap<String, Texture>>>
  lateinit var mapMiramarTiles: MutableMap<String, MutableMap<String, MutableMap<String, Texture>>>
  lateinit var mapTiles: MutableMap<String, MutableMap<String, MutableMap<String, Texture>>>
  lateinit var hub_panel: Texture
  lateinit var hub_panel_blank: Texture
  lateinit var largeFont: BitmapFont
  lateinit var largeFontShadow: BitmapFont
  lateinit var littleFont: BitmapFont
  lateinit var littleFontShadow: BitmapFont
  lateinit var nameFont: BitmapFont
  lateinit var nameFontShadow: BitmapFont
  lateinit var compassFont: BitmapFont
  lateinit var compassFontShadow: BitmapFont
  lateinit var hubFont: BitmapFont
  lateinit var hubFontShadow: BitmapFont
  lateinit var espFont: BitmapFont
  lateinit var espFontShadow: BitmapFont
  lateinit var fontCamera: OrthographicCamera
  lateinit var camera: OrthographicCamera
  lateinit var alarmSound: Sound
  
  val tileZooms = listOf("256", "512", "1024", "2048", "4096", "8192")
  val tileRowCounts = listOf(1, 2, 4, 8, 16, 32)
  val tileSizes = listOf(819200f, 409600f, 204800f, 102400f, 51200f, 25600f)
  

  val layout = GlyphLayout()
  var windowWidth = initialWindowWidth
  var windowHeight = initialWindowWidth
  
  val aimStartTime = HashMap<NetworkGUID, Long>()
  val attackLineStartTime = LinkedList<Triple<NetworkGUID, NetworkGUID, Long>>()
  val pinLocation = Vector2()

  var filterWeapon = 1
  var filterAttach = -1
  var filterLvl2 = -1
  var filterScope = -1

  
  fun Vector2.windowToMap() =
      Vector2(selfCoords.x + (x - windowWidth / 2.0f) * camera.zoom * windowToMapUnit,
              selfCoords.y + (y - windowHeight / 2.0f) * camera.zoom * windowToMapUnit)
  
  fun Vector2.mapToWindow() =
      Vector2((x - selfCoords.x) / (camera.zoom * windowToMapUnit) + windowWidth / 2.0f,
              (y - selfCoords.y) / (camera.zoom * windowToMapUnit) + windowHeight / 2.0f)
  
  override fun scrolled(amount: Int): Boolean {
    camera.zoom *= 1.2f.pow(amount)
    return true
  }
  
  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (button == MIDDLE) {
      pinLocation.set(pinLocation.set(screenX.toFloat(), screenY.toFloat()).windowToMap())
      return true
    } else if (button == LEFT) {
      camera.zoom = 1 / 10f
      camera.update()
    } else if (button == RIGHT) {
      camera.zoom = 1 / 4f
      camera.update()
    }
    return false
  }

  override fun keyDown(keycode: Int): Boolean {
    if (keycode == NUM_1) {
      filterWeapon = filterWeapon * -1
    } else if (keycode == NUM_2) {
      filterAttach = filterAttach * -1
    } else if (keycode == NUM_3) {
      filterLvl2 = filterLvl2 * -1
    } else if (keycode == NUM_4) {
      filterScope = filterScope * -1
    }
    return false
  }
  
  override fun create() {
    spriteBatch = SpriteBatch()
    shapeRenderer = ShapeRenderer()
    Gdx.input.inputProcessor = this;
    camera = OrthographicCamera(windowWidth, windowHeight)
    with(camera) {
      setToOrtho(true, windowWidth * windowToMapUnit, windowHeight * windowToMapUnit)
      zoom = 1 / 4f
      update()
      position.set(mapWidth / 2, mapWidth / 2, 0f)
      update()
    }
    
    fontCamera = OrthographicCamera(initialWindowWidth, initialWindowWidth)
    alarmSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Alarm.wav"))
    hub_panel = Texture(Gdx.files.internal("images/hub_panel.png"))
    hub_panel_blank = Texture(Gdx.files.internal("images/hub_panel_blank.png"))
    mapErangelTiles = mutableMapOf()
    mapMiramarTiles = mutableMapOf()
    var cur = 0
    tileZooms.forEach{
        mapErangelTiles.set(it, mutableMapOf())
        mapMiramarTiles.set(it, mutableMapOf())
        for (i in 1..tileRowCounts[cur]) {
            val y = if (i < 10) "0$i" else "$i"
            mapErangelTiles[it]?.set(y, mutableMapOf())
            mapMiramarTiles[it]?.set(y, mutableMapOf())
            for (j in 1..tileRowCounts[cur]) {
                val x = if (j < 10) "0$j" else "$j"
                mapErangelTiles[it]!![y]?.set(x, Texture(Gdx.files.internal("tiles/Erangel/${it}/${it}_${y}_${x}.png")))
                mapMiramarTiles[it]!![y]?.set(x, Texture(Gdx.files.internal("tiles/Miramar/${it}/${it}_${y}_${x}.png")))
            }
        }
        cur++
    }
    mapTiles = mapErangelTiles

    val generatorHub = FreeTypeFontGenerator(Gdx.files.internal("font/AgencyFB.ttf"))
    val paramHub = FreeTypeFontParameter()
    paramHub.characters = DEFAULT_CHARS
    paramHub.size = 30
    paramHub.color = WHITE
    hubFont = generatorHub.generateFont(paramHub)
    paramHub.color = Color(1f, 1f, 1f, 0.4f) 
    hubFontShadow = generatorHub.generateFont(paramHub)
    paramHub.size = 16
    paramHub.color = WHITE
    espFont = generatorHub.generateFont(paramHub)
    paramHub.color = Color(1f, 1f, 1f, 0.2f) 
    espFontShadow = generatorHub.generateFont(paramHub)
    
    val generatorNumber = FreeTypeFontGenerator(Gdx.files.internal("font/CenturyGothicBold.ttf"))
    val paramNumber = FreeTypeFontParameter()
    paramNumber.characters = DEFAULT_CHARS
    paramNumber.size = 24
    paramNumber.color = WHITE
    largeFont = generatorNumber.generateFont(paramNumber)
    paramNumber.color = Color(0f, 0f, 0f, 0.5f) 
    largeFontShadow = generatorNumber.generateFont(paramNumber)
    paramNumber.size = 14
    paramNumber.color = compassColor
    compassFont = generatorNumber.generateFont(paramNumber)
    paramNumber.color = Color(0f, 0f, 0f, 0.5f) 
    compassFontShadow = generatorNumber.generateFont(paramNumber)

    val generator = FreeTypeFontGenerator(Gdx.files.internal("font/ArialNarrowBold.ttf"))
    val param = FreeTypeFontParameter()
    param.characters = DEFAULT_CHARS
    param.size = 20
    param.color = WHITE
    littleFont = generator.generateFont(param)
    param.color = Color(0f, 0f, 0f, 0.5f) 
    littleFontShadow = generator.generateFont(param)
    param.size = 11
    param.color = Color(0.9f, 0.9f, 0.9f, 1f) 
    nameFont = generator.generateFont(param)
    param.color = Color(0f, 0f, 0f, 0.5f) 
    nameFontShadow = generator.generateFont(param)

    generatorHub.dispose()
    generatorNumber.dispose()
    generator.dispose()    
  }
  
  val dirUnitVector = Vector2(1f, 0f)
  override fun render() {
    Gdx.gl.glClearColor(0.417f, 0.417f, 0.417f, 0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    if (gameStarted)
      mapTiles = if (isErangel) mapErangelTiles else mapMiramarTiles
    else
      return
    val currentTime = System.currentTimeMillis()
    val (selfX, selfY) = selfCoords
    val selfDir = Vector2(selfX, selfY).sub(preSelfCoords)
    if (selfDir.len() < 1e-8)
      selfDir.set(preDirection)
    
    //move camera
    camera.position.set(selfX, selfY, 0f)
    camera.update()
    
    //draw map
    val cameraTileScale = Math.max(windowWidth, windowHeight) / camera.zoom
    var useScale = 0
    when {
        cameraTileScale > 4096 -> useScale = 5
        cameraTileScale > 2048 -> useScale = 4
        cameraTileScale > 1024 -> useScale = 3
        cameraTileScale > 512 -> useScale = 2
        cameraTileScale > 256 -> useScale = 1
        else -> useScale = 0
    }
    val (tlX, tlY) = Vector2(0f, 0f).windowToMap()
    val (brX, brY) = Vector2(windowWidth, windowHeight).windowToMap()
    var tileZoom = tileZooms[useScale]
    var tileRowCount = tileRowCounts[useScale]
    var tileSize = tileSizes[useScale]
    paint(camera.combined) {
      val xMin = (tlX.toInt() / tileSize.toInt()).coerceIn(1, tileRowCount)
      val xMax = ((brX.toInt() + tileSize.toInt()) / tileSize.toInt()).coerceIn(1, tileRowCount)
      val yMin = (tlY.toInt() / tileSize.toInt()).coerceIn(1, tileRowCount)
      val yMax = ((brY.toInt() + tileSize.toInt()) / tileSize.toInt()).coerceIn(1, tileRowCount)
      for (i in yMin..yMax) {
        val y = if (i < 10) "0$i" else "$i"
        for (j in xMin..xMax) {
          val x = if (j < 10) "0$j" else "$j"
          val tileStartX = (j-1)*tileSize
          val tileStartY = (i-1)*tileSize
          draw(mapTiles[tileZoom]!![y]!![x], tileStartX, tileStartY, tileSize, tileSize,
           0, 0, 256, 256,
            false, true)
        }
      }
    }
    
    shapeRenderer.projectionMatrix = camera.combined
    Gdx.gl.glEnable(GL20.GL_BLEND)
    
    drawGrid()
    drawCircles()
    
    val typeLocation = EnumMap<Archetype, MutableList<renderInfo>>(Archetype::class.java)
    for ((_, actor) in visualActors)
      typeLocation.compute(actor.Type) { _, v ->
        val list = v ?: ArrayList()
        val (centerX, centerY) = actor.location
        val direction = actor.rotation.y
        list.add(tuple4(actor, centerX, centerY, direction))
        list
      }
    
    paint(fontCamera.combined) {
      
      // NUMBER PANEL
      val numText = "$NumAlivePlayers"
      layout.setText(hubFont, numText)
      spriteBatch.draw(hub_panel, windowWidth - 130f, windowHeight - 60f)
      hubFontShadow.draw(spriteBatch, "ALIVE", windowWidth - 85f, windowHeight - 29f)
      hubFont.draw(spriteBatch, "$NumAlivePlayers", windowWidth - 110f - layout.width /2, windowHeight - 29f)

      val teamText = "$NumAliveTeams"
      layout.setText(hubFont, teamText)
      spriteBatch.draw(hub_panel, windowWidth - 260f, windowHeight - 60f)
      hubFontShadow.draw(spriteBatch, "TEAM", windowWidth - 215f, windowHeight - 29f)
      hubFont.draw(spriteBatch, "$NumAliveTeams", windowWidth - 240f - layout.width /2, windowHeight - 29f)

      val timeText = "${TotalWarningDuration.toInt()-ElapsedWarningDuration.toInt()}"
      layout.setText(hubFont, timeText)
      spriteBatch.draw(hub_panel, windowWidth - 390f, windowHeight - 60f)
      hubFontShadow.draw(spriteBatch, "SECS", windowWidth - 345f, windowHeight - 29f)
      hubFont.draw(spriteBatch, "${TotalWarningDuration.toInt()-ElapsedWarningDuration.toInt()}", windowWidth - 370f - layout.width /2, windowHeight - 29f)

      // ITEM ESP FILTER PANEL
      spriteBatch.draw(hub_panel_blank, 30f, windowHeight - 60f)
      
      if (filterWeapon == 1) 
        espFont.draw(spriteBatch, "WEAPON", 37f, windowHeight - 25f)
      else
        espFontShadow.draw(spriteBatch, "WEAPON", 37f, windowHeight - 25f)
      
      if (filterAttach == 1) 
        espFont.draw(spriteBatch, "ATTACH", 37f, windowHeight - 42f)
      else
        espFontShadow.draw(spriteBatch, "ATTACH", 37f, windowHeight - 42f)
      
      if (filterLvl2 == 1) 
        espFont.draw(spriteBatch, "EQUIP", 92f, windowHeight - 25f)
      else
        espFontShadow.draw(spriteBatch, "EQUIP", 92f, windowHeight - 25f)
      
      if (filterScope == 1) 
        espFont.draw(spriteBatch, "SCOPE", 92f, windowHeight - 42f)
      else
        espFontShadow.draw(spriteBatch, "SCOPE", 92f, windowHeight - 42f)
      

      val time = (pinLocation.cpy().sub(selfX, selfY).len() / runSpeed).toInt()
      val pinDistance = (pinLocation.cpy().sub(selfX, selfY).len() / 100).toInt()
      val (x, y) = pinLocation.mapToWindow()
      
      safeZoneHint()
      drawPlayerInfos(typeLocation[Player])
      val profileText = "Weapon: ${filterWeapon}"
      layout.setText(compassFontShadow, profileText)

      for(i in -1..1) {
          for(j in -1..1) {
            compassFontShadow.draw(spriteBatch, "0"  , windowWidth/2 + i, windowHeight/2 + 150 + j)        // N
            compassFontShadow.draw(spriteBatch, "45" , windowWidth/2 + 150 + i, windowHeight/2 + 150 + j)  // NE
            compassFontShadow.draw(spriteBatch, "90" , windowWidth/2 + 150 + i, windowHeight/2 + j)        // E
            compassFontShadow.draw(spriteBatch, "135", windowWidth/2 + 150 + i, windowHeight/2 - 150 + j)  // SE
            compassFontShadow.draw(spriteBatch, "180", windowWidth/2 + i, windowHeight/2 - 150 + j)        // S
            compassFontShadow.draw(spriteBatch, "225", windowWidth/2 - 150 + i, windowHeight/2 - 150+ j)   // SW
            compassFontShadow.draw(spriteBatch, "270", windowWidth/2 - 150 + i, windowHeight/2 + j)        // W
            compassFontShadow.draw(spriteBatch, "315", windowWidth/2 - 150 + i, windowHeight/2 + 150+ j)   // NW
            littleFontShadow.draw(spriteBatch, "$pinDistance", x + i, windowHeight - y + j)
          }
      }
      compassFont.draw(spriteBatch, "0"  , windowWidth/2, windowHeight/2 + 150)        // N
      compassFont.draw(spriteBatch, "45" , windowWidth/2 + 150, windowHeight/2 + 150)  // NE
      compassFont.draw(spriteBatch, "90" , windowWidth/2 + 150, windowHeight/2)        // E
      compassFont.draw(spriteBatch, "135", windowWidth/2 + 150, windowHeight/2 - 150)  // SE
      compassFont.draw(spriteBatch, "180", windowWidth/2, windowHeight/2 - 150)        // S
      compassFont.draw(spriteBatch, "225", windowWidth/2 - 150, windowHeight/2 - 150)  // SW
      compassFont.draw(spriteBatch, "270", windowWidth/2 - 150, windowHeight/2)        // W
      compassFont.draw(spriteBatch, "315", windowWidth/2 - 150, windowHeight/2 + 150)  // NW
      littleFont.draw(spriteBatch, "$pinDistance", x, windowHeight - y)
    }
    
    val zoom = camera.zoom
    
    Gdx.gl.glEnable(GL20.GL_BLEND)
    draw(Filled) {
      color = redZoneColor
      circle(RedZonePosition, RedZoneRadius, 100)
      
      color = visionColor
      circle(selfX, selfY, visionRadius, 100)
      
      color = pinColor
      circle(pinLocation, pinRadius * zoom, 10)
      //draw self
      drawPlayer(LIME, tuple4(null, selfX, selfY, selfDir.angle()))
      drawItem()
      drawAirDrop(zoom)
      drawCorpse()
      drawAPawn(typeLocation, selfX, selfY, zoom, currentTime)
    }
    
    drawAttackLine(currentTime)
    
    preSelfCoords.set(selfX, selfY)
    preDirection = selfDir
    
    Gdx.gl.glDisable(GL20.GL_BLEND)
  }
  
  private fun drawAttackLine(currentTime: Long) {
    while (attacks.isNotEmpty()) {
      val (A, B) = attacks.poll()
      attackLineStartTime.add(Triple(A, B, currentTime))
    }
    if (attackLineStartTime.isEmpty()) return
    draw(Line) {
      val iter = attackLineStartTime.iterator()
      while (iter.hasNext()) {
        val (A, B, st) = iter.next()
        if (A == selfID || B == selfID) {
          val enemyID = if (A == selfID) B else A
          val actorEnemyID = playerStateToActor[enemyID]
          if (actorEnemyID == null) {
            iter.remove()
            continue
          }
          val actorEnemy = actors[actorEnemyID]
          if (actorEnemy == null || currentTime - st > attackMeLineDuration) {
            iter.remove()
            continue
          }
          color = attackLineColor
          val (xA, yA) = selfCoords
          val (xB, yB) = actorEnemy.location
          line(xA, yA, xB, yB)
        } else {
          val actorAID = playerStateToActor[A]
          val actorBID = playerStateToActor[B]
          if (actorAID == null || actorBID == null) {
            iter.remove()
            continue
          }
          val actorA = actors[actorAID]
          val actorB = actors[actorBID]
          if (actorA == null || actorB == null || currentTime - st > attackLineDuration) {
            iter.remove()
            continue
          }
          color = attackLineColor
          val (xA, yA) = actorA.location
          val (xB, yB) = actorB.location
          line(xA, yA, xB, yB)
        }
      }
    }
  }

  private fun drawCircles() {
    Gdx.gl.glLineWidth(2f)
    draw(Line) {
      //vision circle
      
      color = safeZoneColor
      circle(PoisonGasWarningPosition, PoisonGasWarningRadius, 100)
      
      color = BLUE
      circle(SafetyZonePosition, SafetyZoneRadius, 100)
      
      if (PoisonGasWarningPosition.len() > 0) {
        color = safeDirectionColor
        line(selfCoords, PoisonGasWarningPosition)
      }
    }
    Gdx.gl.glLineWidth(1f)
  }
  
  private fun drawGrid() {
    draw(Filled) {
      color = GRAY
      for (i in 0..7) {
        rectLine(0f, i * unit, gridWidth, i * unit, 500f)
        rectLine(i * unit, 0f, i * unit, gridWidth, 500f)
      }

    }
  }

  private fun ShapeRenderer.drawAPawn(typeLocation: EnumMap<Archetype, MutableList<renderInfo>>,
                                      selfX: Float, selfY: Float,
                                      zoom: Float,
                                      currentTime: Long) {
    for ((type, actorInfos) in typeLocation) {
      when (type) {
        TwoSeatBoat -> actorInfos?.forEach {
          drawVehicle(boatColor, it, vehicle2Width, vehicle6Width)
        }
        SixSeatBoat -> actorInfos?.forEach {
          drawVehicle(boatColor, it, vehicle4Width, vehicle6Width)
        }
        TwoSeatBike -> actorInfos?.forEach {
          drawVehicle(bikeColor, it, vehicle2Width, vehicle6Width)
        }
        ThreeSeatBike -> actorInfos?.forEach {
          drawVehicle(bikeColor, it, vehicle4Width, vehicle6Width)
        }
        TwoSeatCar -> actorInfos?.forEach {
          drawVehicle(carColor, it, vehicle2Width, vehicle6Width)
        }
        FourSeatCar -> actorInfos?.forEach {
          drawVehicle(carColor, it, vehicle4Width, vehicle6Width)
        }
        SixSeatCar -> actorInfos?.forEach {
          drawVehicle(carColor, it, vehicle6Width, vehicle6Width)
        }
        Plane -> actorInfos?.forEach {
          drawPlayer(planeColor, it)
        }
        Player -> actorInfos?.forEach {
          drawPlayer(playerColor, it)
          
          aimAtMe(it, selfX, selfY, currentTime, zoom)
        }
        Parachute -> actorInfos?.forEach {
          drawPlayer(parachuteColor, it)
        }
        Grenade -> actorInfos?.forEach {
          drawPlayer(WHITE, it, false)
        }
        else -> {
          //            actorInfos?.forEach {
          //            bugln { "${it._1!!.archetype.pathName} ${it._1.location}" }
          //            drawPlayer(BLACK, it)
          //            }
        }
      }
    }
  }
  
  private fun ShapeRenderer.drawCorpse() {
    corpseLocation.values.forEach {
      val (x, y) = it
      val backgroundRadius = (corpseRadius + 50f)
      val radius = corpseRadius
      color = BLACK
      rect(x - backgroundRadius, y - backgroundRadius, backgroundRadius * 2, backgroundRadius * 2)
      color = corpseColor
      rect(x - radius, y - radius, radius * 2, radius * 2)
    }
  }
  
  private fun ShapeRenderer.drawAirDrop(zoom: Float) {
    airDropLocation.values.forEach {
      val (x, y) = it
      val backgroundRadius = (airDropRadius + 2000) * zoom
      val airDropRadius = airDropRadius * zoom
      color = BLACK
      rect(x - backgroundRadius, y - backgroundRadius, backgroundRadius * 2, backgroundRadius * 2)
      color = BLUE
      rect(x, y - airDropRadius, airDropRadius, airDropRadius * 2)
      color = RED
      rect(x - airDropRadius, y - airDropRadius, airDropRadius, airDropRadius * 2)
    }
  }
  
  private fun ShapeRenderer.drawItem() {
    droppedItemLocation.values.asSequence().filter { it.second.isNotEmpty() }
      .forEach {
        val (x, y) = it.first
        val items = it.second
        val finalColor = it.third
        if (finalColor.a == 0f)
          finalColor.set(
              when {
                "bag3" in items -> rareBagColor
                "helmet3" in items -> rareHelmetColor
                "armor3" in items -> rareArmorColor

                "4x" in items -> rare4xColor
                "8x" in items -> rare8xColor

                "k98" in items -> rareSniperColor
                "m416" in items || "scar" in items || "m16" in items -> rareRifleColor
                "dp28" in items || "ak" in items -> rareRifleColor

                "heal" in items -> healItemColor
                "drink" in items -> drinkItemColor

                "AR_Extended" in items -> rareARAttachColor
                "AR_Suppressor" in items -> rareARAttachColor
                "AR_Composite" in items -> rareARAttachColor
                "SR_Extended" in items -> rareSRAttachColor
                "SR_Suppressor" in items -> rareSRAttachColor
                "CheekPad" in items -> rareSRAttachColor

                "reddot" in items || "holo" in items -> rare4xColor

                "bag2" in items -> rareBagColor
                "helmet2" in items -> rareHelmetColor
                "armor2" in items -> rareArmorColor

                else -> normalItemColor
              }
          )


        val backgroundRadius = (itemRadius + 50f)
        val radius = itemRadius
        val triBackRadius = backgroundRadius * 1.2f
        val triRadius = radius * 1.2f

        if (("bag2" in items || "helmet2" in items || "armor2" in items) && (filterLvl2 == 1)) {
          color = BLACK
          triangle(x - triBackRadius, y - triBackRadius,
                  x - triBackRadius, y + triBackRadius,
                  x + triBackRadius, y - triBackRadius)
          color = finalColor
          triangle(x - triRadius, y - triRadius,
                  x - triRadius, y + triRadius,
                  x + triRadius, y - triRadius)
        } 
        
        if (("reddot" in items || "holo" in items) && (filterScope == 1)) {
          color = BLACK
          triangle(x - triBackRadius, y - triBackRadius,
                  x - triBackRadius, y + triBackRadius,
                  x + triBackRadius, y - triBackRadius)
          color = finalColor
          triangle(x - triRadius, y - triRadius,
                  x - triRadius, y + triRadius,
                  x + triRadius, y - triRadius)
        }
        
        if (filterAttach == 1) {
          if (("AR_Extended" in items || "SR_Extended" in items)) {
            color = BLACK
            rect(x - backgroundRadius, y - backgroundRadius, backgroundRadius * 2, backgroundRadius * 2)
            color = finalColor
            rect(x - radius, y - radius, radius * 2, radius * 2)
          } 
          else if (("AR_Suppressor" in items || "SR_Suppressor" in items)) {
            color = BLACK
            circle(x, y, backgroundRadius * 1.2f, 10)
            color = finalColor
            circle(x, y, radius * 1.2f, 10)
          } 
          else if (("AR_Composite" in items || "CheekPad" in items)) {
            color = BLACK
            triangle(x - triBackRadius, y - triBackRadius,
                    x - triBackRadius, y + triBackRadius,
                    x + triBackRadius, y - triBackRadius)
            color = finalColor
            triangle(x - triRadius, y - triRadius,
                    x - triRadius, y + triRadius,
                    x + triRadius, y - triRadius)
          }
        }

        if (filterWeapon == 1) {
          if (("k98" in items || "m416" in items || "scar" in items)) {
            color = BLACK
            rect(x - backgroundRadius, y - backgroundRadius, backgroundRadius * 2, backgroundRadius * 2)
            color = finalColor
            rect(x - radius, y - radius, radius * 2, radius * 2)
          } 
          else if (("m16" in items)) {
            color = BLACK
            circle(x, y, backgroundRadius * 1.2f, 10)
            color = finalColor
            circle(x, y, radius * 1.2f, 10)
          } 
          else if (("dp28" in items || "ak" in items)) {
            color = BLACK
            triangle(x - triBackRadius, y - triBackRadius,
                    x - triBackRadius, y + triBackRadius,
                    x + triBackRadius, y - triBackRadius)
            color = finalColor
            triangle(x - triRadius, y - triRadius,
                    x - triRadius, y + triRadius,
                    x + triRadius, y - triRadius)
          }
        }

        if ("4x" in items || "8x" in items || "heal" in items || "drink" in items || "bag3" in items || "helmet3" in items || "armor3" in items) {
          color = BLACK
          rect(x - backgroundRadius, y - backgroundRadius, backgroundRadius * 2, backgroundRadius * 2)
          color = finalColor
          rect(x - radius, y - radius, radius * 2, radius * 2)
        }
      }
  }
  
  fun drawPlayerInfos(players: MutableList<renderInfo>?) {
    players?.forEach {
      val (actor, x, y, _) = it
      actor!!
      val playerStateGUID = actorWithPlayerState[actor.netGUID] ?: return@forEach
      val name = playerNames[playerStateGUID] ?: return@forEach
      val teamNumber = teamNumbers[playerStateGUID] ?: 0
      val numKills = playerNumKills[playerStateGUID] ?: 0
      val (sx, sy) = Vector2(x, y).mapToWindow()
      query(name)
      if (completedPlayerInfo.containsKey(name)) {
        val info = completedPlayerInfo[name]!!
        //val desc = "$name($numKills)\n${info.win}/${info.totalPlayed}\n${info.roundMostKill}-${info.killDeathRatio.d(2)}/${info.headshotKillRatio.d(2)}\n$teamNumber"
        val desc = "$teamNumber($numKills)\n${info.killDeathRatio.d(2)} / ${info.headshotKillRatio.d(2)}"
        for(i in -1..1)
          for(j in -1..1)
            nameFontShadow.draw(spriteBatch, desc, sx + 2 + i, windowHeight - sy - 6 + j)
        nameFont.draw(spriteBatch, desc, sx + 2, windowHeight - sy - 6)
      } else {
        for(i in -1..1)
          for(j in -1..1)
            nameFontShadow.draw(spriteBatch, "$teamNumber($numKills)", sx + 2 + i, windowHeight - sy - 6 + j)
        nameFont.draw(spriteBatch, "$teamNumber($numKills)", sx + 2, windowHeight - sy - 6)
      }
    }
    
    /*
    val profileText = "${completedPlayerInfo.size}/${completedPlayerInfo.size + pendingPlayerInfo.size}"
    layout.setText(largeFont, profileText)
    for(i in -1..1)
      for(j in -1..1)
        largeFontShadow.draw(spriteBatch, profileText, windowWidth - layout.width + i, windowHeight - 10f + j)
    largeFont.draw(spriteBatch, profileText, windowWidth - layout.width, windowHeight - 10f)
    */
  }
  
  var lastPlayTime = System.currentTimeMillis()
  fun safeZoneHint() {
    if (PoisonGasWarningPosition.len() > 0) {
      val dir = PoisonGasWarningPosition.cpy().sub(selfCoords)
      val road = dir.len() - PoisonGasWarningRadius
      if (road > 0) {
        val runningTime = (road / runSpeed).toInt()
        val (x, y) = dir.nor().scl(road).add(selfCoords).mapToWindow()
        for(i in -1..1)
          for(j in -1..1)
            littleFontShadow.draw(spriteBatch, "$runningTime", x + i, windowHeight - y + j)
        littleFont.draw(spriteBatch, "$runningTime", x, windowHeight - y)
        val remainingTime = (TotalWarningDuration - ElapsedWarningDuration).toInt()
        if (remainingTime == 60 && runningTime > remainingTime) {
          val currentTime = System.currentTimeMillis()
          if (currentTime - lastPlayTime > 10000) {
            lastPlayTime = currentTime
            alarmSound.play()
          }
        }
      }
    }
  }
  
  inline fun draw(type: ShapeType, draw: ShapeRenderer.() -> Unit) {
    shapeRenderer.apply {
      begin(type)
      draw()
      end()
    }
  }
  
  inline fun paint(matrix: Matrix4, paint: SpriteBatch.() -> Unit) {
    spriteBatch.apply {
      projectionMatrix = matrix
      begin()
      paint()
      end()
    }
  }
  
  fun ShapeRenderer.circle(loc: Vector2, radius: Float, segments: Int) {
    circle(loc.x, loc.y, radius, segments)
  }
  
  fun ShapeRenderer.aimAtMe(it: renderInfo, selfX: Float, selfY: Float, currentTime: Long, zoom: Float) {
    //draw aim line
    val (actor, x, y, dir) = it
    if (isTeamMate(actor)) return
    val actorID = actor!!.netGUID
    val dirVec = dirUnitVector.cpy().rotate(dir)
    val focus = Vector2(selfX - x, selfY - y)
    val distance = focus.len()
    var aim = false
    if (distance < aimLineRange && distance > aimCircleRadius) {
      val aimAngle = focus.angle(dirVec)
      if (aimAngle.absoluteValue < asin(aimCircleRadius / distance) * MathUtils.radiansToDegrees) {//aim
        aim = true
        aimStartTime.compute(actorID) { _, startTime ->
          if (startTime == null) currentTime
          else {
            if (currentTime - startTime > aimTimeThreshold) {
              color = aimLineColor
              rectLine(x, y, selfX, selfY, aimLineWidth * zoom)
            }
            startTime
          }
        }
      }
    }
    if (!aim)
      aimStartTime.remove(actorID)
  }
  
  fun ShapeRenderer.drawPlayer(pColor: Color?, actorInfo: renderInfo, drawSight: Boolean = true) {
    val zoom = camera.zoom
    val backgroundRadius = (playerRadius + 2000f) * zoom
    val playerRadius = playerRadius * zoom
    val directionRadius = directionRadius * zoom
    
    color = BLACK
    val (actor, x, y, dir) = actorInfo
    circle(x, y, backgroundRadius, 10)
    
    color = if (isTeamMate(actor))
      teamColor
    else
      pColor
    
    circle(x, y, playerRadius, 10)
    
    if (drawSight) {
      color = sightColor
      arc(x, y, directionRadius, dir - fov / 2, fov, 10)
    }
  }


  private fun isTeamMate(actor: Actor?): Boolean {
    if (actor != null) {
      val playerStateGUID = actorWithPlayerState[actor.netGUID]
      if (playerStateGUID != null) {
        val name = playerNames[playerStateGUID] ?: return false
        if (name in team)
          return true
      }
    }
    return false
  }
  
  fun ShapeRenderer.drawVehicle(_color: Color, actorInfo: renderInfo,
                                width: Float, height: Float) {
    
    val (actor, x, y, dir) = actorInfo
    val v_x = actor!!.velocity.x
    val v_y = actor.velocity.y
    
    val dirVector = dirUnitVector.cpy().rotate(dir).scl(height / 2)
    color = BLACK
    val backVector = dirVector.cpy().nor().scl(height / 2 + 200f)
    rectLine(x - backVector.x, y - backVector.y,
             x + backVector.x, y + backVector.y, width + 400f)
    color = _color
    rectLine(x - dirVector.x, y - dirVector.y,
             x + dirVector.x, y + dirVector.y, width)
    
    if (actor.beAttached || v_x * v_x + v_y * v_y > 40) {
      color = playerColor
      circle(x, y, playerRadius * camera.zoom, 10)
    }
  }
  
  override fun resize(width: Int, height: Int) {
    windowWidth = width.toFloat()
    windowHeight = height.toFloat()
    camera.setToOrtho(true, windowWidth * windowToMapUnit, windowHeight * windowToMapUnit)
    fontCamera.setToOrtho(false, windowWidth, windowHeight)
  }
  
  override fun pause() {
  }
  
  override fun resume() {
  }
  
  override fun dispose() {
    deregister(this)
    alarmSound.dispose()
    hubFont.dispose()
    hubFontShadow.dispose()
    espFont.dispose()
    espFontShadow.dispose()
    nameFont.dispose()
    nameFontShadow.dispose()
    largeFont.dispose()
    largeFontShadow.dispose()
    littleFont.dispose()
    littleFontShadow.dispose()
    compassFont.dispose()
    compassFontShadow.dispose()
    var cur = 0
    tileZooms.forEach{
        for (i in 1..tileRowCounts[cur]) {
            val y = if (i < 10) "0$i" else "$i"
            for (j in 1..tileRowCounts[cur]) {
                val x = if (j < 10) "0$j" else "$j"
                mapErangelTiles[it]!![y]!![x]!!.dispose()
                mapMiramarTiles[it]!![y]!![x]!!.dispose()
                mapTiles[it]!![y]!![x]!!.dispose()
            }
        }
        cur++
    }
    spriteBatch.dispose()
    shapeRenderer.dispose()
  }
  
}