import org.jfree.svg.SVGGraphics2D
import java.awt.*
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.io.FileReader
import java.io.FileWriter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.reflect.typeOf


class Turtle(
        val x0: Double,
        val y0: Double,
        val theta0: Double
) {

    interface TutleControl {
        fun walk(length: Double)
        fun turn(angle: Double)
        fun direction(angle: Double)
        fun color(color: Color)
    }

    fun paint(g: SVGGraphics2D, painter: TutleControl.() -> Unit) {
        val path = Path2D.Double()
        path.moveTo(x0, y0)
        var x = x0
        var y = y0
        var theta = theta0

        val control = object : TutleControl {
            override fun walk(length: Double) {
                val dx = length * cos(theta)
                val dy = length * sin(theta)
                x += dx
                y += dy
                path.lineTo(x, y)
            }

            override fun turn(angle: Double) {
                theta += angle
            }

            override fun direction(angle: Double) {
                theta = angle
            }

            override fun color(color: Color) {
                g.draw(path)
                path.reset()
                path.moveTo(x, y)
                g.color = color
            }
        }
        control.painter()
        g.draw(path)
    }
}


class Spirograph(
        val cx: Double,
        val cy: Double,
        val r1: Double,
        val r2: Double,
        val rho: Double
) {
    fun paint(g: SVGGraphics2D) {
        val t0 = 0.0
        val step = 0.2
        val tEnd = 100

        val k = rho / r2
        val l = r2 / r1
        val omegaF = (1 - k) / k
        val x: (Double) -> Double = { t -> cx + r1 * ((1 - k) * cos(t) + l * k * cos(omegaF * t)) }
        val y: (Double) -> Double = { t -> cy + r1 * ((1 - k) * sin(t) - l * k * sin(omegaF * t)) }

        val path = Path2D.Double()
        path.moveTo(x(t0), y(t0))
        generateSequence(t0 + step, { t -> t + step })
                .takeWhile { it <= tEnd }
                .forEach { t ->
                    path.lineTo(x(t), y(t))
                }

        g.draw(path)
    }
}

fun paint(width: Int, height: Int, painter: SVGGraphics2D.() -> Unit): String {
    val g2 = SVGGraphics2D(width, height)
    g2.paint = Color.WHITE
    g2.stroke = BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g2.fillRect(0, 0, width, height)
    g2.paint = Color.RED
    g2.painter()
    return g2.svgElement
}

fun spirograph() {
    val svgElement = paint(1000, 1000) {
        val s = 1000.0
        val num = 10
        (0 until num).forEach { i ->
            (0 until num).forEach { j ->
                val cx = i * s / num + s / num / 2
                val cy = j * s / num + s / num / 2
                val r1 = 0.5 * s / num
                val r2 = r1 * 0.8 / num * (i + 1)
                val rho = r2 * 0.8 / num * (j + 1)
//                println("${i * s / 10 + s / 20} ${ j * s / 10 + s / 20}")
                Spirograph(cx, cy, r1, r2, rho).paint(this)
            }
        }
    }

    FileWriter("out/spirograph.svg").use { file ->
        file.write(svgElement)
    }
}

fun SVGGraphics2D.pointer(x0: Double, y0: Double, theta: Double, dTheta: Double, pointerLength: Double, pointerGap: Double) {
    val path = Path2D.Double()
    path.moveTo(x0, y0)
    path.lineTo(x0 - pointerLength * cos(theta + dTheta), y0 - pointerLength * sin(theta + dTheta))
    path.lineTo(x0 - (pointerLength - pointerGap) * cos(theta), y0 - (pointerLength - pointerGap) * sin(theta))
    path.lineTo(x0 - pointerLength * cos(theta - dTheta), y0 - pointerLength * sin(theta - dTheta))
    path.lineTo(x0, y0)
    fill(path)
}

fun SVGGraphics2D.colorScale(x0: Double, y0: Double, numberOfSteps: Int, width: Double, height: Double, makeColor: (Float) -> Color) {
    (0 until numberOfSteps).forEach { index ->
        val t = index.toDouble() / (numberOfSteps - 1)
        val w = width / numberOfSteps
        withColor(makeColor(t.toFloat())) {
            fill(Rectangle2D.Double(x0 - width / 2 + width * t - w / 2, y0, w, height))
        }
    }
}

fun SVGGraphics2D.drawCenteredString(text: String, x0: Double, y0: Double) {
    drawString(
            text,
            x0.toFloat() - fontMetrics.stringWidth(text).toFloat() / 2,
            y0.toFloat() + (fontMetrics.ascent - fontMetrics.descent) / 2
    )
}

fun SVGGraphics2D.withColor(color: Color, action: SVGGraphics2D.() -> Unit) {
    val previousColor = this.color
    this.color = color
    action()
    this.color = previousColor
}

