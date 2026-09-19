package com.wolfden.app.data.remote

import android.content.Context

/**
 * Local access-token storage.
 *
 * The token is isolated behind a small interface so it can later be replaced
 * with encrypted storage without changing the repository or UI layers.
 */
interface AccessTokenStore {
    fun get(): String?
    fun save(token: String)
    fun clear()
}

class SharedPreferencesAccessTokenStore(
    context: Context
) : AccessTokenStore {
    private val preferences =
        context.getSharedPreferences("wolf_den_auth", Context.MODE_PRIVATE)

    override fun get(): String? =
        preferences.getString(KEY_ACCESS_TOKEN, null)

    override fun save(token: String) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    override fun clear() {
        preferences.edit().remove(KEY_ACCESS_TOKEN).apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
    }
}
