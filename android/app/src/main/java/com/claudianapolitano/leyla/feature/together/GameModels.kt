package com.claudianapolitano.leyla.feature.together

import kotlinx.serialization.Serializable

/**
 * Wire models for everything on the Games tab, matching the Go JSON field for
 * field. Ports of `QuizModels.swift`, `HwdykmModels.swift`, `DebateModels.swift`,
 * `DrawModels.swift` and `SnapModels.swift`.
 *
 * Every field the backend only started sending recently is nullable with a
 * default, so an older deploy still decodes rather than blowing up the screen —
 * the same tolerance the iOS decoders were given.
 */

// MARK: - Quiz

@Serializable
data class QuizCategoriesResponse(val categories: List<QuizCategorySummary> = emptyList())

@Serializable
data class QuizCategorySummary(
    val id: String,
    val title: String,
    /** SF Symbol name; [sfSymbolIcon] maps it to a Material icon. */
    val icon: String = "sparkles",
    val colorKey: String = "pink",
    val quizCount: Int = 0,
    /** Quizzes I've finished. */
    val completedCount: Int = 0,
    /**
     * 0..1 for the two of us: a quiz counts half when one of us has answered it
     * and fully once both have, because comparing is the point.
     */
    val progress: Double = 0.0,
    // Sent only by a backend that knows about the partner's half.
    val partnerCompletedCount: Int? = null,
    val yourTurnCount: Int? = null,
    val myProgress: Double? = null,
) {
    /** Quizzes my partner has finished and I haven't — my move. */
    val waitingForMe: Int get() = yourTurnCount ?: 0
}

@Serializable
data class QuizCategoryDetail(
    val id: String,
    val title: String,
    val icon: String = "sparkles",
    val colorKey: String = "pink",
    val quizzes: List<QuizSummary> = emptyList(),
)

@Serializable
data class QuizSummary(
    val id: String,
    val title: String,
    val icon: String = "sparkles",
    /** [QuizFormat] raw value. */
    val format: String = "",
    /** "WEDDING", "18+", … */
    val tag: String? = null,
    val questionCount: Int = 0,
    val myDone: Boolean = false,
    val partnerDone: Boolean = false,
)

@Serializable
data class QuizDetail(
    val id: String,
    val title: String,
    val icon: String = "sparkles",
    val format: String = "",
    val tag: String? = null,
    val questions: List<QuizQuestion> = emptyList(),
)

@Serializable
data class QuizDaily(
    val date: String = "",
    val categoryId: String = "",
    val categoryTitle: String = "",
    val colorKey: String = "pink",
    val icon: String = "sparkles",
    val quizTitle: String = "",
    val question: QuizQuestion,
)

@Serializable
data class QuizQuestion(
    val id: String,
    val prompt: String,
    /** "open" | "choice" */
    val type: String = "open",
    val options: List<QuizOption>? = null,
    /** Option id (choice) or free text (open). */
    val myAnswer: String? = null,
    /** Present only once I've answered this one. */
    val partnerAnswer: String? = null,
    /**
     * Whether my partner has answered — known even before I answer, so a card
     * can say "your turn" without revealing what they picked.
     */
    val partnerAnswered: Boolean? = null,
    val bothAnswered: Boolean = false,
) {
    /** True when they've answered and I haven't: my move. */
    val isMyTurn: Boolean get() = myAnswer == null && partnerAnswered == true

    val isChoice: Boolean get() = type == "choice"

    /** True when this question's options carry photos (render as image cards). */
    val hasPhotos: Boolean get() = options?.any { it.image != null } == true

    /**
     * Look up the option matching a stored answer id, to show its label/icon/
     * photo in compare. Ids are stable across languages; labels are not, so
     * they must never be used to match — see catalogOption in the Go catalog.
     */
    fun option(id: String?): QuizOption? {
        if (id == null) return null
        return options?.firstOrNull { it.id == id }
    }
}

@Serializable
data class QuizOption(
    /**
     * Older backend deploys omit this entirely; the label stands in so the card
     * still decodes. [stableId] is what call sites should compare on.
     */
    val id: String? = null,
    val label: String,
    /** SF Symbol name. */
    val icon: String? = null,
    /** The backend resolves photo keywords to concrete, curated image URLs. */
    val image: String? = null,
) {
    val stableId: String get() = id ?: label
    val imageUrl: String? get() = image?.takeIf { it.isNotEmpty() }
}

/** Badge label shown on the quiz card, like Couple Joy. */
enum class QuizFormat(val raw: String, val badge: String) {
    THIS_OR_THAT("thisOrThat", "THIS OR THAT"),
    DEEP_CONVERSATION("deepConversation", "DEEP CONVERSATION"),
    WHICH_DO_YOU_PREFER("whichDoYouPrefer", "WHICH DO YOU PREFER?");

