package com.claudianapolitano.leyla.feature.journal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.core.JournalEntry
import com.claudianapolitano.leyla.core.MediaItem
import com.claudianapolitano.leyla.designsystem.Avatar
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaCard
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.softShadow
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.RemoteImage
import com.claudianapolitano.leyla.feature.together.plainClickable

/**
 * A single day card: a coral date badge on the left, then each partner's block
 * (name + text + optional photo stack), divided like a scrapbook page. Port of
 * `JournalDayCard`.
 *
 * iOS puts edit and delete behind a left swipe plus a long-press context menu.
 * Android has no swipe-actions idiom in this position, so the long press is the
 * whole gesture here — the same actions, reached the way Android reaches them.
 */
@Composable
fun JournalDayCard(
    day: JournalDay,
    authorName: (String) -> String,
    authorAvatarPath: (String) -> String?,
    isMine: (String) -> Boolean,
    onOpenPhotos: (List<MediaItem>, Int) -> Unit,
    onEdit: (JournalEntry) -> Unit,
    onDelete: (JournalEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    LeylaCard(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DateBadge(day)
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                day.entries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(color = colors.hairline)
                    EntryBlock(
                        entry = entry,
                        authorName = authorName,
                        authorAvatarPath = authorAvatarPath,
                        isMine = isMine(entry.authorId),
                        onOpenPhotos = onOpenPhotos,
                        onEdit = { onEdit(entry) },
                        onDelete = { onDelete(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DateBadge(day: JournalDay) {
    val colors = LeylaTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(54.dp)
                .softShadow(27.dp)
                .clip(CircleShape)
                .background(Theme.roseGradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                JournalDates.dayNumber(day.date),
                style = IOSText.title2.weight(FontWeight.ExtraBold),
                color = Color.White,
            )
        }
        Text(
            JournalDates.weekdayShort(day.date),
            style = IOSText.caption2.weight(FontWeight.SemiBold),
            color = colors.secondary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryBlock(
    entry: JournalEntry,
    authorName: (String) -> String,
    authorAvatarPath: (String) -> String?,
    isMine: Boolean,
    onOpenPhotos: (List<MediaItem>, Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LeylaTheme.colors
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Column(
            Modifier
                .fillMaxWidth()
                // Only your own entry can be edited or removed; the partner's
                // block is static, so it takes no gesture at all.
                .then(
                    if (isMine) {
                        Modifier.combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onLongClick = { menuOpen = true },
                            onClick = {},
                        )
                    } else {
                        Modifier
                    },
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(
                    path = authorAvatarPath(entry.authorId),
                    name = authorName(entry.authorId),
                    size = 22.dp,
                )
                Text(
                    leylaString(R.string.journal_author_label, authorName(entry.authorId)),
                    style = IOSText.subheadline.weight(FontWeight.Bold),
                    color = if (isMine) Theme.coral else colors.ink,
                )
            }

            if (entry.body.isNotEmpty()) {
                Text(entry.body, style = IOSText.body, color = colors.ink)
            }

            if (entry.photos.isNotEmpty()) {
                PhotoStack(photos = entry.photos, onTap = { onOpenPhotos(entry.photos, it) })
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(leylaString(R.string.edit)) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = { menuOpen = false; onEdit() },
            )
            DropdownMenuItem(
                text = { Text(leylaString(R.string.delete), color = Theme.coral) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Theme.coral) },
                onClick = { menuOpen = false; onDelete() },
            )
        }
    }
}

/**
 * A playful "photos tossed on the page" preview: up to three overlapping
 * thumbnails with a stable tilt, and a +N badge when there are more. Port of
 * `PhotoStackView`.
 */
@Composable
fun PhotoStack(photos: List<MediaItem>, onTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shown = photos.take(3)
    val size = 104.dp
    val step = 22.dp

    Row(
        modifier
            .padding(vertical = 4.dp)
            // The whole stack is one target: tapping it opens the pager at the
            // top photo, as iOS does.
            .plainClickable { onTap(0) },
    ) {
        Box(
            Modifier.padding(end = step * (shown.size - 1).coerceAtLeast(0) + 8.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            // Drawn back-to-front so the first photo ends up on top, the way a
            // stack tossed on a page actually lies.
            shown.indices.reversed().forEach { index ->
                RemoteImage(
                    path = shown[index].thumbUrl,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .offset(x = step * index, y = 6.dp * index)
                        .rotate(TILTS[index % TILTS.size])
                        .size(size)
                        .softShadow(14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(4.dp, Color.White, RoundedCornerShape(14.dp)),
                )
            }
            if (photos.size > shown.size) {
                Text(
                    "+${photos.size - shown.size}",
                    style = IOSText.caption.weight(FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier
                        .offset(x = 30.dp, y = 4.dp)
                        .clip(CircleShape)
                        .background(Theme.coral)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Stable tilt per stack position, so it doesn't jump between renders. */
private val TILTS = floatArrayOf(-6f, 4f, -3f)
