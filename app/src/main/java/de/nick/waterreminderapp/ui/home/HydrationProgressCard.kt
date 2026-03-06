package de.nick.waterreminderapp.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Design-Konstanten
// ---------------------------------------------------------------------------

private val CardBackground    = Color(0xFF151F2E)   // leicht dunkler als vorher
private val RingTrackColor    = Color(0xFF1E2E40)
private val RingProgressStart = Color(0xFF00D4FF)   // kräftigeres Aqua
private val RingProgressEnd   = Color(0xFF0088CC)
private val RingGlow          = Color(0xFF00AADD)   // für den Innen-Glüheffekt
private val DropBackground    = Color(0xFF152030)
private val DropOutline       = Color(0xFF2A7A9A)   // subtil, gedämpft, nicht neon
private val WaterTop          = Color(0xFF4DD8EE)
private val WaterBottom       = Color(0xFF0096C7)
private val TextPrimary       = Color(0xFFEAF6FF)
private val TextSecondary     = Color(0xFF7AAFC8)
private val TextLabel         = Color(0xFF4A7A96)   // dezentes Label "Heute"

// Gauge-Bogen: 240° breiter Halbkreis-Gauge (startet links-unten, endet rechts-unten)
private const val GAUGE_START_ANGLE = 150f
private const val GAUGE_SWEEP       = 240f

private val RING_STROKE         = 20.dp
private val RING_GLOW_STROKE    = 36.dp   // weicher Halo dahinter
private val DROP_SIZE           = 112.dp
private val RING_SIZE           = 280.dp
private val CARD_CORNER         = 32.dp
private val CARD_PADDING_V      = 36.dp
private val CARD_PADDING_H      = 28.dp

// ---------------------------------------------------------------------------
// Fortschrittslogik (rein, testbar)
// ---------------------------------------------------------------------------

/**
 * Berechnet den Hydrations-Fortschritt als Float im Bereich [0, 1].
 *
 * Edge Cases:
 * - goalMl <= 0  → 0f (kein Ziel definiert)
 * - totalMl <= 0 → 0f (negativer Wert unmöglich)
 * - totalMl > goalMl → 1f (Ziel erreicht / überschritten)
 */
internal fun calculateProgress(totalMl: Int, goalMl: Int): Float {
    if (goalMl <= 0 || totalMl <= 0) return 0f
    return (totalMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)
}

// ---------------------------------------------------------------------------
// Öffentliches Composable
// ---------------------------------------------------------------------------

/**
 * Zeigt den Hydrations-Fortschritt als dunkle Card mit breitem Gauge-Ring (240°)
 * und animiertem Wassertropfen.
 *
 * @param totalMl  Bereits getrunkene Menge in ml
 * @param goalMl   Tagesziel in ml
 * @param modifier Optionaler Modifier
 */
@Composable
fun HydrationProgressCard(
    totalMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier
) {
    val progress = calculateProgress(totalMl, goalMl)

    Surface(
        modifier        = modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(CARD_CORNER),
        color           = CardBackground,
        tonalElevation  = 0.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier            = Modifier.padding(vertical = CARD_PADDING_V, horizontal = CARD_PADDING_H),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            HydrationGauge(
                progress = progress,
                ringSize = RING_SIZE,
                dropSize = DROP_SIZE
            )

            Spacer(Modifier.height(20.dp))

            ProgressValueText(totalMl = totalMl, goalMl = goalMl, progress = progress)
        }
    }
}

// ---------------------------------------------------------------------------
// Gauge: Ring + Tropfen übereinander
// ---------------------------------------------------------------------------

@Composable
private fun HydrationGauge(
    progress: Float,
    ringSize: Dp,
    dropSize: Dp
) {
    // Höhe: bei 240°-Bogen (150° → 390°) reicht der Bogen von oben links nach oben rechts
    // und geht unten durch → Scheitelpunkt oben. 75 % Höhe ist gut für diesen Bogen.
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier.size(ringSize, ringSize * 0.75f)
    ) {
        HydrationRing(
            progress = progress,
            modifier = Modifier.size(ringSize)
        )
        WaterDrop(
            fillFraction = progress,
            modifier     = Modifier
                .size(dropSize)
                .align(Alignment.Center)
        )
    }
}

// ---------------------------------------------------------------------------
// Halbkreisförmiger Fortschrittsring (Gauge, 240°)
// ---------------------------------------------------------------------------

