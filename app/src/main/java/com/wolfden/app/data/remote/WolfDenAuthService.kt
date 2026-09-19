package com.wolfden.app.data.remote

/**
 * Authentication boundary for the member app.
 *
 * The UI can use Demo Mode when no API URL is configured and switch to this
 * service automatically when a production backend is supplied.
 */
interface WolfDenAuthService {
    fun requestOtp(phone: String): Boolean
    fun verifyOtp(phone: String, code: String): AuthResponse
}

class RemoteWolfDenAuthService(
    private val api: WolfDenApi,
    private val tokenStore: AccessTokenStore
) : WolfDenAuthService {

    override fun requestOtp(phone: String): Boolean =
        api.requestOtp(LoginRequest(phone))

    override fun verifyOtp(phone: String, code: String): AuthResponse =
        api.verifyOtp(VerifyOtpRequest(phone, code)).also {
            tokenStore.save(it.accessToken)
        }
}
