package com.claudianapolitano.leyla.feature.journal

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.core.ApiException
import com.claudianapolitano.leyla.core.JournalEntry
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.MediaItem
import com.claudianapolitano.leyla.core.loadBitmap
import com.claudianapolitano.leyla.core.newCaptureUri
import com.claudianapolitano.leyla.core.rememberHaptics
import com.claudianapolitano.leyla.core.toJpeg
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.RemoteImage
import com.claudianapolitano.leyla.feature.together.plainClickable
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Composer for a diary entry: pick a day, write some words and/or attach photos
 * — from the library or taken there and then. Saving adds a new entry for that
 * day (you can write as many per day as you like) or rewrites the one being
 * edited, then uploads the photos that were attached. Port of
 * `AddJournalEntrySheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJournalEntrySheet(
    existing: JournalEntry?,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = LeylaTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isEditing = existing != null
    var date by remember { mutableStateOf(JournalDates.day(existing?.date) ?: LocalDate.now()) }
    var text by remember { mutableStateOf(existing?.body.orEmpty()) }
    val existingPhotos = remember { mutableStateListOf<MediaItem>().apply { addAll(existing?.photos.orEmpty()) } }
    /** Attached in this session, already decoded so they show as real thumbnails. */
    val newPhotos = remember { mutableStateListOf<Bitmap>() }

    var loadingPicks by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val canSave = (text.isNotBlank() || newPhotos.isNotEmpty() || existingPhotos.isNotEmpty()) && !saving

    // MARK: photo sources

    val galleryPick = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(12),
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        loadingPicks = true
        scope.launch {
            uris.forEach { uri -> context.loadBitmap(uri)?.let(newPhotos::add) }
            loadingPicks = false
        }
    }

    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        val uri = captureUri
        if (saved && uri != null) context.loadBitmap(uri)?.let(newPhotos::add)
        captureUri = null
    }

    fun launchCamera() {
        val uri = context.newCaptureUri("journal")
        if (uri == null) {
            galleryLauncher.launch(galleryPick)
            return
        }
        captureUri = uri
        cameraLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // Refusing the camera shouldn't dead-end the entry: the photo picker
        // needs no permission at all, so it stands in.
        if (granted) launchCamera() else galleryLauncher.launch(galleryPick)
    }

    fun openCamera() {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            galleryLauncher.launch(galleryPick)
            return
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // MARK: saving

    fun save() {
        if (!canSave) return
        scope.launch {
            saving = true
            errorMessage = null
            try {
                val entry = if (existing != null) {
                    LeylaApi.updateJournalEntry(existing.id, text.trim())
                } else {
                    LeylaApi.createJournalEntry(JournalDates.isoDay(date), text.trim())
                }
                newPhotos.forEachIndexed { index, bitmap ->
                    if (newPhotos.size > 1) progress = "${index + 1}/${newPhotos.size}"
                    LeylaApi.uploadJournalPhoto(entry.id, bitmap.toJpeg())
                }
                haptics.success()
                onDone()
                onDismiss()
            } catch (e: Exception) {
                errorMessage = (e as? ApiException)?.payload?.message ?: e.message
            } finally {
                saving = false
                progress = null
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = sheetState,
        containerColor = colors.backgroundTop,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    leylaString(if (isEditing) R.string.journal_edit_day else R.string.journal_new_entry),
                    style = IOSText.title3.weight(FontWeight.Bold),
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss, enabled = !saving) {
                    Text(leylaString(R.string.cancel), color = colors.secondary)
                }
                TextButton(onClick = ::save, enabled = canSave) {
                    Text(
                        if (saving) progress ?: leylaString(R.string.saving) else leylaString(R.string.save),
                        style = IOSText.subheadline.weight(FontWeight.SemiBold),
                        color = if (canSave) Theme.coral else Theme.coral.copy(alpha = 0.4f),
                    )
                }
            }

            // The day an entry belongs to is fixed once written, as on iOS.
            Row(
                Modifier
                    .fillMaxWidth()
                    .plainClickable(enabled = !isEditing && !saving) { showDatePicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(leylaString(R.string.journal_day), style = IOSText.body, color = colors.ink)
                Text(
                    JournalDates.medium(date),
                    style = IOSText.body.weight(FontWeight.SemiBold),
                    color = if (isEditing) colors.secondary else Theme.coral,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    leylaString(R.string.journal_words),
                    style = IOSText.footnote.weight(FontWeight.SemiBold),
                    color = colors.secondary,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(leylaString(R.string.journal_what_happened)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !saving,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    leylaString(R.string.journal_photos),
                    style = IOSText.footnote.weight(FontWeight.SemiBold),
                    color = colors.secondary,
                )

                if (existingPhotos.isNotEmpty() || newPhotos.isNotEmpty() || loadingPicks) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        existingPhotos.forEach { item ->
                            Thumbnail(
                                // Only the person who uploaded a photo may remove it.
                                onRemove = if (item.uploaderId == existing?.authorId) {
                                    {
                                        scope.launch {
                                            runCatching { LeylaApi.deletePhoto(item.id) }
                                                .onSuccess { existingPhotos.remove(item) }
                                                .onFailure { e ->
                                                    errorMessage =
                                                        (e as? ApiException)?.payload?.message ?: e.message
                                                }
                                        }
                                    }
                                } else {
                                    null
                                },
                            ) {
                                RemoteImage(
                                    path = item.thumbUrl,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        newPhotos.forEach { bitmap ->
                            Thumbnail(onRemove = { newPhotos.remove(bitmap) }) {
                                Image(
                                    bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        if (loadingPicks) {
                            Box(
                                Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.secondary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = Theme.rose, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                PhotoSourceRow(
                    icon = Icons.Filled.PhotoLibrary,
                    label = leylaString(R.string.journal_choose_library),
                    enabled = !loadingPicks && !saving,
                    onClick = { galleryLauncher.launch(galleryPick) },
                )
                PhotoSourceRow(
                    icon = Icons.Filled.PhotoCamera,
                    label = leylaString(R.string.journal_take_photo),
                    enabled = !loadingPicks && !saving,
                    onClick = ::openCamera,
                )
            }

            errorMessage?.let {
                Text(it, style = IOSText.footnote, color = Theme.coral)
            }
        }
    }

    if (showDatePicker) {
        JournalDayPicker(
            initial = date,
            onDismiss = { showDatePicker = false },
            onPick = { date = it; showDatePicker = false },
        )
    }
}

@Composable
private fun PhotoSourceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .plainClickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Theme.coral, modifier = Modifier.size(20.dp))
        Text(
            label,
            style = IOSText.body,
            color = if (enabled) Theme.coral else Theme.coral.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun Thumbnail(onRemove: (() -> Unit)?, content: @Composable () -> Unit) {
    Box {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) { content() }
        if (onRemove != null) {
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .plainClickable(onClick = onRemove)
                    .padding(3.dp),
            )
        }
    }
}

/** A day picker that refuses the future — you cannot write tomorrow's diary. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalDayPicker(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerStateUpTo(initial)
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text(leylaString(R.string.done)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(leylaString(R.string.cancel)) } },
    ) {
        androidx.compose.material3.DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDatePickerStateUpTo(initial: LocalDate) =
    androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = PastDates,
    )

@OptIn(ExperimentalMaterial3Api::class)
private object PastDates : androidx.compose.material3.SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
