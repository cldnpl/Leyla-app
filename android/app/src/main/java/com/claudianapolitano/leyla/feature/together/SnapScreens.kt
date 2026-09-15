package com.claudianapolitano.leyla.feature.together

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.core.rememberHaptics
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Snap Hunt: one clue, both of you race to photograph the cleverest find, and
 * the judge crowns a winner. Port of `SnapViews.swift`.
 */

data class SnapUiState(
    val round: SnapRound? = null,
    val isLoading: Boolean = true,
    val submitting: Boolean = false,
    val errorMessage: String? = null,
    val partnerName: String = "your partner",
    val myAvatarPath: String? = null,
    val partnerAvatarPath: String? = null,
)

class SnapViewModel : ViewModel() {

    private val _state = MutableStateFlow(SnapUiState())
    val state: StateFlow<SnapUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            Session.snapshot.collect { s ->
                _state.update {
                    it.copy(
                        partnerName = s.partner?.displayName ?: "your partner",
                        myAvatarPath = s.user?.avatarPath,
                        partnerAvatarPath = s.partner?.avatarPath,
                    )
                }
            }
        }
        load()
        startPolling()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(round = GamesApi.snap(), isLoading = false, errorMessage = null) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = describe(e), isLoading = false) }
            }
        }
    }

    fun reload() {
        viewModelScope.launch {
            runCatching { GamesApi.snap() }.getOrNull()?.let { fresh ->
                _state.update { it.copy(round = fresh) }
            }
        }
    }

    /**
     * Poll so the hunt follows the partner on its own: one they started replaces
     * the local round even mid-hunt, and their snap flips this phone from
     * "waiting" to the verdict.
     *
     * Any change is taken, not just a new round id — see the same note on
     * [DrawViewModel.startPolling]; without a push, matching on the id alone
     * strands the waiting screen until someone taps "Check again".
     */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(5_000)
                val latest = runCatching { GamesApi.snap() }.getOrNull() ?: continue
                if (latest != _state.value.round) {
                    _state.update { it.copy(round = latest) }
                }
            }
        }
    }

    fun submit(jpeg: ByteArray, onSaved: () -> Unit = {}) {
        val roundId = _state.value.round?.roundId ?: return
        if (_state.value.submitting) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, errorMessage = null) }
            try {
                _state.update { it.copy(round = GamesApi.submitSnap(jpeg, roundId), submitting = false) }
                onSaved()
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, errorMessage = describe(e)) }
            }
        }
    }

    fun newRound(force: Boolean = false, onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(round = GamesApi.newSnap(force), errorMessage = null) }
                onStarted()
            } catch (e: Exception) {
                // A partner may have just started the next hunt. Reload the
                // single shared round so this phone can join it rather than
                // showing a stale clue or creating another one.
                val message = describe(e)
                runCatching { GamesApi.snap() }.getOrNull()?.let { fresh ->
                    _state.update { it.copy(round = fresh) }
                }
                _state.update { it.copy(errorMessage = message) }
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}

