package com.claudianapolitano.leyla.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claudianapolitano.leyla.core.ApiException
import com.claudianapolitano.leyla.core.JournalEntry
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.MediaItem
import com.claudianapolitano.leyla.core.Milestone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One calendar day of the diary — both partners' entries for that date. */
data class JournalDay(
    /** `yyyy-MM-dd`, the grouping key. */
    val id: String,
    val date: LocalDate,
    val entries: List<JournalEntry>,
) {
    companion object {
        /**
         * Groups server-ordered entries (already date DESC) into days,
         * preserving that newest-first order. Port of `JournalDay.group`.
         */
        fun group(entries: List<JournalEntry>): List<JournalDay> {
            val order = mutableListOf<String>()
            val buckets = linkedMapOf<String, MutableList<JournalEntry>>()
            val days = mutableMapOf<String, LocalDate>()

            for (entry in entries) {
                val date = JournalDates.day(entry.date) ?: continue
                val key = JournalDates.dayKey(date)
                if (buckets[key] == null) {
                    order += key
                    days[key] = date
                }
                buckets.getOrPut(key) { mutableListOf() } += entry
            }
            return order.mapNotNull { key ->
                val items = buckets[key] ?: return@mapNotNull null
                JournalDay(id = key, date = days.getValue(key), entries = items)
            }
        }
    }
}

/** One month of diary photos, in the journal's own order. Port of `JournalPhotoMonth`. */
data class JournalPhotoMonth(
    /** `yyyy-MM`. */
    val id: String,
    val date: LocalDate,
    val photos: List<MediaItem>,
) {
    companion object {
        /**
         * Flattens the entries' photos into months, newest first. Entries arrive
         * date DESC from the server, so preserving that order keeps the gallery
         * and the diary pages telling the story the same way round.
         */
        fun group(entries: List<JournalEntry>): List<JournalPhotoMonth> {
            val order = mutableListOf<String>()
            val buckets = mutableMapOf<String, MutableList<MediaItem>>()
            val dates = mutableMapOf<String, LocalDate>()

            for (entry in entries) {
                if (entry.photos.isEmpty()) continue
                val date = JournalDates.day(entry.date) ?: continue
                val key = JournalDates.monthKey(date)
                if (buckets[key] == null) {
                    order += key
                    dates[key] = date
                }
                buckets.getOrPut(key) { mutableListOf() } += entry.photos
            }
            return order.mapNotNull { key ->
                val photos = buckets[key] ?: return@mapNotNull null
                JournalPhotoMonth(id = key, date = dates.getValue(key), photos = photos)
            }
        }
    }
}

data class JournalUiState(
    val milestones: List<Milestone> = emptyList(),
    val entries: List<JournalEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val days: List<JournalDay> get() = JournalDay.group(entries)
    val photoMonths: List<JournalPhotoMonth> get() = JournalPhotoMonth.group(entries)
}

/**
 * Owns the Journal tab's data: the couple's milestones and the shared diary.
 * Port of the data half of `JournalView`.
 */
class JournalViewModel : ViewModel() {

    private val _state = MutableStateFlow(JournalUiState())
    val state: StateFlow<JournalUiState> = _state.asStateFlow()

    init { reload() }

    /**
     * Fetches both sections independently, so a failure in one never blanks the
     * other — losing the whole diary because the milestones call timed out would
     * be the wrong trade.
     */
    fun reload() {
        viewModelScope.launch {
            runCatching { LeylaApi.listMilestones() }
                .onSuccess { list -> _state.update { it.copy(milestones = list, errorMessage = null) } }
                .onFailure { e -> report(e) }

            runCatching { LeylaApi.listJournal() }
                .onSuccess { list -> _state.update { it.copy(entries = list, errorMessage = null) } }
                .onFailure { e -> report(e) }

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun dismissError() = _state.update { it.copy(errorMessage = null) }

    // MARK: - Milestones

    fun addMilestone(title: String, date: LocalDate, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { LeylaApi.createMilestone(title, JournalDates.isoDay(date)) }
                .onSuccess { reload(); onDone() }
                .onFailure { report(it); onDone() }
        }
    }

    fun updateMilestone(id: String, title: String, date: LocalDate, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { LeylaApi.updateMilestone(id, title, JournalDates.isoDay(date)) }
                .onSuccess { reload(); onDone() }
                .onFailure { report(it); onDone() }
        }
    }

    /** Same immediate removal as the diary entries, same rollback on failure. */
    fun deleteMilestone(id: String) {
        val previous = _state.value.milestones
        _state.update { it.copy(milestones = it.milestones.filterNot { m -> m.id == id }) }
        viewModelScope.launch {
            runCatching { LeylaApi.deleteMilestone(id) }
                .onSuccess { reload() }
                .onFailure {
                    _state.update { s -> s.copy(milestones = previous) }
                    report(it)
                }
        }
    }

    // MARK: - Diary entries

    /**
     * Removes the entry from the feed at once, then confirms with the server.
     *
     * A delete you just asked for shouldn't sit there for a round trip before
     * looking like it happened. If the server refuses, the entry comes back
     * exactly where it was and the message says why.
     */
    fun deleteEntry(entry: JournalEntry) {
        val previous = _state.value.entries
        _state.update { it.copy(entries = it.entries.filterNot { e -> e.id == entry.id }) }
        viewModelScope.launch {
            runCatching { LeylaApi.deleteJournalEntry(entry.id) }
                .onSuccess { reload() }
                .onFailure {
                    _state.update { s -> s.copy(entries = previous) }
                    report(it)
                }
        }
    }

    // MARK: - Photos

    /** Deleting a photo changes an entry, so the feed is refetched after it. */
    fun deletePhoto(id: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { LeylaApi.deletePhoto(id) }
                .onSuccess { reload(); onDone(true) }
                .onFailure { report(it); onDone(false) }
        }
    }

    private fun report(error: Throwable) {
        val message = (error as? ApiException)?.payload?.message
            ?: error.message
            ?: "Something went wrong"
        _state.update { it.copy(errorMessage = message) }
    }
}
