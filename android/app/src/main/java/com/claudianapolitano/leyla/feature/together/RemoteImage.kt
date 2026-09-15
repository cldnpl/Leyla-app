package com.claudianapolitano.leyla.feature.together

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.claudianapolitano.leyla.core.ApiConfig
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme

/**
 * A drawing or snap served by the backend. Port of `RemoteImage`: the media
 * routes are couple-scoped and authenticated, so the request carries the bearer
 * token the way [ApiConfig.imageRequest] sets up.
 */
@Composable
fun RemoteImage(
    path: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    SubcomposeAsyncImage(
        model = ApiConfig.imageRequest(path),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
        loading = { ImagePlaceholder() },
        error = { ImagePlaceholder(spinner = false) },
    )
}

/**
 * A catalog photo option. Unlike [RemoteImage] these are absolute URLs the
 * backend already resolved to a curated public image, so they need no token.
 */
@Composable
fun CatalogImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
    )
}

@Composable
private fun ImagePlaceholder(spinner: Boolean = true) {
    val colors = LeylaTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (spinner) {
            CircularProgressIndicator(
                color = Theme.rose,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
