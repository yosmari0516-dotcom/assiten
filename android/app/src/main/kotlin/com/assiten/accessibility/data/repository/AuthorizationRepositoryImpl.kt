package com.assiten.accessibility.data.repository

import com.assiten.accessibility.data.remote.ApiClient
import com.assiten.accessibility.data.remote.AuthorizationRequest
import com.assiten.accessibility.data.remote.AuthorizationResponse
import com.assiten.accessibility.domain.repository.AuthorizationRepository
import timber.log.Timber

/**
 * AuthorizationRepositoryImpl: Actualizado con seguridad mejorada
 */
class AuthorizationRepositoryImpl(
    private val apiClient: ApiClient = ApiClient
) : AuthorizationRepository {

    override suspend fun checkAction(
        telefono_id: String,
        authToken: String,
        accion: String,
        elemento_id: String?
    ): Result<AuthorizationResponse> = try {
        val request = AuthorizationRequest(
            telefono_id = telefono_id,  // Ya contiene firma
            accion = accion,
            elemento_id = elemento_id
        )

        // ✨ Incluye authToken en el header
        val response = apiClient.retrofitService.checkAuthorization(
            request = request,
            apiKey = BuildConfig.API_KEY,
            authToken = authToken
        )

        if (response.isSuccessful) {
            response.body()?.let {
                Timber.d("Authorization check passed for action: $accion, estado: ${it.estado}")
                Result.success(it)
            } ?: run {
                Timber.e("Empty response body from authorization endpoint")
                Result.failure(Exception("Empty response from server"))
            }
        } else {
            Timber.e("Authorization failed: ${response.code()} - ${response.message()}")
            Result.failure(Exception("Authorization failed: ${response.message()}"))
        }
    } catch (e: Exception) {
        Timber.e(e, "Error checking authorization for action: $accion")
        Result.failure(e)
    }
}
