package com.claudianapolitano.leyla.feature.journal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.core.Milestone
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaCard
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.plainClickable
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The editable Milestones card at the top of the Journal. Rows can be tapped to
 * edit inline (title + date), long-pressed to delete, and a trailing
 * "Add milestone" reveals the same inline editor — with tappable suggestions —
 * right below the existing ones. Port of `MilestonesSection`.
 */
private val SUGGESTIONS = listOf(
    R.string.milestone_first_date,
    R.string.milestone_first_kiss,
    R.string.milestone_first_i_love_you,
    R.string.milestone_first_trip,
    R.string.milestone_anniversary,
    R.string.milestone_moved_in,
    R.string.milestone_met_family,
    R.string.milestone_got_engaged,
)

/** nil = not editing, "" = adding a new one, otherwise the id being edited. */
private const val ADDING = ""

@Composable
fun MilestonesSection(
    milestones: List<Milestone>,
    onAdd: (String, LocalDate, () -> Unit) -> Unit,
    onUpdate: (String, String, LocalDate, () -> Unit) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    var editingId by remember { mutableStateOf<String?>(null) }
    var draftTitle by remember { mutableStateOf("") }
    var draftDate by remember { mutableStateOf(LocalDate.now()) }
    var saving by remember { mutableStateOf(false) }

    fun finishEditing() {
        editingId = null
        draftTitle = ""
        saving = false
    }

    // Tabs keep their children alive, so a half-completed editor would otherwise
    // still be open on the way back from another tab.
    val close by rememberUpdatedState(::finishEditing)
    DisposableEffect(Unit) { onDispose { close() } }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                leylaString(R.string.milestones),
                style = IOSText.title2.weight(FontWeight.ExtraBold),
                color = colors.ink,
            )
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Theme.rose, modifier = Modifier.size(18.dp))
        }

        LeylaCard {
            Column {
                if (milestones.isEmpty() && editingId != ADDING) {
                    Text(
                        leylaString(R.string.milestones_empty),
                        style = IOSText.subheadline,
                        color = colors.secondary,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }

                milestones.forEachIndexed { index, milestone ->
                    if (editingId == milestone.id) {
                        MilestoneEditor(
                            title = draftTitle,
                            onTitleChange = { draftTitle = it },
                            date = draftDate,
                            onDateChange = { draftDate = it },
                            saveLabel = leylaString(R.string.save),
                            saving = saving,
                            onCancel = ::finishEditing,
                            onSave = {
                                saving = true
                                onUpdate(milestone.id, draftTitle.trim(), draftDate, ::finishEditing)
                            },
                        )
                    } else {
                        MilestoneRow(
                            milestone = milestone,
                            onEdit = {
                                draftTitle = milestone.title
                                draftDate = JournalDates.day(milestone.date) ?: LocalDate.now()
                                editingId = milestone.id
                            },
                            onDelete = { onDelete(milestone.id) },
                        )
                        if (index < milestones.lastIndex || editingId == ADDING) {
                            HorizontalDivider(color = colors.hairline)
                        }
                    }
                }

                if (editingId == ADDING) {
                    MilestoneEditor(
                        title = draftTitle,
                        onTitleChange = { draftTitle = it },
                        date = draftDate,
                        onDateChange = { draftDate = it },
                        saveLabel = leylaString(R.string.add),
                        saving = saving,
                        onCancel = ::finishEditing,
                        onSave = {
                            saving = true
                            onAdd(draftTitle.trim(), draftDate, ::finishEditing)
                        },
                    )
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .plainClickable {
                                draftTitle = ""
                                draftDate = LocalDate.now()
                                editingId = ADDING
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, tint = Theme.coral, modifier = Modifier.size(22.dp))
                        Text(
                            leylaString(R.string.milestone_add),
                            style = IOSText.subheadline.weight(FontWeight.SemiBold),
                            color = Theme.coral,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MilestoneRow(milestone: Milestone, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LeylaTheme.colors
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onLongClick = { menuOpen = true },
                    onClick = onEdit,
                )
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    // Aligns the bullet with the title's centre.
                    .padding(top = 8.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Theme.rose),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(milestone.title, style = IOSText.headline, color = colors.ink)
                Text(
                    JournalDates.day(milestone.date)?.let(JournalDates::medium).orEmpty(),
                    style = IOSText.subheadline,
                    color = Theme.coral,
                )
            }
            Icon(
                Icons.Filled.Edit,
                contentDescription = null,
                tint = colors.secondary.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp).size(16.dp),
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(leylaString(R.string.delete), color = Theme.coral) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Theme.coral) },
                onClick = { menuOpen = false; onDelete() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilestoneEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    saveLabel: String,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = LeylaTheme.colors
    var showPicker by remember { mutableStateOf(false) }
    val canSave = title.isNotBlank() && !saving

    Column(
        Modifier.padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text(leylaString(R.string.milestone_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SUGGESTIONS.forEach { res ->
                val suggestion = leylaString(res)
                Text(
                    suggestion,
                    style = IOSText.caption.weight(FontWeight.SemiBold),
                    color = colors.ink,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Theme.blush.copy(alpha = 0.30f))
                        .plainClickable { onTitleChange(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .plainClickable { showPicker = true }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(leylaString(R.string.date), style = IOSText.subheadline, color = colors.ink)
            Text(
                JournalDates.medium(date),
                style = IOSText.subheadline.weight(FontWeight.SemiBold),
                color = Theme.coral,
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                leylaString(R.string.cancel),
                style = IOSText.subheadline,
                color = colors.secondary,
                modifier = Modifier.plainClickable(enabled = !saving, onClick = onCancel),
            )
            Box(Modifier.weight(1f))
            Text(
                if (saving) leylaString(R.string.saving) else saveLabel,
                style = IOSText.subheadline.weight(FontWeight.SemiBold),
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (canSave) Theme.coral else Theme.coral.copy(alpha = 0.4f))
                    .plainClickable(enabled = canSave, onClick = onSave)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDateChange(
                            java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                    showPicker = false
                }) { Text(leylaString(R.string.done)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(leylaString(R.string.cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
