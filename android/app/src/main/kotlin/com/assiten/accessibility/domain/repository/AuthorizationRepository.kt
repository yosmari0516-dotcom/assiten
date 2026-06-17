package com.assiten.accessibility.domain.repository

import com.assiten.accessibility.data.remote.AuthorizationResponse

/**
 * AuthorizationRepository: Interfaz actualizada
 * Ahora incluye authToken en la validación
 */
interface AuthorizationRepository {
    suspend fun checkAction(
        telefono_id: String,
        authToken: String,  // ✨ NUEVO: Token rotativo
        accion: String,
        elemento_id: String? = null
    ): Result<AuthorizationResponse>
}
