package com.assiten.accessibility.data.repository

import com.assiten.accessibility.data.remote.ApiClient
import com.assiten.accessibility.data.remote.AuthorizationRequest
import com.assiten.accessibility.data.remote.AuthorizationResponse
import com.assiten.accessibility.domain.repository.AuthorizationRepository
import timber.log.Timber

/**
 * AuthorizationRepositoryImpl: Implementación concreta del repositorio
 * Maneja:
 * - Llamadas HTTP al backend
 * - Manejo de errores y timeouts
 * - Logging de fallos
 */
class AuthorizationRepositoryImpl(
    private val apiClient: ApiClient
) : AuthorizationRepository {

    override suspend fun checkAction(
        telefono_id: String,
        accion: String,
        elemento_id: String?
    ): Result<AuthorizationResponse> = try {
        val request = AuthorizationRequest(
            telefono_id = telefono_id,
            accion = accion,
            elemento_id = elemento_id
        )

        val response = apiClient.retrofitService.checkAuthorization(
            request = request,
            apiKey = BuildConfig.API_KEY
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
