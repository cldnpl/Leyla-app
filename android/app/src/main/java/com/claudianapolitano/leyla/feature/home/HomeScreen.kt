package com.claudianapolitano.leyla.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.designsystem.BrandLogo
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.softShadow
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.cycle.CycleSetupCard
import com.claudianapolitano.leyla.feature.cycle.PartnerPeriodCard
import com.claudianapolitano.leyla.feature.cycle.PregnancyHomeCard
import com.claudianapolitano.leyla.feature.cycle.SelfCycleCard

/**
 * Home: hero → map → cycle. Port of `Us/Features/Home/HomeView.swift`.
 *
 * The destinations it reaches live in [com.claudianapolitano.leyla.feature.home.HomeTab],
 * which owns the stack — this screen only says which one was asked for, the way
 * the SwiftUI `NavigationLink`s do.
 */
@Composable
fun HomeScreen(
    onAddWidget: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenCycle: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.onAppear(context) }

    Box(modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            HomeTopBar(onAddWidget = onAddWidget, onEditProfile = onEditProfile)
            HomeContent(state, viewModel, onOpenMap = onOpenMap, onOpenCycle = onOpenCycle)
        }
    }
}

/**
 * Logo left, "add widget" pill centred, profile button right.
 *
 * Material's `TopAppBar` lays its title out after the navigation icon, which
 * would push the pill off-centre; iOS puts it in the `.principal` slot, dead
 * centre regardless of what flanks it. A Box gets that exactly, so the two
 * builds' chrome lines up.
 */
@Composable
private fun HomeTopBar(onAddWidget: () -> Unit, onEditProfile: () -> Unit) {
    val colors = LeylaTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        BrandLogo(Modifier.align(Alignment.CenterStart))
        AddWidgetPill(onClick = onAddWidget, modifier = Modifier.align(Alignment.Center))
        IconButton(
            onClick = onEditProfile,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = leylaString(R.string.edit_profile),
                tint = colors.ink,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    viewModel: HomeViewModel,
    onOpenMap: () -> Unit,
    onOpenCycle: () -> Unit,
) {
    val colors = LeylaTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        HeroButton(
            title = leylaString(state.heroTitleRes, state.partnerName),
            partnerName = state.partnerName,
            isSent = state.missYouSent,
            isSending = state.isSending,
            onClick = viewModel::sendMissYou,
            modifier = Modifier.padding(top = 28.dp),
        )

        MapSection(state, onOpenMap = onOpenMap)

        CycleSection(state, onOpenCycle = onOpenCycle)

        state.errorMessage?.let { message ->
            Text(
                message,
                style = IOSText.footnote,
                color = Theme.coral,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// MARK: - Hero "miss you" button

@Composable
private fun HeroButton(
    title: String,
    partnerName: String,
    isSent: Boolean,
    isSending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sentLabel = leylaString(R.string.hero_sent)
    val sentAccessibility = leylaString(R.string.hero_sent_accessibility)
    val sendAccessibility = leylaString(R.string.hero_send_accessibility, partnerName)
    val enabled = !isSending && !isSent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(32.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Theme.roseGradient)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .heightIn(min = 180.dp)
            .padding(24.dp)
            .semantics { contentDescription = if (isSent) sentAccessibility else sendAccessibility },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Crossfade(isSent, label = "heart") { sent ->
                Icon(
                    if (sent) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }
            Text(
                text = if (isSent) sentLabel else title,
                style = IOSText.title3.weight(FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isSending) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(38.dp))
        }
    }
}

/** Pill in the top bar (between the Leyla logo and the profile icon). */
@Composable
private fun AddWidgetPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(CircleShape)
            .background(Theme.rose.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Theme.rose, modifier = Modifier.size(14.dp))
        Text(
            leylaString(R.string.add_widget),
            style = IOSText.footnote.weight(FontWeight.SemiBold),
            color = Theme.rose,
        )
    }
}

// MARK: - Map section (tap to expand)

@Composable
private fun MapSection(state: HomeUiState, onOpenMap: () -> Unit) {
    val mine = state.mapMine
    val partner = state.mapPartner
    if (mine != null && partner != null) {
        DistanceMapCard(
            mine = mine,
            partner = partner,
            myName = state.myName,
            partnerName = state.partnerName,
            km = state.mapKm ?: 0.0,
            myAvatarPath = state.myAvatarPath,
            partnerAvatarPath = state.partnerAvatarPath,
            onClick = onOpenMap,
        )
    } else {
        ShareLocationCard(partnerName = state.partnerName, onClick = onOpenMap)
    }
}

@Composable
private fun ShareLocationCard(partnerName: String, onClick: () -> Unit) {
    val colors = LeylaTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().softShadow(26.dp),
        shape = RoundedCornerShape(26.dp),
        color = colors.card,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Map, contentDescription = null, tint = Theme.rose, modifier = Modifier.size(28.dp))
            Text(leylaString(R.string.map_see_each_other), style = IOSText.headline, color = colors.ink)
            Text(
                leylaString(R.string.map_turn_on_location, partnerName),
                style = IOSText.footnote,
                color = colors.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// MARK: - Cycle cards (partner's shared cycle + your own, when available)

@Composable
private fun CycleSection(state: HomeUiState, onOpenCycle: () -> Unit) {
    val clickable = Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onOpenCycle,
    )
    val cycle = state.cycle
    when {
        // She's tracking a pregnancy → show that instead of the cycle.
        cycle.userHasCycle == true && cycle.isPregnant && cycle.pregnancyInsights != null ->
            PregnancyHomeCard(
                insights = cycle.pregnancyInsights,
                title = leylaString(R.string.pregnancy_yours),
                modifier = clickable,
            )

        // His partner is expecting and sharing it.
        cycle.userHasCycle == false && state.partnerPregnancyInsights != null ->
            PregnancyHomeCard(
                insights = state.partnerPregnancyInsights,
                title = leylaString(R.string.pregnancy_partner_expecting, state.partnerName),
                modifier = clickable,
            )

        // He doesn't have a cycle → her current phase + supportive tips.
        cycle.userHasCycle == false ->
            PartnerPeriodCard(
                partner = cycle.partner,
                partnerName = state.partnerName,
                modifier = clickable,
            )

        // She has a cycle → her own tracking (connect Health, then insights).
        cycle.userHasCycle == true && cycle.insights != null ->
            SelfCycleCard(insights = cycle.insights, modifier = clickable)

        // Not answered yet (existing users) → neutral entry to the question.
        else -> CycleSetupCard(modifier = clickable)
    }
}