fun SVGGraphics2D.drawLegendCircle(x0: Double, y0: Double, unitSize: Double, makeDirection: (Int) -> Double) {
    withColor(Color.getHSBColor(0.0f, 0.0f, 0.7f)) {
        val fontSize = 26
        font = Font("Fira Code", Font.PLAIN, fontSize)
        (0 until 10).forEach { digit ->
            draw(Line2D.Double(
                    x0 + unitSize * cos(makeDirection(digit)),
                    y0 + unitSize * sin(makeDirection(digit)),
                    x0 + 3 * unitSize * cos(makeDirection(digit)),
                    y0 + 3 * unitSize * sin(makeDirection(digit))
            ))
            drawCenteredString(
                    digit.toString(),
                    x0 + 4 * unitSize * cos(makeDirection(digit)),
                    y0 + 4 * unitSize * sin(makeDirection(digit))
            )
        }
    }
}

fun pi() {

    val digits = FileReader("resources/pi100000.txt").useLines {
        it.first().toCharArray().take(100_1000).map { c -> c - '0' }
    }

    val width = 2100
    val height = 2970
    val svgElement = paint(width, height) {

        val makeColor: (Float) -> Color = { t ->
            Color.getHSBColor((0.8f * (t + 0.65f)) % 1.0f, 1.0f, 0.8f)
        }
        val makeDirection: (Int) -> Double = { digit ->
            digit * 2 * PI / 10 - PI / 2
        }

        val backgroundMargin = 50
        font = Font("Fira Code", Font.PLAIN, 8)
        val numDigitsPerLine = 400
        withColor(Color.getHSBColor(0.0f, 0.0f, 0.8f)) {
            digits.chunked(numDigitsPerLine).forEachIndexed { index, lineDigits ->
                val line = lineDigits.chunked(numDigitsPerLine / 8).joinToString(" ") { it.joinToString("") }
                val x = width.toDouble() / 2 - 15
                val y = backgroundMargin + index * (height - 2 * backgroundMargin).toDouble() / (digits.size / numDigitsPerLine - 1)
                if (index == 0) {
                    drawString(
                            "3.",
                            (x - fontMetrics.stringWidth(line) / 2 - fontMetrics.stringWidth("3.")).toFloat(),
                            y.toFloat() + (fontMetrics.ascent - fontMetrics.descent) / 2
                    )
                }
                drawCenteredString(line, x, y)
            }
        }

        drawLegendCircle(
                width.toDouble() / 2,
                0.87 * height.toDouble(),
                40.0,
                makeDirection
        )

        val legendWidth = 500.0
        val legendHeight = 8.0
        val lx = width.toDouble() / 2
        val ly = height - legendHeight - 2.5 * backgroundMargin
        colorScale(lx, ly, 100, legendWidth, legendHeight, makeColor)

        val legendFontSize = 20
        font = Font("Fira Code", Font.PLAIN, legendFontSize)
        color = Color.getHSBColor(0.0f, 0.0f, 0.7f)

        val pointerLength = 20.0
        val pointerGap = 8.0
        val dTheta = PI / 10
        pointer(
                lx - legendWidth / 2 - legendFontSize,
                ly + legendHeight / 2,
                0.0,
                dTheta,
                1.5 * pointerLength,
                1.5 * pointerGap
        )

        color = Color.getHSBColor(0.0f, 0.0f, 0.4f)
        font = Font("Nimbus Roman", Font.ITALIC, 600)
        val textTl = TextLayout("π", font, fontRenderContext)
        val transform = AffineTransform().apply {
            translate(
                    width.toDouble() / 2 - fontMetrics.stringWidth("π") / 2,
                    300.0 + backgroundMargin + (fontMetrics.ascent - fontMetrics.descent) / 2
            )
        }
        draw(textTl.getOutline(transform))

        color = Color.getHSBColor(0.0f, 0.0f, 0.4f)
        val topFont = 26
        font = Font("Nimbus Roman", Font.PLAIN, topFont * 2)
        drawCenteredString("1 0 0 , 0 0 0", width.toDouble() / 2, backgroundMargin + 60.0)
        color = Color.getHSBColor(0.0f, 0.0f, 0.5f)
        font = Font("Nimbus Roman", Font.ITALIC, topFont)
        drawCenteredString("digits of", width.toDouble() / 2, backgroundMargin + 60.0 + 1.8 * topFont)

//        val pp = Path2D.Double()
//        pp.moveTo(300.0, 300.0)
//        pp.append(textTl.getOutline(null), true)
//        fill(textTl.getOutline(transform))

//        val glyphVector = font.createGlyphVector(fontRenderContext, "H")
//        val glyphShape = glyphVector.getGlyphOutline(0)
//        draw(glyphShape)

        val x0 = width.toDouble() / 2 + 180
        val y0 = height.toDouble() / 2 + backgroundMargin - 600
        val theta0 = makeDirection(digits.first())

        color = Color.getHSBColor(0.0f, 0.0f, 0.4f)
        pointer(x0, y0, theta0, dTheta, pointerLength, pointerGap)

        Turtle(x0, y0, theta0).paint(this) {
            color(makeColor(0.0f))
            digits.drop(1).forEachIndexed { index, digit ->
                if (index % 1000 == 0) {
                    color(makeColor(index.toFloat() / digits.size))
                }
                walk(5.2)
                direction(makeDirection(digit))
            }
        }
    }


    FileWriter("out/turtle.svg").use { file ->
        file.write(svgElement)
    }
}

fun main() {
    spirograph()
    pi()
}