    companion object {
        fun badgeFor(raw: String): String =
            entries.firstOrNull { it.raw == raw }?.badge ?: raw.uppercase()
    }
}

// MARK: - How Well Do You Know Me

@Serializable
data class HwdykmPacksResponse(val packs: List<HwdykmPackSummary> = emptyList())

@Serializable
data class HwdykmPackSummary(
    val id: String,
    val title: String,
    val icon: String = "questionmark.circle.fill",
    val colorKey: String = "pink",
    val tag: String = "",
    val questionCount: Int = 0,
    val myDone: Boolean = false,
    /** Sent only by a backend that reports the partner's half. */
    val partnerDone: Boolean? = null,
    val bothDone: Boolean = false,
) {
    /** They finished this pack and I haven't — my move. */
    val isMyTurn: Boolean get() = !myDone && partnerDone == true
}

@Serializable
data class HwdykmPackDetail(
    val id: String,
    val title: String,
    val icon: String = "questionmark.circle.fill",
    val colorKey: String = "pink",
    val tag: String = "",
    val myDone: Boolean = false,
    val bothDone: Boolean = false,
    val score: Int = 0,
    val questions: List<HwdykmQuestion> = emptyList(),
)

@Serializable
data class HwdykmOption(val id: String, val label: String)

@Serializable
data class HwdykmQuestion(
    val id: String,
    val prompt: String,
    val options: List<HwdykmOption> = emptyList(),
    /** true → I answer honestly; false → I guess my partner. */
    val subjectIsMe: Boolean = true,
    /** Option id. */
    val myAnswer: String? = null,
    /** Option id, reveal only. */
    val honestAnswer: String? = null,
    /** Option id, reveal only. */
    val guess: String? = null,
    val matched: Boolean = false,
) {
    /**
     * Look up the option matching a stored answer id, for display. Ids are
     * stable across languages; labels are not, so never match on label.
     */
    fun option(id: String?): HwdykmOption? {
        if (id == null) return null
        return options.firstOrNull { it.id == id }
    }
}

// MARK: - Couples Debate

@Serializable
data class DebatePacksResponse(val packs: List<DebatePackSummary> = emptyList())

@Serializable
data class DebatePackSummary(
    val id: String,
    val title: String,
    val icon: String = "bubble.left.and.bubble.right.fill",
    val colorKey: String = "blue",
    val tag: String = "",
    val roundCount: Int = 0,
    val myDone: Boolean = false,
    /** Sent only by a backend that reports the partner's half. */
    val partnerDone: Boolean? = null,
    val bothDone: Boolean = false,
) {
    /** They finished this pack and I haven't — my move. */
    val isMyTurn: Boolean get() = !myDone && partnerDone == true
}

@Serializable
data class DebatePackDetail(
    val id: String,
    val title: String,
    val icon: String = "bubble.left.and.bubble.right.fill",
    val colorKey: String = "blue",
    val tag: String = "",
    val myDone: Boolean = false,
    val bothDone: Boolean = false,
    /** "me" | "partner" | "tie", when bothDone. */
    val overallWinner: String? = null,
    val myWins: Int = 0,
    val partnerWins: Int = 0,
    val rounds: List<DebateRound> = emptyList(),
)

@Serializable
data class DebateRound(
    val id: String,
    /**
     * The one statement both partners argue about. There are no assigned sides:
     * you each make your own case for the same prompt, and the judge compares
     * the two answers.
     */
    val motion: String,
    val myArgument: String? = null,
    /** Revealed once both have argued. */
    val partnerArgument: String? = null,
    val judged: Boolean = false,
    val myScore: Int? = null,
    val partnerScore: Int? = null,
    /** "me" | "partner" | "tie" */
    val roundWinner: String? = null,
    val verdict: String? = null,
)

// MARK: - Draw Together

@Serializable
data class DrawRound(
    val roundId: String,
    val prompt: String,
    val mySubmitted: Boolean = false,
    val partnerSubmitted: Boolean = false,
    /** Both submitted → drawings shown. */
    val revealed: Boolean = false,
    val myImagePath: String? = null,
    val partnerImagePath: String? = null,
)

// MARK: - Snap Hunt

@Serializable
data class SnapRound(
    val roundId: String,
    val clue: String,
    val mySubmitted: Boolean = false,
    val partnerSubmitted: Boolean = false,
    val revealed: Boolean = false,
    val myImagePath: String? = null,
    val partnerImagePath: String? = null,
    /** "me" | "partner" | "tie", when revealed. */
    val outcome: String? = null,
    val reason: String? = null,
    /**
     * Older deployed API versions do not include this presentation hint yet.
     * Decode those rounds too so a staged release never blocks Snap Hunt.
     */
    val startedByMe: Boolean = true,
)