@Composable
fun SnapHuntScreen(
    modifier: Modifier = Modifier,
    viewModel: SnapViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val context = LocalContext.current
    val haptics = rememberHaptics()

    var picked by remember { mutableStateOf<Bitmap?>(null) }
    var confirmForceNewRound by remember { mutableStateOf(false) }
    val round = state.round

    // ACTION_IMAGE_CAPTURE only returns a thumbnail unless it is handed a file
    // to write into, and a hunt is judged on the photo — so the shot goes to a
    // FileProvider URI in the cache and is decoded back from there.
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    val galleryPick = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) picked = context.loadBitmap(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        val uri = captureUri
        if (saved && uri != null) picked = context.loadBitmap(uri)
        captureUri = null
    }

    fun launchCamera() {
        val uri = context.newCaptureUri()
        if (uri == null) {
            // No writable cache for the capture: fall back rather than
            // leaving the button doing nothing.
            galleryLauncher.launch(galleryPick)
            return
        }
        captureUri = uri
        cameraLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // Refusing the camera shouldn't dead-end the hunt: the photo picker
        // needs no permission at all, so it stands in.
        if (granted) launchCamera() else galleryLauncher.launch(galleryPick)
    }

    fun openCamera() {
        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        if (!hasCamera) {
            galleryLauncher.launch(galleryPick)
            return
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier.fillMaxSize().background(colors.background)) {
        when {
            round == null -> LoadingOrError(state.isLoading, state.errorMessage, onRetry = viewModel::reload)

            round.mySubmitted -> SnapRevealScreen(
                state = state,
                round = round,
                onNewRound = { picked = null; viewModel.newRound { haptics.lightTap() } },
                onReload = viewModel::reload,
                onStartOver = { confirmForceNewRound = true },
            )

            else -> SnapHuntCard(
                state = state,
                round = round,
                picked = picked,
                onSnap = ::openCamera,
                onRetake = { picked = null; openCamera() },
                onUse = {
                    val bitmap = picked ?: return@SnapHuntCard
                    viewModel.submit(bitmap.toJpeg()) {
                        picked = null
                        haptics.success()
                    }
                },
                onStartOver = { confirmForceNewRound = true },
            )
        }
    }

    if (confirmForceNewRound) {
        AlertDialog(
            onDismissRequest = { confirmForceNewRound = false },
            title = { Text("Start a new hunt?") },
            text = { Text("This will end the current hunt for both of you and pick a new clue.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmForceNewRound = false
                    picked = null
                    viewModel.newRound(force = true) { haptics.lightTap() }
                }) {
                    Text("Start over", color = Theme.coral)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmForceNewRound = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SnapHuntCard(
    state: SnapUiState,
    round: SnapRound,
    picked: Bitmap?,
    onSnap: () -> Unit,
    onRetake: () -> Unit,
    onUse: () -> Unit,
    onStartOver: () -> Unit,
) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent("green")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        MaterialCard(cornerRadius = 28.dp, contentPadding = 24.dp) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "FIND",
                        style = IOSText.caption2.weight(FontWeight.Bold).copy(letterSpacing = 2.sp),
                        color = accent,
                    )
                    Text(
                        "“${round.clue}”",
                        style = IOSText.title.weight(FontWeight.Bold),
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        when {
                            round.partnerSubmitted -> "${state.partnerName} already found theirs — quick!"
                            round.startedByMe -> "Race around the house and snap your cleverest find."
                            else -> "${state.partnerName} started this hunt — join them!"
                        },
                        style = IOSText.subheadline,
                        color = colors.secondary,
                        textAlign = TextAlign.Center,
                    )
                }

                if (picked != null) {
                    androidx.compose.foundation.Image(
                        bitmap = picked.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                    )
                    PillButton(
                        text = "Use this photo",
                        color = accent,
                        onClick = onUse,
                        icon = Icons.Filled.Check,
                        enabled = !state.submitting,
                        loading = state.submitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextAction("Retake", onRetake, icon = Icons.Filled.CameraAlt, color = accent)
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(QuizPalette.gradient("green", alpha = 0.4f))
                            .plainClickable(onClick = onSnap)
                            .padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(44.dp),
                        )
                        Text("Snap a photo", style = IOSText.headline, color = accent)
                    }
                }

                if (state.errorMessage != null) {
                    Text(
                        state.errorMessage,
                        style = IOSText.footnote,
                        color = errorRed(),
                        textAlign = TextAlign.Center,
                    )
                }

                TextAction("Start a new hunt", onStartOver, icon = Icons.Filled.Refresh)
            }
        }
    }
}

