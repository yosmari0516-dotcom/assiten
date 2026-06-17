package com.assiten.accessibility.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

/**
 * AuthorizationApiService: Actualizado con seguridad mejorada
 * Incluye:
 * - deviceId firmado (deviceId:signature)
 * - Token de autenticación rotativo
 * - Validación en servidor
 */
interface AuthorizationApiService {

    /**
     * Valida si un dispositivo está autorizado para realizar una acción
     * Ahora con seguridad mejorada
     */
    @POST("/api/v1/authorize")
    suspend fun checkAuthorization(
        @Body request: AuthorizationRequest,
        @Header("X-API-Key") apiKey: String,
        @Header("X-Auth-Token") authToken: String
    ): Response<AuthorizationResponse>
}

/**
 * AuthorizationRequest: Actualizado con deviceId firmado
 */
data class AuthorizationRequest(
    val telefono_id: String,  // Ahora: "deviceId:signature"
    val accion: String,
    val elemento_id: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AuthorizationResponse: Respuesta del backend
 */
data class AuthorizationResponse(
    val authorized: Boolean,
    val estado: String,  // "activo", "bloqueado", "suspendido"
    val mensaje: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
