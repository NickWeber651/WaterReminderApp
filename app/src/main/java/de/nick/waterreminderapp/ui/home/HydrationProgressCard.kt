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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Farben
// ---------------------------------------------------------------------------

private val CardBackground    = Color(0xFF1A2332)
private val RingTrackColor    = Color(0xFF263445)
private val RingProgressStart = Color(0xFF00C2FF)
private val RingProgressEnd   = Color(0xFF0077B6)
private val DropOutline       = Color(0xFF00B4D8)
private val WaterTop          = Color(0xFF48CAE4)
private val WaterBottom       = Color(0xFF0096C7)
private val TextPrimary       = Color(0xFFE8F4FD)
private val TextSecondary     = Color(0xFF90B4CE)

// ---------------------------------------------------------------------------
// Fortschrittslogik (rein, testbar)
// ---------------------------------------------------------------------------

/**
 * Berechnet den Hydrations-Fortschritt als Float im Bereich [0, 1].
 *
 * Edge Cases:
 * - goalMl <= 0  → 0f (kein Ziel definiert)
 * - totalMl < 0  → 0f (negativer Wert unmöglich)
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
 * Zeigt den Hydrations-Fortschritt als dunkle Card mit halbkreisförmigem Gauge-Ring
 * und einem gefüllten Wassertropfen.
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
        modifier      = modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(28.dp),
        color         = CardBackground,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier            = Modifier.padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Gauge + Tropfen übereinander im selben Box-Stack
            HydrationGauge(
                progress = progress,
                ringSize = 260.dp,
                dropSize = 100.dp
            )

            Spacer(Modifier.height(16.dp))

            ProgressValueText(totalMl = totalMl, goalMl = goalMl)
        }
    }
}

// ---------------------------------------------------------------------------
// Gauge: halbkreisförmiger Ring + Tropfen zentriert
// ---------------------------------------------------------------------------

@Composable
private fun HydrationGauge(
    progress: Float,
    ringSize: Dp,
    dropSize: Dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier.size(ringSize, ringSize * 0.65f) // Gauge ist ein Halbbogen
    ) {
        HydrationRing(
            progress = progress,
            modifier = Modifier.size(ringSize)
        )

        // Tropfen leicht nach unten verschoben, damit er mittig im Bogen sitzt
        WaterDrop(
            fillFraction = progress,
            modifier     = Modifier
                .size(dropSize)
                .align(Alignment.BottomCenter)
        )
    }
}

// ---------------------------------------------------------------------------
// Halbkreisförmiger Fortschrittsring (Gauge)
// ---------------------------------------------------------------------------

@Composable
private fun HydrationRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    // Weiche Animation: beim ersten Einblenden und bei Wertänderungen
    val animatedProgress by animateFloatAsState(
        targetValue  = progress,
        animationSpec = tween(
            durationMillis = 800,
            easing         = FastOutSlowInEasing
        ),
        label = "ringProgress"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 18.dp.toPx()
        val inset       = strokeWidth / 2f
        val arcRect     = Rect(
            offset = Offset(inset, inset),
            size   = Size(size.width - strokeWidth, size.height - strokeWidth)
        )

        // Bogen-Parameter: startet bei 210°, geht 120° (links-unten → rechts-unten, als Halbkreis-Gauge)
        val startAngle  = 210f
        val sweepTotal  = 120f

        // Track (Hintergrundring)
        drawArc(
            color      = RingTrackColor,
            startAngle = startAngle,
            sweepAngle = sweepTotal,
            useCenter  = false,
            topLeft    = arcRect.topLeft,
            size       = arcRect.size,
            style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Fortschritts-Bogen
        if (animatedProgress > 0f) {
            drawArc(
                brush      = Brush.sweepGradient(
                    0.0f to RingProgressStart,
                    1.0f to RingProgressEnd
                ),
                startAngle = startAngle,
                sweepAngle = sweepTotal * animatedProgress,
                useCenter  = false,
                topLeft    = arcRect.topLeft,
                size       = arcRect.size,
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Wassertropfen mit animiertem Füllstand
// ---------------------------------------------------------------------------

@Composable
internal fun WaterDrop(
    fillFraction: Float,
    modifier: Modifier = Modifier
) {
    // Wasserstand: weich animieren wenn fillFraction sich ändert
    val animatedFill by animateFloatAsState(
        targetValue   = fillFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label         = "waterFill"
    )

    // Endlos-Animation für die Wellenbewegung
    val infiniteTransition = rememberInfiniteTransition(label = "waterWave")

    // Phasen-Offset für die erste Welle (volle Periode in 2,4 s)
    val wavePhase by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing)
        ),
        label = "wavePhase"
    )

    // Zweite Welle etwas versetzt – gibt Tiefenwirkung
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue  = (PI).toFloat(),
        targetValue   = (3f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing)
        ),
        label = "wavePhase2"
    )

    // Blasen-Offset: treibt dezent nach oben (0→1 in ~4 s)
    val bubbleRise by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "bubbleRise"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val dropPath = buildDropPath(w, h)

        // Clip auf Tropfen-Form
        clipPath(dropPath) {
            // Dunkler Hintergrund
            drawPath(path = dropPath, color = Color(0xFF1E3A50))

            val waterTopY = h * (1f - animatedFill)

            // ---- Welle 1 (vordere, helle Welle) ----
            val wavePath1 = buildWavePath(
                w          = w,
                h          = h,
                waterTopY  = waterTopY,
                amplitude  = h * 0.04f,
                frequency  = 1.5f,
                phaseShift = wavePhase
            )
            drawPath(
                path  = wavePath1,
                brush = Brush.verticalGradient(
                    colors = listOf(WaterTop.copy(alpha = 0.9f), WaterBottom),
                    startY = waterTopY,
                    endY   = h
                )
            )

            // ---- Welle 2 (hintere, dunklere Welle, leicht versetzt) ----
            val wavePath2 = buildWavePath(
                w          = w,
                h          = h,
                waterTopY  = waterTopY + h * 0.025f,
                amplitude  = h * 0.03f,
                frequency  = 1.2f,
                phaseShift = wavePhase2
            )
            drawPath(
                path  = wavePath2,
                brush = Brush.verticalGradient(
                    colors = listOf(WaterBottom.copy(alpha = 0.7f), WaterBottom),
                    startY = waterTopY,
                    endY   = h
                )
            )

            // ---- Dezente Blasen (nur sichtbar wenn Wasser > 20 %) ----
            if (animatedFill > 0.2f) {
                drawBubbles(
                    w         = w,
                    h         = h,
                    waterTopY = waterTopY,
                    riseOffset = bubbleRise
                )
            }
        }

        // Tropfen-Umriss
        drawPath(
            path  = dropPath,
            color = DropOutline,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Glanz-Highlight oben links
        drawCircle(
            color  = Color.White.copy(alpha = 0.22f),
            radius = w * 0.08f,
            center = Offset(w * 0.35f, h * 0.28f)
        )
    }
}

// ---------------------------------------------------------------------------
// Wellen-Path aufbauen (Sinus-Kurve über die Breite)
// ---------------------------------------------------------------------------

private fun buildWavePath(
    w: Float,
    h: Float,
    waterTopY: Float,
    amplitude: Float,
    frequency: Float,
    phaseShift: Float
): Path = Path().apply {
    val steps = 64
    moveTo(0f, h)                          // unten links starten
    lineTo(0f, waterTopY)                  // hoch zur Wasseroberfläche links

    for (i in 0..steps) {
        val x = w * i / steps
        val y = waterTopY + amplitude * sin(frequency * 2f * PI.toFloat() * (i.toFloat() / steps) + phaseShift)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }

    lineTo(w, h)   // unten rechts
    close()
}

// ---------------------------------------------------------------------------
// Dezente Blasen zeichnen
// ---------------------------------------------------------------------------

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBubbles(
    w: Float,
    h: Float,
    waterTopY: Float,
    riseOffset: Float
) {
    // Feste Blasen-Definitionen: (x-Anteil, Starttiefe-Anteil, Radius)
    val bubbles = listOf(
        Triple(0.28f, 0.75f, w * 0.025f),
        Triple(0.60f, 0.55f, w * 0.018f),
        Triple(0.45f, 0.85f, w * 0.015f),
        Triple(0.72f, 0.65f, w * 0.022f)
    )

    val waterHeight = h - waterTopY

    for ((xFrac, depthFrac, radius) in bubbles) {
        val bx = w * xFrac
        // Blase steigt: depthFrac gibt Startposition an, riseOffset bewegt sie hoch
        val rawY = waterTopY + waterHeight * depthFrac - waterHeight * riseOffset
        // Wrap-around: sobald Blase oben raus ist, taucht sie unten wieder auf
        val by  = waterTopY + ((rawY - waterTopY).mod(waterHeight))

        // Nur zeichnen wenn innerhalb des Wasserbereichs
        if (by > waterTopY && by < h - radius) {
            drawCircle(
                color  = Color.White.copy(alpha = 0.15f),
                radius = radius,
                center = Offset(bx, by),
                style  = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Hilfsfunktion: Tropfen-Path aufbauen
// ---------------------------------------------------------------------------

private fun buildDropPath(w: Float, h: Float): Path = Path().apply {
    // Spitze unten, Kreis oben – klassische Tropfenform
    val cx = w / 2f
    // Kreis-Radius: oberes Drittel
    val circleR  = w * 0.42f
    val circleCy = h * 0.38f
    // Spitze unten
    val tipY     = h * 0.97f

    // Starte an der linken Seite des Kreises (unten)
    moveTo(cx - circleR * 0.85f, circleCy + circleR * 0.5f)

    // Kurve zur Spitze (links)
    cubicTo(
        cx - circleR * 0.9f, circleCy + circleR * 0.9f,
        cx - circleR * 0.3f, tipY - h * 0.08f,
        cx, tipY
    )

    // Kurve von Spitze nach rechts oben
    cubicTo(
        cx + circleR * 0.3f, tipY - h * 0.08f,
        cx + circleR * 0.9f, circleCy + circleR * 0.9f,
        cx + circleR * 0.85f, circleCy + circleR * 0.5f
    )

    // Oberer Kreisbogen
    arcTo(
        rect      = Rect(
            center = Offset(cx, circleCy),
            radius = circleR
        ),
        startAngleDegrees  = 30f,
        sweepAngleDegrees  = -240f,
        forceMoveTo        = false
    )

    close()
}

// ---------------------------------------------------------------------------
// Texte: Menge & Ziel
// ---------------------------------------------------------------------------

@Composable
private fun ProgressValueText(
    totalMl: Int,
    goalMl: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = formatMl(totalMl),
            fontSize   = 48.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            lineHeight = 52.sp
        )
        Text(
            text     = "/ ${formatMl(goalMl)} mL",
            fontSize = 16.sp,
            color    = TextSecondary
        )
    }
}

// ---------------------------------------------------------------------------
// Hilfsfunktion: ml-Wert formatieren (z. B. 1250 → "1.250")
// ---------------------------------------------------------------------------

private fun formatMl(ml: Int): String =
    String.format(Locale.GERMAN, "%,d", ml)

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "50 % Fortschritt", showBackground = true, backgroundColor = 0xFF0F1923)
@Composable
private fun HydrationProgressCardPreview50() {
    HydrationProgressCard(
        totalMl = 1250,
        goalMl  = 2500,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "0 % Fortschritt", showBackground = true, backgroundColor = 0xFF0F1923)
@Composable
private fun HydrationProgressCardPreview0() {
    HydrationProgressCard(
        totalMl = 0,
        goalMl  = 2500,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "100 % Fortschritt", showBackground = true, backgroundColor = 0xFF0F1923)
@Composable
private fun HydrationProgressCardPreview100() {
    HydrationProgressCard(
        totalMl = 2500,
        goalMl  = 2500,
        modifier = Modifier.padding(16.dp)
    )
}

