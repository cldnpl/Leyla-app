import Foundation

/// High-level API methods mapping to the Leyla backend endpoints.
extension APIClient {
    func register(email: String, password: String, displayName: String) async throws -> AuthResponse {
        try await send("/v1/auth/register", method: "POST",
                       body: ["email": email, "password": password, "displayName": displayName],
                       authorized: false)
    }

    func login(email: String, password: String) async throws -> AuthResponse {
        try await send("/v1/auth/login", method: "POST",
                       body: ["email": email, "password": password], authorized: false)
    }

    func appleSignIn(identityToken: String, displayName: String) async throws -> AuthResponse {
        try await send("/v1/auth/apple", method: "POST",
                       body: ["identityToken": identityToken, "displayName": displayName], authorized: false)
    }

    func logout() async throws {
        guard let rt = TokenStore.refreshToken else { return }
        try await sendVoid("/v1/auth/logout", method: "POST", body: ["refreshToken": rt], authorized: false)
    }

    func me() async throws -> User {
        try await send("/v1/me")
    }

    func updateProfile(displayName: String) async throws -> User {
        try await send("/v1/me", method: "PATCH", body: ["displayName": displayName])
    }

    /// Persist how this user refers to their partner (she | he | they).
    @discardableResult
    func updatePartnerPronoun(_ pronoun: String) async throws -> User {
        try await send("/v1/me", method: "PATCH", body: ["partnerPronoun": pronoun])
    }

    // MARK: - Avatar

    /// Uploads a new profile photo. The server saves it, sets `avatarPath` on
    /// the account, and returns the refreshed user.
    @discardableResult
    func uploadAvatar(_ jpeg: Data) async throws -> User {
        let data = try await uploadImage("/v1/me/avatar", imageData: jpeg,
                                         filename: "avatar.jpg", caption: nil)
        return try decoder.decode(User.self, from: data)
    }

    /// Clears the profile photo, reverting to the heart fallback.
    @discardableResult
    func deleteAvatar() async throws -> User {
        try await send("/v1/me/avatar", method: "DELETE")
    }

    // MARK: - Changing the account email

    /// Starts an email change: the server mails a one-time code to `newEmail`.
    /// Nothing changes on the account until `confirmEmailChange` succeeds.
    func requestEmailChange(newEmail: String) async throws -> EmailChangeRequestResponse {
        try await send("/v1/me/email/request", method: "POST", body: ["newEmail": newEmail])
    }

    /// Completes an email change with the code from the message.
    func confirmEmailChange(code: String) async throws -> User {
        try await send("/v1/me/email/confirm", method: "POST", body: ["code": code])
    }

    func getCouple() async throws -> CoupleResponse {
        try await send("/v1/couple")
    }

    func createPairingCode() async throws -> PairingCode {
        try await send("/v1/pairing/code", method: "POST")
    }

    func redeemPairing(code: String) async throws -> CoupleResponse {
        try await send("/v1/pairing/redeem", method: "POST", body: ["code": code])
    }

    func setStartDate(_ isoDate: String) async throws -> Couple {
        try await send("/v1/couple", method: "PATCH", body: ["startDate": isoDate])
    }

    func unpair() async throws {
        try await sendVoid("/v1/couple", method: "DELETE")
    }

    func sendMissYou() async throws -> MissYouEvent {
        try await send("/v1/miss-you", method: "POST")
    }

    func listMissYou() async throws -> MissYouList {
        try await send("/v1/miss-you")
    }

    func registerDevice(apnsToken: String, environment: String) async throws {
        try await sendVoid("/v1/devices", method: "POST",
                           body: ["apnsToken": apnsToken, "environment": environment])
    }

    /// Unregisters this install's token from the signed-out account, so the
    /// phone stops receiving that account's notifications.
    func deleteDevice(apnsToken: String) async throws {
        try await sendVoid("/v1/devices", method: "DELETE", body: ["apnsToken": apnsToken])
    }

    // MARK: - Gallery

    func listMedia() async throws -> MediaList {
        try await send("/v1/media")
    }

    func uploadPhoto(_ jpeg: Data, caption: String?) async throws -> MediaItem {
        let data = try await uploadImage("/v1/media", imageData: jpeg, filename: "photo.jpg", caption: caption)
        return try decoder.decode(MediaItem.self, from: data)
    }

    func deletePhoto(id: String) async throws {
        try await sendVoid("/v1/media/\(id)", method: "DELETE")
    }

    /// Loads image bytes for a relative media path (thumb or full).
    func imageData(relativePath: String) async throws -> Data {
        try await fetchData(relativePath)
    }

    // MARK: - Moments

    func listMilestones() async throws -> [Milestone] {
        let list: MilestoneList = try await send("/v1/milestones")
        return list.milestones
    }

    func createMilestone(title: String, date: String, kind: String) async throws -> Milestone {
        try await send("/v1/milestones", method: "POST",
                       body: ["title": title, "date": date, "kind": kind])
    }

    @discardableResult
    func updateMilestone(id: String, title: String, date: String) async throws -> Milestone {
        try await send("/v1/milestones/\(id)", method: "PATCH", body: ["title": title, "date": date])
    }

    func deleteMilestone(id: String) async throws {
        try await sendVoid("/v1/milestones/\(id)", method: "DELETE")
    }

    /// Permanently deletes the signed-in account (App Store 5.1.1).
    func deleteAccount() async throws {
        try await sendVoid("/v1/me", method: "DELETE")
    }

    // MARK: - Journal

    func listJournal() async throws -> [JournalEntry] {
        let list: JournalList = try await send("/v1/journal")
        return list.entries
    }

    @discardableResult
    func createJournalEntry(date: String, body: String) async throws -> JournalEntry {
        try await send("/v1/journal", method: "POST", body: ["date": date, "body": body])
    }

    @discardableResult
    func updateJournalEntry(id: String, body: String) async throws -> JournalEntry {
        try await send("/v1/journal/\(id)", method: "PUT", body: ["body": body])
    }

    @discardableResult
    func uploadJournalPhoto(entryId: String, _ jpeg: Data) async throws -> MediaItem {
        let data = try await uploadImage("/v1/journal/\(entryId)/photos", imageData: jpeg, filename: "photo.jpg", caption: nil)
        return try decoder.decode(MediaItem.self, from: data)
    }

    func deleteJournalEntry(id: String) async throws {
        try await sendVoid("/v1/journal/\(id)", method: "DELETE")
    }

    func listReunions() async throws -> [Reunion] {
        let list: ReunionList = try await send("/v1/reunions")
        return list.reunions
    }

    func createReunion(title: String, targetDate: String) async throws -> Reunion {
        try await send("/v1/reunions", method: "POST",
                       body: ["title": title, "targetDate": targetDate])
    }

    func deleteReunion(id: String) async throws {
        try await sendVoid("/v1/reunions/\(id)", method: "DELETE")
    }

    // MARK: - Partner location

    func updateLocation(lat: Double, lng: Double, accuracy: Double?, mode: String) async throws {
        struct Body: Encodable { let lat: Double; let lng: Double; let accuracy: Double?; let mode: String }
        try await sendVoid("/v1/location", method: "PUT",
                           body: Body(lat: lat, lng: lng, accuracy: accuracy, mode: mode))
    }

    func stopSharingLocation() async throws {
        try await sendVoid("/v1/location", method: "PUT", body: ["mode": "off"])
    }

    func partnerLocation() async throws -> PartnerLocation {
        try await send("/v1/location")
    }
}
