package com.assiten.accessibility.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

/**
 * AuthorizationApiService: Define endpoints para validar acciones con el backend
 * Cada acción automatizada requiere autorización remota
 */
interface AuthorizationApiService {

    /**
     * Valida si un dispositivo (por telefono_id) está autorizado para realizar una acción
     * @param request Contiene telefono_id, tipo de acción y otros datos contextuales
     * @return AuthorizationResponse con estado de autorización
     */
    @POST("/api/v1/authorize")
    suspend fun checkAuthorization(
        @Body request: AuthorizationRequest,
        @Header("X-API-Key") apiKey: String
    ): Response<AuthorizationResponse>
}

/**
 * AuthorizationRequest: Datos enviados al backend para validar una acción
 */
data class AuthorizationRequest(
    val telefono_id: String,
    val accion: String,  // Ej: "click", "scroll", "input_text"
    val elemento_id: String? = null,  // ID único del elemento en pantalla
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AuthorizationResponse: Respuesta del backend
 * - authorized: true si la acción está permitida
 * - estado: "activo", "bloqueado", etc.
 * - mensaje: Razón si está bloqueado
 */
data class AuthorizationResponse(
    val authorized: Boolean,
    val estado: String,  // "activo", "bloqueado", "suspendido"
    val mensaje: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
