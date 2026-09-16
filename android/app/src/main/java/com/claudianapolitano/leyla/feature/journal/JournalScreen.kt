package com.claudianapolitano.leyla.feature.journal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.core.JournalEntry
import com.claudianapolitano.leyla.core.MediaItem
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.softShadow
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.RemoteImage
import com.claudianapolitano.leyla.feature.together.plainClickable

/**
 * The Journal tab: editable **Milestones** at the top, then a reverse-chrono
 * **diary** — one card per day grouping both partners' text and photos, newest
 * first. Everything is shared: what one partner writes, the other sees. Port of
 * `Us/Features/Journal/JournalView.swift`.
 */
@Composable
fun JournalScreen(
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by Session.snapshot.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors

    var composing by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<JournalEntry?>(null) }
    var pager by remember { mutableStateOf<Pair<List<MediaItem>, Int>?>(null) }
    var showGallery by remember { mutableStateOf(false) }

    val myId = session.user?.id
    fun isMine(authorId: String) = authorId == myId
    fun authorName(authorId: String): String =
        if (isMine(authorId)) session.user?.displayName.orEmpty().ifEmpty { "You" }
        else session.partner?.displayName.orEmpty().ifEmpty { "Partner" }
    fun authorAvatar(authorId: String): String? =
        if (isMine(authorId)) session.user?.avatarPath else session.partner?.avatarPath

    // The gallery replaces the tab's content, so system Back has to close it
    // rather than leaving the tab. The pager is a dialog and handles its own.
    BackHandler(enabled = showGallery) { showGallery = false }

    pager?.let { (photos, start) ->
        JournalPhotoPager(
            photos = photos,
            startIndex = start,
            currentUserId = myId,
            onDelete = { item, done -> viewModel.deletePhoto(item.id) { done(it) } },
            onClose = { pager = null },
        )
    }

    if (showGallery) {
        GalleryScreen(
            months = state.photoMonths,
            isLoading = state.isLoading,
            onOpenPhotos = { photos, index -> pager = photos to index },
            onBack = { showGallery = false },
            modifier = modifier,
        )
        return
    }

    Box(modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            JournalTopBar(
                onOpenGallery = { showGallery = true },
                onCompose = { editing = null; composing = true },
            )

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    MilestonesSection(
                        milestones = state.milestones,
                        onAdd = { title, date, done -> viewModel.addMilestone(title, date, done) },
                        onUpdate = { id, title, date, done -> viewModel.updateMilestone(id, title, date, done) },
                        onDelete = viewModel::deleteMilestone,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }

                val days = state.days
                when {
                    state.isLoading && state.entries.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Theme.rose)
                        }
                    }

                    days.isEmpty() -> item { EmptyDiary() }

                    else -> itemsIndexed(days) { index, day ->
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            val previous = days.getOrNull(index - 1)
                            val label = JournalDates.monthYear(day.date)
                            if (previous == null || JournalDates.monthYear(previous.date) != label) {
                                Text(
                                    label,
                                    style = IOSText.title3.weight(FontWeight.ExtraBold),
                                    color = colors.ink,
                                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 6.dp),
                                )
                            }
                            JournalDayCard(
                                day = day,
                                authorName = ::authorName,
                                authorAvatarPath = ::authorAvatar,
                                isMine = ::isMine,
                                onOpenPhotos = { photos, start -> pager = photos to start },
                                onEdit = { editing = it; composing = true },
                                onDelete = viewModel::deleteEntry,
                            )
                        }
                    }
                }
            }
        }

        state.errorMessage?.let { message ->
            ErrorToast(message, Modifier.align(Alignment.BottomCenter))
        }
    }

    if (composing) {
        AddJournalEntrySheet(
            existing = editing,
            onDismiss = { composing = false; editing = null },
            onDone = viewModel::reload,
        )
    }
}

@Composable
private fun JournalTopBar(onOpenGallery: () -> Unit, onCompose: () -> Unit) {
    val colors = LeylaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenGallery) {
            Icon(
                Icons.Filled.PhotoLibrary,
                contentDescription = leylaString(R.string.journal_photos),
                tint = Theme.coral,
            )
        }
        Text(
            leylaString(R.string.tab_journal),
            style = IOSText.title3.weight(FontWeight.Bold),
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onCompose) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = leylaString(R.string.journal_write),
                tint = Theme.coral,
            )
        }
    }
}

@Composable
private fun EmptyDiary() {
    val colors = LeylaTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = Theme.coral,
            modifier = Modifier.size(46.dp),
        )
        Text(
            leylaString(R.string.journal_empty_title),
            style = IOSText.title3.weight(FontWeight.Bold),
            color = colors.ink,
        )
        Text(
            leylaString(R.string.journal_empty_body),
            style = IOSText.subheadline,
            color = colors.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorToast(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        style = IOSText.footnote,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
            .clip(CircleShape)
            .background(Theme.coral.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/**
 * Every photo in the diary, newest first and grouped by month — the same months,
 * in the same order, as the journal pages this screen opens from. Port of
 * `GalleryView`.
 *
 * The photos come from the journal entries themselves, not from the media
 * library: a diary photo belongs to the day it was written under, which is the
 * order this screen has to show.
 */
@Composable
private fun GalleryScreen(
    months: List<JournalPhotoMonth>,
    isLoading: Boolean,
    onOpenPhotos: (List<MediaItem>, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    Column(modifier.fillMaxSize().background(colors.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = leylaString(R.string.back),
                    tint = colors.ink,
                )
            }
            Text(
                leylaString(R.string.journal_photos),
                style = IOSText.title3.weight(FontWeight.Bold),
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.size(48.dp))
        }

        when {
            isLoading && months.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Theme.rose)
            }

            months.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = Theme.coral,
                    modifier = Modifier.size(52.dp),
                )
                Text(
                    leylaString(R.string.gallery_empty_title),
                    style = IOSText.title3.weight(FontWeight.Bold),
                    color = colors.ink,
                )
                Text(
                    leylaString(R.string.gallery_empty_body),
                    style = IOSText.subheadline,
                    color = colors.secondary,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(months, key = { it.id }) { month ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                JournalDates.monthYear(month.date),
                                style = IOSText.title3.weight(FontWeight.ExtraBold),
                                color = colors.ink,
                            )
                            Text(
                                month.photos.size.toString(),
                                style = IOSText.caption.weight(FontWeight.Bold),
                                color = colors.secondary,
                                modifier = Modifier.padding(bottom = 3.dp),
                            )
                        }
                        MonthGrid(month = month, onOpenPhotos = onOpenPhotos)
                    }
                }
            }
        }
    }
}

/**
 * A month's thumbnails. Rendered as rows rather than a nested lazy grid: a
 * vertical grid inside a vertical list has no bounded height to lay out in.
 */
@Composable
private fun MonthGrid(month: JournalPhotoMonth, onOpenPhotos: (List<MediaItem>, Int) -> Unit) {
    val columns = 3
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        month.photos.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { columnIndex, item ->
                    val index = rowIndex * columns + columnIndex
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .softShadow(14.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .plainClickable { onOpenPhotos(month.photos, index) },
                    ) {
                        RemoteImage(
                            path = item.thumbUrl,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                // Keeps a short last row aligned with the columns above it.
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}
