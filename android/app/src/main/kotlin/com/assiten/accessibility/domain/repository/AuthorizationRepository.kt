package com.assiten.accessibility.domain.repository

import com.assiten.accessibility.data.remote.AuthorizationRequest
import com.assiten.accessibility.data.remote.AuthorizationResponse

/**
 * AuthorizationRepository: Abstracción para validar acciones
 * Implementa patrón Repository para desacoplar la lógica de negocio de la fuente de datos
 */
interface AuthorizationRepository {
    /**
     * Verifica si una acción está autorizada
     * @param telefono_id ID del dispositivo
     * @param accion Tipo de acción a realizar
     * @param elemento_id ID del elemento en pantalla (opcional)
     * @return Result con AuthorizationResponse o error
     */
    suspend fun checkAction(
        telefono_id: String,
        accion: String,
        elemento_id: String? = null
    ): Result<AuthorizationResponse>
}
