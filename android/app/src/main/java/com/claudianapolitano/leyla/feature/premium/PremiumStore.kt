package com.claudianapolitano.leyla.feature.premium

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns Leyla Premium: what's free, what's locked, and the entitlement state.
 * Port of `Us/Features/Premium/PremiumStore.swift`.
 *
 * Free tier: the first two quiz categories (Starters, Relationship) and the
 * How Well Do You Know Me? game. Everything else opens the paywall.
 *
 * The iOS build settles entitlement with StoreKit 2. The Android half of that —
 * Play Billing plus a subscription product in the Play Console — is not wired
 * yet, so [isPremium] currently reflects only the developer unlock. The gating
 * questions below are the part the rest of the app asks, and they are final:
 * when billing lands it only has to publish into [setEntitled].
 */
object PremiumStore {

    /** Quiz categories playable without a subscription (backend catalog ids). */
    val freeQuizCategoryIds = setOf("starters", "relationship")

    /** Games playable without a subscription ([com.claudianapolitano.leyla.feature.together.GameDef.id]). */
    val freeGameIds = setOf("hwdykm")

    /** The configured price, shown until a store can quote a localised one. */
    const val DISPLAY_PRICE = "€2.99"

    val priceLine: String get() = "$DISPLAY_PRICE / month"

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // MARK: - Gating

    /**
     * Callers pass the entitlement they are already observing rather than
     * having it read off the flow here: a composable that locks a card without
     * reading [isPremium] would never recompose when a purchase lands.
     */
    fun isQuizCategoryLocked(categoryId: String, isPremium: Boolean): Boolean =
        !isPremium && categoryId !in freeQuizCategoryIds

    fun isGameLocked(gameId: String, isPremium: Boolean): Boolean =
        !isPremium && gameId !in freeGameIds

    // MARK: - Entitlement

    /** Where Play Billing will publish its answer once it is wired up. */
    fun setEntitled(entitled: Boolean) {
        _isPremium.value = entitled
    }
}
