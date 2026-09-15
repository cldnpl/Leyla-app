package com.claudianapolitano.leyla.feature.together

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claudianapolitano.leyla.core.rememberHaptics
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.Theme
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import androidx.compose.ui.graphics.Path as ComposePath

/**
 * The drawing surface for Draw Together. Port of `DrawingPad` in
 * `DrawViews.swift`, which is built on PencilKit; Android has no equivalent
 * stock canvas, so strokes are captured here and replayed onto a Compose
 * [Canvas], with the same tools: a colour palette, four brush widths, an
 * eraser, a paint bucket, undo, clear and a three-minute countdown.
 *
 * The sheet is deliberately white in both colour schemes, exactly like the iOS
 * pad — the export is a JPEG the partner sees on their own device, and it must
 * not change because the artist happened to be in dark mode.
 */

/** One captured stroke, replayed every frame and again at export time. */
private data class PenStroke(
    val path: ComposePath,
    val color: Color,
    val width: Float,
)

/** The canvas palette. Port of `DrawPaletteColor.all`. */
data class DrawPaletteColor(val id: String, val name: String, val color: Color) {
    companion object {
        /**
         * Explicit RGB black rather than a theme colour — this ink is baked into
         * an exported bitmap and must never resolve to white in dark mode.
         */
        val black = DrawPaletteColor("black", "Black", Color(0.02f, 0.02f, 0.02f))

        val all: List<DrawPaletteColor> = listOf(
            black,
            DrawPaletteColor("red", "Red", Color(0.90f, 0.22f, 0.21f)),
            DrawPaletteColor("orange", "Orange", Color(1.0f, 0.58f, 0.0f)),
            DrawPaletteColor("yellow", "Yellow", Color(1.0f, 0.84f, 0.0f)),
            DrawPaletteColor("green", "Green", Color(0.20f, 0.66f, 0.33f)),
            DrawPaletteColor("blue", "Blue", Color(0.0f, 0.48f, 1.0f)),
            DrawPaletteColor("purple", "Purple", Color(0.58f, 0.24f, 0.80f)),
        )
    }
}

private const val CANVAS_WHITE = 0xFFFFFFFF.toInt()
private val BRUSH_SIZES = listOf(3f, 6f, 12f, 20f)
private const val TOTAL_SECONDS = 180

