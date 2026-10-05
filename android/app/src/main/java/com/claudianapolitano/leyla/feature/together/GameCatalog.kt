package com.claudianapolitano.leyla.feature.together

/** One of the couple games shown in the Games section. Port of `GameDef`. */
data class GameDef(
    val id: String,
    val title: String,
    val subtitle: String,
    /** SF Symbol name; [sfSymbolIcon] maps it to a Material icon. */
    val icon: String,
    val colorKey: String,
    /** "MOST PLAYED", or null. */
    val badge: String?,
    val cta: String,
    val kind: Kind,
) {
    enum class Kind { HWDYKM, DEBATE, DRAW, SNAP, COMING_SOON }

    companion object {
        val all: List<GameDef> = listOf(
            GameDef(
                id = "hwdykm",
                title = "How Well Do You Know Me?",
                subtitle = "Answer privately, reveal together, see your score.",
                icon = "questionmark.circle.fill",
                colorKey = "pink",
                badge = "MOST PLAYED",
                cta = "Start the quiz",
                kind = Kind.HWDYKM,
            ),
            GameDef(
                id = "debate",
                title = "Couples Debate",
                subtitle = "Argue your assigned side. An AI judge picks a winner.",
                icon = "bubble.left.and.bubble.right.fill",
                colorKey = "blue",
                badge = null,
                cta = "Start a debate",
                kind = Kind.DEBATE,
            ),
            GameDef(
                id = "draw",
                title = "Draw Together",
                subtitle = "Same prompt, two halves of one canvas. No scores.",
                icon = "pencil.tip.crop.circle",
                colorKey = "purple",
                badge = null,
                cta = "Start drawing",
                kind = Kind.DRAW,
            ),
            GameDef(
                id = "snap",
                title = "Snap Hunt",
                subtitle = "Race to find the clue, snap it, best find wins.",
                icon = "camera.viewfinder",
                colorKey = "green",
                badge = null,
                cta = "Start a hunt",
                kind = Kind.SNAP,
            ),
        )

        fun byId(id: String): GameDef? = all.firstOrNull { it.id == id }
    }
}
