package com.claudianapolitano.leyla.feature.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaCard
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import kotlinx.coroutines.launch

/**
 * The screen behind Home's cycle card. Port of the part of `CycleDetailView`
 * the Android build can stand behind today: the "do you have a cycle?" answer,
 * the partner's shared phase with its supportive tips, and an honest note about
 * what personal tracking is still waiting on.
 *
 * iOS reads the wearer's own cycle from HealthKit. Android's counterpart is
 * Health Connect, and until that is wired the personal half of this screen says
 * so rather than showing an empty chart.
 */
@Composable
fun CycleDetailScreen(modifier: Modifier = Modifier) {
    val cycle by CycleState.snapshot.collectAsStateWithLifecycle()
    val session by Session.snapshot.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val scope = rememberCoroutineScope()
    val partnerName = session.partner?.displayName ?: stringResource(R.string.your_partner)

    LaunchedEffect(Unit) { CycleState.refreshOnAppear() }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // The question itself, always answerable here — it decides which half of
        // the feature the whole app shows you.
        LeylaCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.profile_i_have_a_cycle),
                        style = IOSText.body,
                        color = colors.ink,
                    )
                    Text(
                        stringResource(R.string.profile_cycle_footer),
                        style = IOSText.caption,
                        color = colors.secondary,
                    )
                }
                Switch(
                    checked = cycle.userHasCycle == true,
                    onCheckedChange = { scope.launch { CycleState.setUserHasCycle(it) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = Theme.rose),
                )
            }
        }

        when {
            // She's tracking a pregnancy.
            cycle.isPregnant && cycle.pregnancyInsights != null ->
                PregnancyHomeCard(
                    insights = cycle.pregnancyInsights!!,
                    title = stringResource(R.string.pregnancy_yours),
                )

            // Her own cycle, once Health Connect is feeding it.
            cycle.userHasCycle == true && cycle.insights != null ->
                SelfCycleCard(insights = cycle.insights!!)

            // She said yes but nothing is connected yet.
            cycle.userHasCycle == true -> LeylaCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Theme.rose,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Theme.rose.copy(alpha = 0.14f))
                            .padding(8.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.cycle_setup_title),
                            style = IOSText.headline,
                            color = colors.ink,
                        )
                        Text(
                            stringResource(R.string.cycle_health_connect_pending),
                            style = IOSText.footnote,
                            color = colors.secondary,
                        )
                    }
                }
            }

            // He's supporting a partner who has one: her phase and the tips.
            else -> PartnerPeriodCard(partner = cycle.partner, partnerName = partnerName)
        }

        cycle.partnerPregnancy?.let { pregnancy ->
            if (pregnancy.sharing) {
                PregnancyEngine.parseDueDate(pregnancy.dueDate)?.let { due ->
                    PregnancyHomeCard(
                        insights = PregnancyEngine.insights(due),
                        title = stringResource(R.string.pregnancy_partner_expecting, partnerName),
                    )
                }
            }
        }

        Text(
            stringResource(R.string.cycle_privacy_note),
            style = IOSText.caption,
            color = colors.secondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