@Composable
fun DrawingPad(
    prompt: String,
    submitting: Boolean,
    onSubmit: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val accent = QuizPalette.accent("purple")

    val strokes = remember { mutableStateListOf<PenStroke>() }
    /** Bucket fills, stacked oldest-first; undo pops the newest. */
    val fills = remember { mutableStateListOf<ImageBitmap>() }
    var colorId by remember { mutableStateOf(DrawPaletteColor.black.id) }
    var brushSize by remember { mutableStateOf(6f) }
    var isEraser by remember { mutableStateOf(false) }
    var isBucket by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var remaining by remember { mutableStateOf(TOTAL_SECONDS) }
    /** Ordered log of what to undo next: a stroke or a fill. */
    val history = remember { mutableStateListOf<HistoryEntry>() }

    val selectedColor = DrawPaletteColor.all.firstOrNull { it.id == colorId } ?: DrawPaletteColor.black

    fun export(): ByteArray? {
        val size = canvasSize
        if (size.width < 2 || size.height < 2) return null
        val bitmap = renderCanvas(size, fills.lastOrNull(), strokes)
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.toByteArray()
        }
    }

    fun submit() {
        if (submitting) return
        export()?.let(onSubmit)
    }

    // The countdown: the pad submits itself when it runs out, so a round can't
    // stall forever on someone who walked away mid-drawing.
    LaunchedEffect(submitting) {
        while (!submitting && remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
        if (remaining == 0 && !submitting) submit()
    }

    Column(modifier.fillMaxSize().background(Color.White)) {
        DrawPromptHeader(prompt, remaining, accent)

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .onSizeChanged { canvasSize = it }
                .pointerInput(isBucket, selectedColor, brushSize, isEraser) {
                    if (isBucket) {
                        detectTapGestures { position ->
                            val size = canvasSize
                            if (size.width < 2 || size.height < 2) return@detectTapGestures
                            val base = renderCanvas(size, fills.lastOrNull(), strokes)
                            val filled = base.floodFilled(position, selectedColor.color.toArgb())
                            if (filled != null) {
                                fills.add(filled.asImageBitmap())
                                history.add(HistoryEntry.Fill)
                                isBucket = false
                                haptics.lightTap()
                            }
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = { start ->
                                val path = ComposePath().apply { moveTo(start.x, start.y) }
                                strokes.add(
                                    PenStroke(
                                        path = path,
                                        // The sheet is white, so the eraser is
                                        // simply white ink — which is also what
                                        // the flattened export would show.
                                        color = if (isEraser) Color.White else selectedColor.color,
                                        width = if (isEraser) brushSize * 2.2f else brushSize,
                                    ),
                                )
                                history.add(HistoryEntry.StrokeMark)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val last = strokes.lastOrNull() ?: return@detectDragGestures
                                last.path.lineTo(change.position.x, change.position.y)
                                // Path is mutated in place, so nudge the list to
                                // make Compose redraw this frame.
                                strokes[strokes.lastIndex] = last.copy()
                            },
                        )
                    }
                }
                // A quick tap is a dot, not a no-op: detectDragGestures never
                // fires for a touch that doesn't move, so it gets its own
                // detector rather than being silently swallowed.
                .pointerInput(isBucket, selectedColor, brushSize, isEraser) {
                    if (isBucket) return@pointerInput
                    detectTapGestures { position ->
                        val dot = ComposePath().apply {
                            moveTo(position.x, position.y)
                            lineTo(position.x + 0.1f, position.y)
                        }
                        strokes.add(
                            PenStroke(
                                path = dot,
                                color = if (isEraser) Color.White else selectedColor.color,
                                width = if (isEraser) brushSize * 2.2f else brushSize,
                            ),
                        )
                        history.add(HistoryEntry.StrokeMark)
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                fills.lastOrNull()?.let { fill ->
                    drawImage(fill)
                }
                strokes.forEach { stroke ->
                    drawPath(
                        path = stroke.path,
                        color = stroke.color,
                        style = Stroke(
                            width = stroke.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }

        DrawToolbar(
            selectedColorId = colorId,
            brushSize = brushSize,
            isEraser = isEraser,
            isBucket = isBucket,
            accent = accent,
            submitting = submitting,
            onColor = { colorId = it; isEraser = false; isBucket = false; haptics.lightTap() },
            onBrushSize = { brushSize = it; isEraser = false; isBucket = false; haptics.lightTap() },
            onEraser = { isEraser = true; isBucket = false; haptics.lightTap() },
            onBucket = { isBucket = !isBucket; isEraser = false; haptics.lightTap() },
            onUndo = {
                when (history.removeLastOrNull()) {
                    HistoryEntry.Fill -> fills.removeLastOrNull()
                    HistoryEntry.StrokeMark -> strokes.removeLastOrNull()
                    null -> Unit
                }
            },
            onClear = { strokes.clear(); fills.clear(); history.clear() },
            onDone = { submit() },
        )
    }
}

private sealed interface HistoryEntry {
    data object StrokeMark : HistoryEntry
    data object Fill : HistoryEntry
}

/** "DRAW — <prompt>" header with the countdown above the canvas. */
@Composable
private fun DrawPromptHeader(prompt: String, remaining: Int, accent: Color) {
    val minutes = remaining / 60
    val seconds = remaining % 60
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "DRAW",
            style = IOSText.caption2.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = accent,
        )
        Text(
            "“$prompt”",
            style = IOSText.title3.copy(fontWeight = FontWeight.Bold),
            // The pad is a white sheet in both schemes, so its text is fixed
            // dark rather than the theme's ink.
            color = Color(0.18f, 0.16f, 0.20f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.AccessTimeFilled,
                contentDescription = null,
                tint = if (remaining <= 15) Theme.coral else Color(0.18f, 0.16f, 0.20f).copy(alpha = 0.6f),
                modifier = Modifier.size(13.dp),
            )
            Text(
                "%d:%02d".format(minutes, seconds),
                style = IOSText.subheadline.copy(fontWeight = FontWeight.Bold),
                color = if (remaining <= 15) Theme.coral else Color(0.18f, 0.16f, 0.20f).copy(alpha = 0.6f),
            )
        }
    }
}

/** Colour palette, brush/fill tools, eraser/undo/clear, and Done. */
@Composable
private fun DrawToolbar(
    selectedColorId: String,
    brushSize: Float,
    isEraser: Boolean,
    isBucket: Boolean,
    accent: Color,
    submitting: Boolean,
    onColor: (String) -> Unit,
    onBrushSize: (Float) -> Unit,
    onEraser: () -> Unit,
    onBucket: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
) {
    val toolBackground = Color(0.93f, 0.93f, 0.94f)
    val toolInk = Color(0.18f, 0.16f, 0.20f)
    var sizeMenuOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Keep every colour reachable on compact phones: the leading swatches
        // stay inside the safe area rather than being clipped by the fixed
        // controls below.
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DrawPaletteColor.all.forEach { swatch ->
                val active = !isEraser && !isBucket && selectedColorId == swatch.id
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(swatch.color)
                        .then(
                            if (active) Modifier.border(3.dp, Color.White, CircleShape) else Modifier,
                        )
                        .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                        .plainClickable { onColor(swatch.id) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(toolBackground)
                        .plainClickable { sizeMenuOpen = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.LineWeight,
                        contentDescription = "Brush size",
                        tint = toolInk,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        brushSize.toInt().toString(),
                        style = IOSText.caption.copy(fontWeight = FontWeight.Bold),
                        color = toolInk,
                    )
                }
                DropdownMenu(expanded = sizeMenuOpen, onDismissRequest = { sizeMenuOpen = false }) {
                    BRUSH_SIZES.forEach { size ->
                        DropdownMenuItem(
                            text = { Text("${size.toInt()} pt") },
                            onClick = { onBrushSize(size); sizeMenuOpen = false },
                            trailingIcon = {
                                if (size == brushSize) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            }

            ToolButton(Icons.Filled.FormatColorFill, "Fill area", isBucket, accent, toolBackground, toolInk, onBucket)
            ToolButton(Icons.Filled.Delete, "Clear", false, accent, toolBackground, toolInk, onClear)
            ToolButton(Icons.AutoMirrored.Filled.Undo, "Undo", false, accent, toolBackground, toolInk, onUndo)
            ToolButton(Icons.Filled.AutoFixNormal, "Eraser", isEraser, accent, toolBackground, toolInk, onEraser)
        }

        PillButton(
            text = "Done",
            color = accent,
            onClick = onDone,
            icon = Icons.Filled.Check,
            enabled = !submitting,
            loading = submitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    background: Color,
    ink: Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) accent else background)
            .plainClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) Color.White else ink,
            modifier = Modifier.size(18.dp),
        )
    }
}

// MARK: - Bitmap work

/** Flattens the fill layer and every stroke onto an opaque white sheet. */
private fun renderCanvas(
    size: IntSize,
    fill: ImageBitmap?,
    strokes: List<PenStroke>,
): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(CANVAS_WHITE)
    fill?.let { canvas.drawBitmap(it.asAndroidBitmap(), 0f, 0f, null) }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { stroke ->
        paint.color = stroke.color.toArgb()
        paint.strokeWidth = stroke.width
        canvas.drawPath(stroke.path.asAndroidPath(), paint)
    }
    return bitmap
}

