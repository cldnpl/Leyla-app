package com.claudianapolitano.leyla.feature.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.claudianapolitano.leyla.core.MediaItem
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.feature.together.RemoteImage
import com.claudianapolitano.leyla.feature.together.plainClickable

/**
 * Fullscreen, swipeable viewer for a day's photos. Own photos can be deleted
 * from here — a photo's uploader is the only one who may remove it. Port of
 * `JournalPhotoPager`.
 *
 * iOS presents this as a `fullScreenCover`, which covers the tab bar too. The
 * Android equivalent is an undecorated full-screen dialog: rendered inside the
 * tab's own content it would sit above the pages but *below* the tab bar, which
 * is not what a photo viewer should do.
 */
@Composable
fun JournalPhotoPager(
    photos: List<MediaItem>,
    startIndex: Int,
    currentUserId: String?,
    onDelete: (MediaItem, onDone: (Boolean) -> Unit) -> Unit,
    onClose: () -> Unit,
) {
    val items = remember(photos) { mutableStateListOf<MediaItem>().apply { addAll(photos) } }
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)),
    ) { items.size }

    // Removing the last photo leaves nothing to look at.
    if (items.isEmpty()) {
        onClose()
        return
    }

    // A photo's uploader is the only one who may remove it, so the bin only
    // exists when the photo on screen is one of yours.
    val deletable = items.getOrNull(pagerState.currentPage)
        ?.takeIf { currentUserId != null && it.uploaderId == currentUserId }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                items.getOrNull(page)?.let {
                    RemoteImage(
                        path = it.fileUrl,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PagerAction(Icons.Filled.Close, onClick = onClose)
                Box(Modifier.weight(1f))
                if (items.size > 1) {
                    Text(
                        "${pagerState.currentPage + 1} / ${items.size}",
                        style = IOSText.footnote,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
                Box(Modifier.weight(1f))
                if (deletable != null) {
                    PagerAction(Icons.Filled.Delete, onClick = {
                        onDelete(deletable) { removed ->
                            // Leave the viewer open if the delete failed.
                            if (removed) items.remove(deletable)
                        }
                    })
                } else {
                    // Keeps the close button hard left rather than drifting centre.
                    Box(Modifier.size(36.dp))
                }
            }
        }
    }
}

@Composable
private fun PagerAction(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .plainClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
    }
}
