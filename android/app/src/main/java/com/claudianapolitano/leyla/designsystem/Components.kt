package com.claudianapolitano.leyla.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.core.ApiConfig

/**
 * `.shadow(color: .black.opacity(0.06), radius: 12, y: 6)` from the iOS cards.
 *
 * Compose draws shadows from an elevation rather than a radius/offset pair, so
 * the numbers can't be copied across literally; 6.dp with a softened spot
 * colour lands on the same barely-there lift.
 */
fun Modifier.softShadow(cornerRadius: Dp): Modifier = shadow(
    elevation = 6.dp,
    shape = RoundedCornerShape(cornerRadius),
    // Clipped, so the shadow stays outside the card instead of tinting a ring
    // inside its own edges.
    clip = true,
    ambientColor = Color.Black.copy(alpha = 0.30f),
    spotColor = Color.Black.copy(alpha = 0.30f),
)

/**
 * A rounded, subtly shadowed card container. Port of the iOS `Card` view:
 * 20pt padding, 24pt corners, a soft drop shadow.
 *
 * iOS fills this with `.regularMaterial`, a live blur of whatever sits behind
 * it. Compose has no stock material, so [LeylaColors.card] stands in — a
 * translucent plate over the same warm backdrop, which lands in the same place
 * visually because the backdrop is a slow gradient rather than busy content.
 */
@Composable
fun LeylaCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LeylaTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(cornerRadius),
        shape = RoundedCornerShape(cornerRadius),
        color = colors.card,
        border = BorderStroke(1.dp, colors.hairline),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/**
 * The app wordmark shown at the top-left of Home. The asset is a template
 * image (alpha only), so it is tinted with the brand rose to read on the
 * translucent top bar — the same treatment as iOS's `.renderingMode(.template)`.
 */
@Composable
fun BrandLogo(modifier: Modifier = Modifier, color: Color = Theme.rose) {
    val description = leylaString(R.string.leyla_logo_description)
    Image(
        painter = painterResource(R.drawable.leyla_logo),
        contentDescription = description,
        colorFilter = ColorFilter.tint(color),
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(30.dp)
            .semantics { heading() },
    )
}

/**
 * A circular profile photo. Loads the authenticated avatar image for a user (or
 * partner) from the backend; if none has been set, falls back to the brand
 * heart mark so the placeholder stays warm rather than an empty grey circle.
 *
 * Path convention: `/v1/users/{userId}/avatar` — matches the server route.
 * Callers pass the `avatarPath` copied from `User`; null means no photo.
 */
@Composable
fun Avatar(
    path: String?,
    modifier: Modifier = Modifier,
    name: String? = null,
    size: Dp = 40.dp,
) {
    val colors = LeylaTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Theme.rose.copy(alpha = 0.18f))
            .border(1.dp, colors.hairline, CircleShape)
            .semantics { contentDescription = name?.let { "$it avatar" } ?: "Avatar" },
        contentAlignment = Alignment.Center,
    ) {
        if (path != null) {
            AsyncImage(
                model = ApiConfig.imageRequest(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Theme.rose,
                modifier = Modifier.size(size * 0.42f),
            )
        }
    }
}