/**
 * Flood-fills a contiguous region of the rendered canvas. The snapshot already
 * carries the strokes, so the line art bounds the fill exactly the way the iOS
 * bucket does.
 */
private fun Bitmap.floodFilled(point: Offset, fillColor: Int): Bitmap? {
    if (width < 2 || height < 2) return null
    val startX = point.x.toInt().coerceIn(0, width - 1)
    val startY = point.y.toInt().coerceIn(0, height - 1)

    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    val target = pixels[startY * width + startX]
    val tolerance = 34
    if (colorDistance(target, fillColor) <= tolerance) return null

    // An explicit stack rather than recursion: a full-screen fill is hundreds of
    // thousands of pixels deep and would blow the JVM stack. Pixels are marked
    // seen as they are pushed, not as they are popped, so no pixel is ever
    // queued twice and the stack cannot outgrow the image.
    val stack = IntArray(width * height)
    val seen = BooleanArray(width * height)
    var top = 0
    val start = startY * width + startX
    stack[top++] = start
    seen[start] = true

    while (top > 0) {
        val index = stack[--top]
        if (colorDistance(pixels[index], target) > tolerance) continue
        pixels[index] = fillColor

        val x = index % width
        val y = index / width
        if (x > 0 && !seen[index - 1]) { seen[index - 1] = true; stack[top++] = index - 1 }
        if (x < width - 1 && !seen[index + 1]) { seen[index + 1] = true; stack[top++] = index + 1 }
        if (y > 0 && !seen[index - width]) { seen[index - width] = true; stack[top++] = index - width }
        if (y < height - 1 && !seen[index + width]) { seen[index + width] = true; stack[top++] = index + width }
    }

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
}

private fun colorDistance(lhs: Int, rhs: Int): Int {
    val dr = ((lhs shr 16) and 0xFF) - ((rhs shr 16) and 0xFF)
    val dg = ((lhs shr 8) and 0xFF) - ((rhs shr 8) and 0xFF)
    val db = (lhs and 0xFF) - (rhs and 0xFF)
    return kotlin.math.abs(dr) + kotlin.math.abs(dg) + kotlin.math.abs(db)
}
