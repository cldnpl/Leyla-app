package com.claudianapolitano.leyla.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.AppLanguage
import com.claudianapolitano.leyla.core.LanguageManager
import com.claudianapolitano.leyla.core.Translations
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.plainClickable
import kotlinx.coroutines.launch

/**
 * Pick the language Leyla is displayed in. Port of
 * `Us/Features/Settings/LanguagePickerView.swift`.
 *
 * Each row shows the language's own name first, because someone scanning for
 * their language recognises "Русский" faster than "Russian". Search matches
 * either name, so both "deutsch" and "german" find German.
 */
@Composable
fun LanguagePickerScreen(modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    val scope = rememberCoroutineScope()
    val current by LanguageManager.current.collectAsStateWithLifecycle()
    val isLoading by Translations.isLoading.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val trimmed = query.trim()
    val filtered = remember(trimmed) {
        if (trimmed.isEmpty()) {
            AppLanguage.all
        } else {
            AppLanguage.all.filter {
                it.endonym.contains(trimmed, ignoreCase = true) ||
                    it.englishName.contains(trimmed, ignoreCase = true)
            }
        }
    }

    Box(modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(leylaString(R.string.settings_search_languages)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = colors.secondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, bottom = 32.dp,
                ),
            ) {
                items(filtered, key = { it.code }) { language ->
                    LanguageRow(
                        language = language,
                        selected = language == current,
                        enabled = !isLoading,
                        onSelect = { scope.launch { LanguageManager.select(language) } },
                    )
                    if (language != filtered.last()) HorizontalDivider(color = colors.hairline)
                }
            }
        }

        // Blocking, not inline: every string on screen is about to change at
        // once, so a partial re-render mid-fetch would look broken rather than
        // just slow.
        if (isLoading) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .background(colors.card, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                ) {
                    CircularProgressIndicator(color = Theme.rose)
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val colors = LeylaTheme.colors
    // The list itself is always left-to-right even when the app is mirrored:
    // it's a list of languages, not localised content.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier
                .fillMaxWidth()
                .plainClickable(enabled = enabled, onClick = onSelect)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(language.endonym, style = IOSText.body, color = colors.ink)
                // Skip the redundant subtitle when both names are identical
                // (English, Filipino, …).
                if (language.englishName != language.endonym) {
                    Text(language.englishName, style = IOSText.footnote, color = colors.secondary)
                }
            }
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Theme.rose,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** The endonym shown as the Settings row's detail, e.g. "Italiano". */
@Composable
fun currentLanguageName(): String {
    val current by LanguageManager.current.collectAsStateWithLifecycle()
    return current.endonym
}