@Composable
private fun HydrationRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label         = "ringProgress"
    )

    Canvas(modifier = modifier) {
        val strokePx     = RING_STROKE.toPx()
        val glowPx       = RING_GLOW_STROKE.toPx()
        val inset        = glowPx / 2f   // Inset nach dem größten Stroke richten
        val arcRect      = Rect(
            offset = Offset(inset, inset),
            size   = Size(size.width - glowPx, size.height - glowPx)
        )

        // ---- Halo / Glüh-Track (sehr soft, transparent) ----
        drawArc(
            brush      = Brush.sweepGradient(
                0.0f to RingGlow.copy(alpha = 0.0f),
                0.5f to RingGlow.copy(alpha = 0.06f),
                1.0f to RingGlow.copy(alpha = 0.0f)
            ),
            startAngle = GAUGE_START_ANGLE,
            sweepAngle = GAUGE_SWEEP,
            useCenter  = false,
            topLeft    = arcRect.topLeft,
            size       = arcRect.size,
            style      = Stroke(width = glowPx, cap = StrokeCap.Round)
        )

        // ---- Track (Hintergrundring) ----
        drawArc(
            color      = RingTrackColor,
            startAngle = GAUGE_START_ANGLE,
            sweepAngle = GAUGE_SWEEP,
            useCenter  = false,
            topLeft    = arcRect.topLeft,
            size       = arcRect.size,
            style      = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // ---- Fortschritts-Bogen mit Gradient ----
        if (animatedProgress > 0f) {
            drawArc(
                brush      = Brush.sweepGradient(
                    0.0f  to RingProgressStart,
                    0.65f to RingProgressEnd,
                    1.0f  to RingProgressEnd
                ),
                startAngle = GAUGE_START_ANGLE,
                sweepAngle = GAUGE_SWEEP * animatedProgress,
                useCenter  = false,
                topLeft    = arcRect.topLeft,
                size       = arcRect.size,
                style      = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // ---- Leuchtender Dot am Fortschritts-Ende ----
            val endAngleRad = Math.toRadians(
                (GAUGE_START_ANGLE + GAUGE_SWEEP * animatedProgress).toDouble()
            ).toFloat()
            val cx    = size.width  / 2f
            val cy    = size.height / 2f
            val r     = arcRect.width / 2f
            val dotX  = cx + r * cos(endAngleRad)
            val dotY  = cy + r * sin(endAngleRad)

            // Äußeres Leuchten
            drawCircle(
                color  = RingProgressStart.copy(alpha = 0.35f),
                radius = strokePx * 0.9f,
                center = Offset(dotX, dotY)
            )
            // Innerer heller Kern
            drawCircle(
                color  = Color.White.copy(alpha = 0.85f),
                radius = strokePx * 0.28f,
                center = Offset(dotX, dotY)
            )
        }

        // ---- Dot am Start-Ende (immer sichtbar als Anker) ----
        val startAngleRad = Math.toRadians(GAUGE_START_ANGLE.toDouble()).toFloat()
        val cx   = size.width  / 2f
        val cy   = size.height / 2f
        val r    = arcRect.width / 2f
        val sx   = cx + r * cos(startAngleRad)
        val sy   = cy + r * sin(startAngleRad)
        drawCircle(
            color  = RingTrackColor,
            radius = strokePx * 0.38f,
            center = Offset(sx, sy)
        )
    }
}

// ---------------------------------------------------------------------------
// Wassertropfen mit animiertem Füllstand + Wellenbewegung
// ---------------------------------------------------------------------------

@Composable
internal fun WaterDrop(
    fillFraction: Float,
    modifier: Modifier = Modifier
) {
    val animatedFill by animateFloatAsState(
        targetValue   = fillFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label         = "waterFill"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "waterWave")

    val wavePhase by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing)
        ),
        label = "wavePhase"
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue  = PI.toFloat(),
        targetValue   = (3f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing)
        ),
        label = "wavePhase2"
    )

    val bubbleRise by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "bubbleRise"
    )

    Canvas(modifier = modifier) {
        val w        = size.width
        val h        = size.height
        val dropPath = buildDropPath(w, h)

        clipPath(dropPath) {
            // Dunkler Tropfen-Hintergrund
            drawPath(path = dropPath, color = DropBackground)

            val waterTopY = h * (1f - animatedFill)

            // Welle 1 – helle Vorderwelle
            drawPath(
                path  = buildWavePath(
                    w          = w,
                    h          = h,
                    waterTopY  = waterTopY,
                    amplitude  = h * 0.028f,
                    frequency  = 1.3f,
                    phaseShift = wavePhase
                ),
                brush = Brush.verticalGradient(
                    colors = listOf(WaterTop.copy(alpha = 0.92f), WaterBottom),
                    startY = waterTopY,
                    endY   = h
                )
            )

            // Welle 2 – dunklere Hinterwelle
            drawPath(
                path  = buildWavePath(
                    w          = w,
                    h          = h,
                    waterTopY  = waterTopY + h * 0.022f,
                    amplitude  = h * 0.020f,
                    frequency  = 1.1f,
                    phaseShift = wavePhase2
                ),
                brush = Brush.verticalGradient(
                    colors = listOf(WaterBottom.copy(alpha = 0.65f), WaterBottom),
                    startY = waterTopY,
                    endY   = h
                )
            )

            // Dezente innere Reflexion (weißer Schimmer oben im Wasser)
            if (animatedFill > 0.05f) {
                drawRect(
                    brush   = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                        startY = waterTopY,
                        endY   = waterTopY + h * 0.12f
                    ),
                    topLeft = Offset(0f, waterTopY),
                    size    = Size(w, h * 0.12f)
                )
            }

            // Blasen
            if (animatedFill > 0.2f) {
                drawBubbles(w = w, h = h, waterTopY = waterTopY, riseOffset = bubbleRise)
            }
        }

        // Tropfen-Umriss – sauber und subtil
        drawPath(
            path  = dropPath,
            color = DropOutline,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Sanfter äußerer Glow – sehr dezent
        drawPath(
            path  = dropPath,
            color = DropOutline.copy(alpha = 0.08f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Glanz-Highlight (oben links im Tropfenkopf)
        drawCircle(
            color  = Color.White.copy(alpha = 0.15f),
            radius = w * 0.065f,
            center = Offset(w * 0.37f, h * 0.18f)
        )
        // Zweiter, kleinerer Glanzpunkt – wirkt wie Glasreflex
        drawCircle(
            color  = Color.White.copy(alpha = 0.08f),
            radius = w * 0.035f,
            center = Offset(w * 0.33f, h * 0.26f)
        )
    }
}

// ---------------------------------------------------------------------------
// Wellen-Path (Sinus-Kurve)
// ---------------------------------------------------------------------------

private fun buildWavePath(
    w: Float,
    h: Float,
    waterTopY: Float,
    amplitude: Float,
    frequency: Float,
    phaseShift: Float
): Path = Path().apply {
    val steps = 80   // mehr Schritte = glattere Kurve
    moveTo(0f, h)
    lineTo(0f, waterTopY)

    for (i in 0..steps) {
        val x = w * i / steps
        val y = waterTopY + amplitude * sin(
            frequency * 2f * PI.toFloat() * (i.toFloat() / steps) + phaseShift
        )
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }

    lineTo(w, h)
    close()
}

// ---------------------------------------------------------------------------
// Blasen
// ---------------------------------------------------------------------------

private fun DrawScope.drawBubbles(
    w: Float,
    h: Float,
    waterTopY: Float,
    riseOffset: Float
) {
    data class Bubble(val xFrac: Float, val depthFrac: Float, val radius: Float)

    val bubbles = listOf(
        Bubble(0.28f, 0.75f, w * 0.025f),
        Bubble(0.60f, 0.55f, w * 0.018f),
        Bubble(0.45f, 0.85f, w * 0.015f),
        Bubble(0.72f, 0.65f, w * 0.022f)
    )

    val waterHeight = h - waterTopY
    if (waterHeight <= 0f) return

    for (b in bubbles) {
        val bx   = w * b.xFrac
        val rawY = waterTopY + waterHeight * b.depthFrac - waterHeight * riseOffset
        val by   = waterTopY + ((rawY - waterTopY).mod(waterHeight))

        if (by > waterTopY + b.radius && by < h - b.radius) {
            drawCircle(
                color  = Color.White.copy(alpha = 0.14f),
                radius = b.radius,
                center = Offset(bx, by),
                style  = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tropfen-Path – natürliche, weiche Wassertropfen-Silhouette
// ---------------------------------------------------------------------------

/**
 * Erzeugt einen stilisierten Wassertropfen via kubischer Bézier-Kurven.
 *
 * Neue Silhouette:
 *  - streng symmetrisch um die Vertikalachse
 *  - oben weich gerundet, volle Schulter
 *  - sanft bauchig im Mittelteil, kompakte Proportion
 *  - kurze, weiche Spitze ohne harte V-Form
 */
private fun buildDropPath(w: Float, h: Float): Path = Path().apply {
    val cx = w / 2f

    // Vertikale Leitpunkte für eine kompakte, symmetrische Silhouette
    val capY      = h * 0.10f
    val shoulderY = h * 0.26f
    val bellyY    = h * 0.56f
    val waistY    = h * 0.80f
    val tipY      = h * 0.96f

    // Halbbreiten an den Leitpunkten
    val rCap      = w * 0.18f
    val rShoulder = w * 0.37f
    val rBelly    = w * 0.47f
    val rWaist    = w * 0.23f
    val rTip      = w * 0.045f

    data class Cubic(val c1: Offset, val c2: Offset, val end: Offset)
    fun Offset.mirrorX(centerX: Float) = Offset(2f * centerX - x, y)
    fun mirror(segment: Cubic) = Cubic(
        c1  = segment.c1.mirrorX(cx),
        c2  = segment.c2.mirrorX(cx),
        end = segment.end.mirrorX(cx)
    )

    val right = listOf(
        // Scheitel → Schulter: runde Kappe mit breiter Schulter
        Cubic(
            Offset(cx + rCap, capY),
            Offset(cx + rShoulder * 1.06f, capY + (shoulderY - capY) * 0.34f),
            Offset(cx + rShoulder, shoulderY)
        ),
        // Schulter → Bauch: weicher Übergang in die breiteste Zone
        Cubic(
            Offset(cx + rShoulder * 1.08f, shoulderY + (bellyY - shoulderY) * 0.18f),
            Offset(cx + rBelly * 1.02f, shoulderY + (bellyY - shoulderY) * 0.84f),
            Offset(cx + rBelly, bellyY)
        ),
        // Bauch → Taille: sanftes Einziehen Richtung Spitze
        Cubic(
            Offset(cx + rBelly * 0.98f, bellyY + (waistY - bellyY) * 0.32f),
            Offset(cx + rWaist * 1.18f, waistY - (waistY - bellyY) * 0.14f),
            Offset(cx + rWaist, waistY)
        ),
        // Taille → Spitze: kompakter, klarer Abschluss ohne harte V-Form
        Cubic(
            Offset(cx + rWaist * 0.82f, waistY + (tipY - waistY) * 0.26f),
            Offset(cx + rTip * 1.70f, tipY - (tipY - waistY) * 0.08f),
            Offset(cx, tipY)
        )
    )

    moveTo(cx, capY)
    right.forEach { s -> cubicTo(s.c1.x, s.c1.y, s.c2.x, s.c2.y, s.end.x, s.end.y) }
    right.asReversed().map(::mirror)
        .forEach { s -> cubicTo(s.c1.x, s.c1.y, s.c2.x, s.c2.y, s.end.x, s.end.y) }

    close()
}

// ---------------------------------------------------------------------------
// Texte: Label + Menge + Ziel + Prozent
// ---------------------------------------------------------------------------

@Composable
private fun ProgressValueText(
    totalMl: Int,
    goalMl: Int,
    progress: Float
) {
    val percent = (progress * 100f).toInt()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Dezentes Label
        Text(
            text       = "Heute getrunken",
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            color      = TextLabel,
            letterSpacing = 1.5.sp
        )

        Spacer(Modifier.height(4.dp))

        // Große Zahl
        Text(
            text       = formatMl(totalMl),
            fontSize   = 52.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = TextPrimary,
            lineHeight = 56.sp
        )

        // Ziel + Prozent in einer Zeile
        Text(
            text     = "/ ${formatMl(goalMl)} mL  ·  $percent %",
            fontSize = 14.sp,
            color    = TextSecondary
        )
    }
}

// ---------------------------------------------------------------------------
// Hilfsfunktion: ml formatieren (1250 → "1.250")
// ---------------------------------------------------------------------------

private fun formatMl(ml: Int): String =
    String.format(Locale.GERMAN, "%,d", ml)

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "0 % – Leer", showBackground = true, backgroundColor = 0xFF0D1620)
@Composable
private fun PreviewEmpty() {
    HydrationProgressCard(totalMl = 0, goalMl = 2500, modifier = Modifier.padding(16.dp))
}

@Preview(name = "25 % – Wenig", showBackground = true, backgroundColor = 0xFF0D1620)
@Composable
private fun PreviewQuarter() {
    HydrationProgressCard(totalMl = 625, goalMl = 2500, modifier = Modifier.padding(16.dp))
}

@Preview(name = "50 % – Halb", showBackground = true, backgroundColor = 0xFF0D1620)
@Composable
private fun PreviewHalf() {
    HydrationProgressCard(totalMl = 1250, goalMl = 2500, modifier = Modifier.padding(16.dp))
}

@Preview(name = "75 % – Gut dabei", showBackground = true, backgroundColor = 0xFF0D1620)
@Composable
private fun PreviewThreeQuarters() {
    HydrationProgressCard(totalMl = 1875, goalMl = 2500, modifier = Modifier.padding(16.dp))
}

@Preview(name = "100 % – Ziel erreicht", showBackground = true, backgroundColor = 0xFF0D1620)
@Composable
private fun PreviewFull() {
    HydrationProgressCard(totalMl = 2500, goalMl = 2500, modifier = Modifier.padding(16.dp))
}

@Preview(name = "Überschritten – Clamp-Test", showBackground = true, backgroundColor = 0xFF0D1620)
@Composable
private fun PreviewOverflow() {
    HydrationProgressCard(totalMl = 9999, goalMl = 2500, modifier = Modifier.padding(16.dp))
}
