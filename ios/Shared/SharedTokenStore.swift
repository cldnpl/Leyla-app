import Foundation

/// Auth tokens mirrored into the App Group so the **widget extension** can call
/// the API on the user's behalf (the interactive "I miss you" button) without
/// launching the app.
///
/// The main app's Keychain remains the source of truth; it mirrors tokens here
/// on every change (see `TokenStore`). The widget reads them and, if the access
/// token has expired, can refresh and write the rotated tokens back.
///
/// The pair is persisted as a **single JSON blob** under one key. It used to be
/// three independent `UserDefaults` writes (access, refresh, timestamp); if the
/// widget extension was suspended between them the App Group could be left with
/// a new access token next to a stale refresh token, and the reconciliation in
/// `TokenStore.syncToSharedStore` — which keys off that timestamp — would then
/// hand the app a refresh token the server had already rotated away, signing the
/// user out. One atomic write removes the torn-state window.
enum SharedTokenStore {
    private static let pairKey = "shared_token_pair_v1"

    // Legacy individual keys. Still written for one release so a widget process
    // still running the previous build during an in-place app update keeps
    // working; reads prefer the atomic blob above and fall back to these.
    private static let accessKey = "shared_access_token"
    private static let refreshKey = "shared_refresh_token"
    private static let updatedAtKey = "shared_auth_tokens_updated_at"

    /// A complete set of credentials. Keeping the access and refresh token
    /// together avoids treating a half-written pair as a usable login when the
    /// app and widget wake at the same time.
    struct TokenPair: Equatable {
        let accessToken: String
        let refreshToken: String
        let updatedAt: Date?
    }

    private struct StoredPair: Codable {
        let access: String
        let refresh: String
        let updatedAt: Double
    }

    static var accessToken: String? {
        get { tokenPair?.accessToken }
        set {
            let current = tokenPair
            setTokens(accessToken: newValue ?? "",
                      refreshToken: current?.refreshToken ?? "",
                      updatedAt: current?.updatedAt ?? Date())
        }
    }

    static var refreshToken: String? {
        get { tokenPair?.refreshToken }
        set {
            let current = tokenPair
            setTokens(accessToken: current?.accessToken ?? "",
                      refreshToken: newValue ?? "",
                      updatedAt: current?.updatedAt ?? Date())
        }
    }

    static var tokenPair: TokenPair? {
        if let data = SharedConfig.defaults?.data(forKey: pairKey),
           let stored = try? JSONDecoder().decode(StoredPair.self, from: data),
           !stored.access.isEmpty, !stored.refresh.isEmpty {
            return TokenPair(accessToken: stored.access,
                             refreshToken: stored.refresh,
                             updatedAt: Date(timeIntervalSince1970: stored.updatedAt))
        }
        // Legacy fallback: pre-blob installs, or an older widget process that
        // only ever wrote the individual keys.
        guard let access = SharedConfig.defaults?.string(forKey: accessKey), !access.isEmpty,
              let refresh = SharedConfig.defaults?.string(forKey: refreshKey), !refresh.isEmpty
        else { return nil }
        let timestamp = SharedConfig.defaults?.object(forKey: updatedAtKey) as? TimeInterval
        return TokenPair(accessToken: access,
                         refreshToken: refresh,
                         updatedAt: timestamp.map(Date.init(timeIntervalSince1970:)))
    }

    /// Store a freshly issued pair as one logical update. App Group defaults
    /// are shared with the widget, so the timestamp also lets the main app
    /// recognize a pair that the widget rotated while it was not running.
    static func setTokens(accessToken: String, refreshToken: String, updatedAt: Date = Date()) {
        guard let defaults = SharedConfig.defaults else { return }
        let stored = StoredPair(access: accessToken,
                                refresh: refreshToken,
                                updatedAt: updatedAt.timeIntervalSince1970)
        if let data = try? JSONEncoder().encode(stored) {
            defaults.set(data, forKey: pairKey)   // one atomic write — no torn state
        }
        // Legacy mirror — one release of belt-and-suspenders for a widget
        // process that has not picked up this build yet.
        defaults.set(accessToken, forKey: accessKey)
        defaults.set(refreshToken, forKey: refreshKey)
        defaults.set(updatedAt.timeIntervalSince1970, forKey: updatedAtKey)
    }

    static func clear() {
        guard let defaults = SharedConfig.defaults else { return }
        defaults.removeObject(forKey: pairKey)
        defaults.removeObject(forKey: accessKey)
        defaults.removeObject(forKey: refreshKey)
        defaults.removeObject(forKey: updatedAtKey)
    }
}