@Composable
private fun SnapRevealScreen(
    state: SnapUiState,
    round: SnapRound,
    onNewRound: () -> Unit,
    onReload: () -> Unit,
    onStartOver: () -> Unit,
) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent("green")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "“${round.clue}”",
            style = IOSText.title3.weight(FontWeight.Bold),
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (round.revealed) {
            Icon(
                crownIcon(round.outcome),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(48.dp),
            )
            Text(
                crownTitle(round.outcome, state.partnerName),
                style = IOSText.title3.weight(FontWeight.Bold),
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            round.reason?.let { reason ->
                Text(
                    reason,
                    style = IOSText.subheadline,
                    color = colors.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            ArtworkCard(
                title = "You",
                path = round.myImagePath,
                avatarPath = state.myAvatarPath,
                colorKey = "green",
                alpha = if (round.outcome == "me") 0.6f else 0.35f,
                trailing = if (round.outcome == "me") {
                    { CleverestBadge(accent) }
                } else {
                    null
                },
            )
            ArtworkCard(
                title = state.partnerName,
                path = round.partnerImagePath,
                avatarPath = state.partnerAvatarPath,
                colorKey = "green",
                alpha = if (round.outcome == "partner") 0.6f else 0.35f,
                trailing = if (round.outcome == "partner") {
                    { CleverestBadge(accent) }
                } else {
                    null
                },
            )
        } else {
            QuizIconTile(
                Icons.Filled.HourglassTop,
                "green",
                size = 64.dp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text("Got it! 📸", style = IOSText.title3.weight(FontWeight.Bold), color = colors.ink)
            Text(
                "Your find is locked in. The judge crowns a winner once " +
                    "${state.partnerName} snaps theirs too.",
                style = IOSText.subheadline,
                color = colors.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            ArtworkCard("Your find", round.myImagePath, state.myAvatarPath, "green", alpha = 0.35f)
        }

        if (state.errorMessage != null) {
            Text(state.errorMessage, style = IOSText.footnote, color = errorRed(), textAlign = TextAlign.Center)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            if (round.revealed) {
                PillButton("New hunt", accent, onNewRound, icon = Icons.Filled.Refresh)
            } else {
                TextAction("Check again", onReload, icon = Icons.Filled.Refresh, color = accent)
                // Escape hatch while the partner still hasn't snapped — without
                // this the only way out is to keep refreshing.
                TextAction("Start a new hunt", onStartOver, icon = Icons.Filled.Refresh)
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun CleverestBadge(accent: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(13.dp),
        )
        Text("Cleverest", style = IOSText.caption2.weight(FontWeight.Bold), color = accent)
    }
}

private fun crownIcon(outcome: String?): ImageVector = when (outcome) {
    "me" -> Icons.Filled.EmojiEvents
    "partner" -> Icons.Filled.Flag
    else -> Icons.Filled.AllInclusive
}

private fun crownTitle(outcome: String?, partnerName: String): String = when (outcome) {
    "me" -> "You found the cleverest! 🏆"
    "partner" -> "$partnerName wins this hunt 😄"
    else -> "It's a tie — both brilliant! 🤝"
}

// MARK: - Image plumbing

/** Compresses the picked shot the way the iOS client does before uploading. */
private fun Bitmap.toJpeg(quality: Int = 85): ByteArray =
    ByteArrayOutputStream().use { out ->
        compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.toByteArray()
    }

/**
 * A fresh file in the app's cache for the camera to write into, exposed through
 * the manifest's FileProvider. Null only if the cache directory is unavailable.
 */
private fun Context.newCaptureUri(): Uri? = runCatching {
    val dir = java.io.File(cacheDir, "captures").apply { mkdirs() }
    val file = java.io.File(dir, "snap-${System.currentTimeMillis()}.jpg")
    FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}.getOrNull()

/**
 * Decodes a picked or captured photo, downsampled so a 12-megapixel shot
 * doesn't have to be held in memory whole just to become a ~1600px upload.
 */
private fun Context.loadBitmap(uri: Uri, maxDimension: Int = 1600): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}.getOrNull()
