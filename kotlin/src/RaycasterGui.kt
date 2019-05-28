package net.razorvine.raycaster

import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import java.util.*
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.math.min


class RaycasterGui {

    companion object {
        const val PIXEL_WIDTH = 320
        const val PIXEL_HEIGHT = 200
        const val PIXEL_SCALE = 4
    }

    init {
        val image = BufferedImage(PIXEL_WIDTH, PIXEL_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val engine = RaycasterEngine(PIXEL_WIDTH, PIXEL_HEIGHT, image)
        val minimap = MinimapCanvas(engine.map, 3)
        val window = Window("Kotlin Raycaster", minimap, image, engine)

        //Code required to get the current refreshrate
        val displayRefreshRate = min(144L, GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode.refreshRate.toLong())
        println(displayRefreshRate)

        Timer("draw timer", true).scheduleAtFixedRate(DrawTask(window, minimap, engine), 10, 1000 / displayRefreshRate)
    }

    private class PixelCanvas(private val image: BufferedImage) : JPanel(true) {
        init {
            size = Dimension(PIXEL_WIDTH * PIXEL_SCALE, PIXEL_HEIGHT * PIXEL_SCALE)
            preferredSize = Dimension(PIXEL_WIDTH * PIXEL_SCALE, PIXEL_HEIGHT * PIXEL_SCALE)
        }

        override fun paint(graphics: Graphics?) {
            val gfx2d = graphics as Graphics2D
            gfx2d.background = Color.PINK
            gfx2d.color = Color.GREEN
            gfx2d.drawImage(image, 0, 0, PIXEL_WIDTH * PIXEL_SCALE, PIXEL_HEIGHT * PIXEL_SCALE, this)
        }
    }


    private class MinimapCanvas(private val map: WorldMap, var viewDistance: Int) : JPanel(true) {

        companion object {
            const val SCALE = 20
            val squareColors = mapOf(
                    0 to Color.BLACK,
                    1 to Color.BLUE,
                    2 to Color.RED,
                    3 to Color.GREEN,
                    4 to Color.MAGENTA,
                    5 to Color.YELLOW,
                    6 to Color.PINK,
                    7 to Color.ORANGE,
                    8 to Color.CYAN,
                    9 to Color.WHITE
            )
        }

        private var playerLocation = Vec2d(1.5, 1.5)
        private var playerDirection = Vec2d(0, 1)
        private var cameraPlane = Vec2d(1, 0)
        private val scrWidth = map.width * SCALE
        private val scrHeight = map.height * SCALE

        init {
            preferredSize = Dimension(scrWidth, scrHeight)
        }

        override fun paint(g: Graphics) {
            val g2 = g as Graphics2D

            // note that the Y axis of the canvas is inverted
            // draw the map
            g2.background = Color.BLACK
            g2.color = Color.YELLOW
            g2.clearRect(0, 0, width, height)
            for (x in 0 until map.width) {
                for (y in 0 until map.height) {
                    val wall = map.getWall(x, map.height - y - 1)
                    g2.color = squareColors[wall]
                    g2.fillRect(x * SCALE, y * SCALE, SCALE - 1, SCALE - 1)
                    if (Pair(x, map.height - y - 1) in map.monsters) {
                        g2.color = Color.ORANGE
                        g2.fillOval(x * SCALE + SCALE / 4, y * SCALE + SCALE / 4, SCALE / 2, SCALE / 2)
                    }
                }
            }

            // draw the camera view triangle
            val scrLocation = playerLocation * SCALE
            g2.color = Color.LIGHT_GRAY
            g2.fillOval(scrLocation.x.toInt() - SCALE / 4, scrHeight - scrLocation.y.toInt() - SCALE / 4, SCALE / 2, SCALE / 2)
            val angle = playerLocation + playerDirection * viewDistance
            val scrAngle = angle * SCALE
            g2.color = Color.DARK_GRAY
            g2.drawLine(scrLocation.x.toInt(), scrHeight - scrLocation.y.toInt(), scrAngle.x.toInt(), scrHeight - scrAngle.y.toInt())
            val poly = Polygon()
            for (vertex in listOf(
                    playerLocation,
                    playerLocation + (playerDirection + cameraPlane) * viewDistance,
                    playerLocation + (playerDirection - cameraPlane) * viewDistance)) {
                val s = vertex * SCALE
                poly.addPoint(s.x.toInt(), scrHeight - s.y.toInt())
            }
            g2.color = Color.GRAY
            g2.drawPolygon(poly)
        }

        fun movePlayer(location: Vec2d, direction: Vec2d, camera_plane: Vec2d) {
            this.playerLocation = location
            this.playerDirection = direction
            this.cameraPlane = camera_plane
        }
    }


    private class Window(title: String, val minimap: MinimapCanvas, image: BufferedImage, engine: RaycasterEngine) : JFrame(title) {
        private var frame = 0
        private val canvas = PixelCanvas(image)
        private val label = JLabel("frame counter here")

        init {
            layout = BorderLayout(0, 0)
            defaultCloseOperation = EXIT_ON_CLOSE
            label.background = Color.BLACK
            label.isOpaque = true
            label.foreground = Color.WHITE
            add(label, BorderLayout.PAGE_START)
            add(canvas, BorderLayout.CENTER)
            add(minimap, BorderLayout.PAGE_END)
            minimap.movePlayer(engine.playerPosition, engine.playerDirection, engine.cameraPlane)
            pack()
            setLocationRelativeTo(null)
            isVisible = true

            addMouseMotionListener(MouseListener(engine))
            addKeyListener(KeyListener(engine))
        }

        fun nextFrame() {
            frame++
            label.text = "frame $frame"
            canvas.repaint()
            minimap.repaint()
            Toolkit.getDefaultToolkit().sync()
        }

        class KeyListener(val engine: RaycasterEngine) : java.awt.event.KeyListener {
            override fun keyTyped(e: KeyEvent) {}

            override fun keyPressed(e: KeyEvent) {
                println("pressed: $e")  // TODO
            }

            override fun keyReleased(e: KeyEvent) {
                println("released: $e") // TODO
            }
        }

        class MouseListener(val engine: RaycasterEngine) : MouseMotionListener {
            override fun mouseDragged(e: MouseEvent) = engine.mousePos(e.x, e.y, e.xOnScreen, e.yOnScreen)
            override fun mouseMoved(e: MouseEvent) = engine.mousePos(e.x, e.y, e.xOnScreen, e.yOnScreen)
        }
    }

    private class DrawTask(private val window: Window, private val minimap: MinimapCanvas, private val engine: RaycasterEngine) : TimerTask() {
        override fun run() {
            engine.tick()
            minimap.movePlayer(engine.playerPosition, engine.playerDirection, engine.cameraPlane)
            window.nextFrame()
        }
    }
}