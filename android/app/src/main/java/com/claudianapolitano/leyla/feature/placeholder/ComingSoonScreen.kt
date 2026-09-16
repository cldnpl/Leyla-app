package com.claudianapolitano.leyla.feature.placeholder

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight

/** Placeholder for feature tabs shipping in later phases. Port of `ComingSoonView`. */
@Composable
fun ComingSoonScreen(
    @StringRes titleRes: Int,
    icon: ImageVector,
    @StringRes blurbRes: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = Theme.coral, modifier = Modifier.size(52.dp))
        Text(leylaString(titleRes), style = IOSText.title.weight(FontWeight.Bold), color = colors.ink)
        Text(
            leylaString(blurbRes),
            style = IOSText.subheadline,
            color = colors.secondary,
            textAlign = TextAlign.Center,
        )
        Text(
            leylaString(R.string.coming_soon),
            style = IOSText.caption.weight(FontWeight.Bold),
            color = Theme.coral,
            modifier = Modifier
                .clip(CircleShape)
                .background(Theme.coral.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
