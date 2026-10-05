package com.claudianapolitano.leyla.feature.together

import com.claudianapolitano.leyla.core.ApiClient
import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable

/**
 * Every Games-tab endpoint, one method each. Ports of `QuizAPI.swift`,
 * `DebateAPI.swift`, `DrawAPI.swift` and `SnapAPI.swift`.
 */
object GamesApi {

    // MARK: - Quiz

    suspend fun quizCategories(): List<QuizCategorySummary> =
        ApiClient.get<QuizCategoriesResponse>("/v1/quiz/categories").categories

    suspend fun quizCategory(id: String): QuizCategoryDetail =
        ApiClient.get("/v1/quiz/categories/$id")

    suspend fun quiz(id: String): QuizDetail = ApiClient.get("/v1/quiz/$id")

    suspend fun answerQuiz(quizId: String, questionId: String, answer: String) {
        ApiClient.sendVoid(
            "/v1/quiz/$quizId/answer",
            HttpMethod.Post,
            AnswerBody(questionId = questionId, answer = answer),
        )
    }

    suspend fun dailyQuiz(): QuizDaily = ApiClient.get("/v1/quiz/daily")

    suspend fun answerDailyQuiz(answer: String) {
        ApiClient.sendVoid("/v1/quiz/daily/answer", HttpMethod.Post, AnswerBody(answer = answer))
    }

    // MARK: - How Well Do You Know Me

    suspend fun hwdykmPacks(): List<HwdykmPackSummary> =
        ApiClient.get<HwdykmPacksResponse>("/v1/games/hwdykm/packs").packs

    suspend fun hwdykmPack(id: String): HwdykmPackDetail =
        ApiClient.get("/v1/games/hwdykm/packs/$id")

    suspend fun answerHwdykm(packId: String, questionId: String, answer: String) {
        ApiClient.sendVoid(
            "/v1/games/hwdykm/packs/$packId/answer",
            HttpMethod.Post,
            AnswerBody(questionId = questionId, answer = answer),
        )
    }

    /**
     * Tells the server we've opened the reveal screen for this pack. When both
     * partners have marked the current round as seen, the pack rotates
     * server-side and the next fetch returns brand-new questions.
     */
    suspend fun markHwdykmPackSeen(packId: String) {
        ApiClient.sendVoid("/v1/games/hwdykm/packs/$packId/seen", HttpMethod.Post)
    }

    // MARK: - Couples Debate

    suspend fun debatePacks(): List<DebatePackSummary> =
        ApiClient.get<DebatePacksResponse>("/v1/games/debate/packs").packs

    suspend fun debatePack(id: String): DebatePackDetail =
        ApiClient.get("/v1/games/debate/packs/$id")

    suspend fun argueDebate(packId: String, roundId: String, argument: String) {
        ApiClient.sendVoid(
            "/v1/games/debate/packs/$packId/argue",
            HttpMethod.Post,
            ArgueBody(roundId = roundId, argument = argument),
        )
    }

    /**
     * Tells the server we've opened the results screen for this pack. Once both
     * partners have called this on the same round, the pack rotates on the
     * server — the next fetch returns brand-new motions.
     */
    suspend fun markDebatePackSeen(packId: String) {
        ApiClient.sendVoid("/v1/games/debate/packs/$packId/seen", HttpMethod.Post)
    }

    // MARK: - Draw Together

    suspend fun draw(): DrawRound = ApiClient.get("/v1/games/draw")

    suspend fun submitDraw(jpeg: ByteArray, roundId: String): DrawRound =
        ApiClient.json.decodeFromString(
            ApiClient.uploadImage(
                "/v1/games/draw/submit",
                jpeg,
                filename = "drawing.jpg",
                query = mapOf("roundId" to roundId),
            ),
        )

    /**
     * `force` abandons an in-progress round even if the partner hasn't
     * submitted yet. Without it the server returns 409 `draw_waiting`.
     */
    suspend fun newDrawRound(force: Boolean = false): DrawRound = ApiClient.post(
        "/v1/games/draw/new",
        query = if (force) mapOf("force" to "1") else emptyMap(),
    )

    // MARK: - Snap Hunt

    suspend fun snap(): SnapRound = ApiClient.get("/v1/games/snap")

    suspend fun submitSnap(jpeg: ByteArray, roundId: String): SnapRound =
        ApiClient.json.decodeFromString(
            ApiClient.uploadImage(
                "/v1/games/snap/submit",
                jpeg,
                filename = "snap.jpg",
                query = mapOf("roundId" to roundId),
            ),
        )

    /**
     * `force` abandons an in-progress hunt even if the partner hasn't snapped
     * yet. Without it the server returns 409 `snap_waiting`.
     */
    suspend fun newSnap(force: Boolean = false): SnapRound = ApiClient.post(
        "/v1/games/snap/new",
        query = if (force) mapOf("force" to "1") else emptyMap(),
    )
}

@Serializable
private data class AnswerBody(val questionId: String? = null, val answer: String)

@Serializable
private data class ArgueBody(val roundId: String, val argument: String)
