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
// Halbkreisförmiger Fortschrittsring (sauber, schlicht)
// ---------------------------------------------------------------------------

@Composable
private fun HydrationRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label         = "ringProgress"
    )

    Canvas(modifier = modifier) {
        val strokePx = RING_STROKE.toPx()
        val glowPx   = RING_GLOW_STROKE.toPx()
        val inset    = glowPx / 2f
        val arcRect  = Rect(
            offset = Offset(inset, inset),
            size   = Size(size.width - glowPx, size.height - glowPx)
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

        // ---- Fortschrittsbogen mit Gradient ----
        if (animatedProgress > 0f) {
            // sweepGradient startet bei 0° (rechts), unser Bogen bei 150° (GAUGE_START_ANGLE).
            // Farbstops müssen auf die reale Bogenposition normiert werden:
            //   Bogenstart = 150° → 150/360 = 0.417
            //   Bogenende  = 150°+240° = 390° → 390/360 = 1.083 → wraps zu 0.083
            // Da sweepGradient 0→1 = 0°→360°: Start bei ~0.42, Ende bei ~0.08 (wrap).
            val startFrac = GAUGE_START_ANGLE / 360f                        // 0.417
            val endFrac   = (GAUGE_START_ANGLE + GAUGE_SWEEP) / 360f % 1f   // 0.083

            drawArc(
                brush      = Brush.sweepGradient(
                    // Vor dem Bogen: Startfarbe (damit der Anfang bei 150° stimmt)
                    0.0f      to RingProgressEnd,
                    endFrac   to RingProgressEnd,     // 0→30° = Ende des Bogens (dunkles Blau)
                    startFrac to RingProgressStart,   // 150° = Anfang des Bogens (helles Aqua)
                    0.8f      to RingProgressEnd,     // Mitte → dunkel
                    1.0f      to RingProgressEnd      // wrap zurück
                ),
                startAngle = GAUGE_START_ANGLE,
                sweepAngle = GAUGE_SWEEP * animatedProgress,
                useCenter  = false,
                topLeft    = arcRect.topLeft,
                size       = arcRect.size,
                style      = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
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
            // Hintergrund: bei fast vollem Tropfen direkt Wasserfarbe,
            // sonst dunkler Hintergrund – verhindert sichtbare dunkle Lücken
            val bgColor = if (animatedFill >= 0.97f) WaterBottom else DropBackground
            drawPath(path = dropPath, color = bgColor)

            // waterTopY: um 3× die Amplitude nach oben verschieben, damit
            // auch bei hohem Füllstand die Welle den Tropfen vollständig bedeckt.
            // Bei 0 % kein Offset, damit kein Wasser-Streifen sichtbar ist.
            val maxAmplitude    = h * 0.028f
            val amplitudeOffset = if (animatedFill > 0f) maxAmplitude * 3f else 0f
            val waterTopY       = h * (1f - animatedFill) - amplitudeOffset

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

        // Glanz-Highlight oben-links im runden Teil des Tropfens
        drawCircle(
            color  = Color.White.copy(alpha = 0.18f),
            radius = w * 0.055f,
            center = Offset(w * 0.40f, h * 0.13f)
        )
        // Zweiter, kleinerer Glanzpunkt – subtiler Glasreflex
        drawCircle(
            color  = Color.White.copy(alpha = 0.09f),
            radius = w * 0.030f,
            center = Offset(w * 0.36f, h * 0.21f)
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
    val steps = 80

    // Starte unten-links, gehe hoch zur Wellenlinie
    moveTo(0f, h)
    lineTo(0f, waterTopY)

    // Zeichne die Sinuswelle von links nach rechts
    // WICHTIG: kein moveTo im Loop – sonst reißt der Pfad ab
    for (i in 0..steps) {
        val x = w * i / steps
        val y = waterTopY + amplitude * sin(
            frequency * 2f * PI.toFloat() * (i.toFloat() / steps) + phaseShift
        )
        lineTo(x, y)
    }

    // Schließe den Pfad: rechts runter zur Unterkante, dann zurück
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
// Tropfen-Path – klassische Wassertropfen-Silhouette (Spitze unten)
// ---------------------------------------------------------------------------

/**
 * Erzeugt eine klassische Wassertropfen-Silhouette:
 *  - Spitze zeigt nach unten (wie ein echter fallender Tropfen)
 *  - Oberseite: breite, gleichmäßige Halbkreis-Wölbung
 *  - Untere Flanken: fließend und symmetrisch in eine weiche Spitze laufend
 *  - Streng symmetrisch, nur 2 kubische Segmente pro Seite
 *  - Proportionen: Breite ≈ 65 % der Höhe → klassisches, harmonisches Verhältnis
 */
private fun buildDropPath(w: Float, h: Float): Path = Path().apply {
    val cx = w / 2f

    // Schlanker Tropfen: Kreis ist deutlich schmaler als das Canvas
    // Breite des Kreises ≈ 64 % der Canvas-Breite (war vorher fast 92 %)
    val circleRadius  = w * 0.32f      // schlanker Kreis – echter Tropfen-Look
    val circleCenterY = h * 0.35f      // Kreismitte etwas höher → mehr Platz für Spitze

    // Spitze ganz unten
    val tipY = h * 0.965f

    // Schultern: links/rechts auf Höhe des Kreismittelpunkts
    val shoulderX = circleRadius
    val shoulderY = circleCenterY

    // Flanken-Kontrollpunkte (Schulter → Spitze):
    // cp1 leicht breiter als Schulter → sanfter Bauch, aber schmal
    // cp2 zieht eng zur Mitte → klarer Einzug zur Spitze
    val cp1X = w * 0.33f
    val cp1Y = h * 0.63f
    val cp2X = w * 0.12f
    val cp2Y = h * 0.88f

    moveTo(cx - shoulderX, shoulderY)

    // Linke Flanke → Spitze
    cubicTo(
        cx - cp1X, cp1Y,
        cx - cp2X, cp2Y,
        cx,        tipY
    )

    // Rechte Flanke ← Spitze
    cubicTo(
        cx + cp2X, cp2Y,
        cx + cp1X, cp1Y,
        cx + shoulderX, shoulderY
    )

    // Oberer Halbkreis – 2-Segment-Bézier-Approximation (k ≈ 0.5523 * r)
    val k = circleRadius * 0.5523f

    cubicTo(
        cx + circleRadius, circleCenterY - k,
        cx + k,            circleCenterY - circleRadius,
        cx,                circleCenterY - circleRadius
    )
    cubicTo(
        cx - k,            circleCenterY - circleRadius,
        cx - circleRadius, circleCenterY - k,
        cx - shoulderX,    shoulderY
    )

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
