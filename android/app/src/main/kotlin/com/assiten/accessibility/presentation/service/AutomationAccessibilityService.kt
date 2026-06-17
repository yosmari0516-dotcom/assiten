package com.assiten.accessibility.presentation.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.assiten.accessibility.data.remote.AuthorizationResponse
import com.assiten.accessibility.data.repository.AuthorizationRepositoryImpl
import com.assiten.accessibility.domain.repository.AuthorizationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

/**
 * AutomationAccessibilityService: Servicio de accesibilidad principal
 * Responsabilidades:
 * - Interceptar eventos de AccessibilityEvent
 * - Identificar elementos UI
 * - Consultar backend antes de automatizar
 * - Detener automáticamente si está bloqueado
 */
class AutomationAccessibilityService : AccessibilityService() {

    private lateinit var authRepository: AuthorizationRepository
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var telefono_id: String = ""
    private var isServiceBlocked = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("AutomationAccessibilityService created")
        authRepository = AuthorizationRepositoryImpl()
        telefono_id = getTelefonoId()
        setupServiceInfo()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Si el servicio está bloqueado, ignorar eventos
        if (isServiceBlocked) {
            Timber.w("Service is blocked, ignoring event")
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowChange(event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChanged(event)
            }
        }
    }

    /**
     * Maneja cambios de ventana (pantalla)
     */
    private fun handleWindowChange(event: AccessibilityEvent) {
        Timber.d("Window changed: ${event.source?.className}")
        val source = event.source ?: return
        source.recycle()
    }

    /**
     * Intercepta clics y valida autorización antes de ejecutar
     */
    private fun handleViewClicked(event: AccessibilityEvent) {
        val source = event.source ?: return
        val nodeInfo = AccessibilityNodeInfoCompat.wrap(source)
        val elementoId = nodeInfo.viewIdResourceName ?: UUID.randomUUID().toString()

        Timber.d("View clicked: $elementoId")

        // Valida autorización de forma asincrónica
        validateAndExecuteAction("click", elementoId)

        nodeInfo.recycle()
    }

    /**
     * Intercepta cambios de texto
     */
    private fun handleTextChanged(event: AccessibilityEvent) {
        val source = event.source ?: return
        val elementoId = source.viewIdResourceName ?: UUID.randomUUID().toString()
        val text = event.text.joinToString()

        Timber.d("Text changed in: $elementoId, text: $text")

        validateAndExecuteAction("text_input", elementoId)

        source.recycle()
    }

    /**
     * Valida la acción con el backend antes de proceder
     * Si recibe "bloqueado", detiene el servicio inmediatamente
     */
    private fun validateAndExecuteAction(accion: String, elementoId: String) {
        serviceScope.launch {
            val result = authRepository.checkAction(
                telefono_id = telefono_id,
                accion = accion,
                elemento_id = elementoId
            )

            result.onSuccess { response ->
                when (response.estado) {
                    "activo" -> {
                        if (response.authorized) {
                            Timber.d("Action authorized: $accion")
                            executeAction(accion, elementoId)
                        } else {
                            Timber.w("Action denied: $accion - ${response.mensaje}")
                        }
                    }
                    "bloqueado" -> {
                        Timber.e("Service blocked by backend: ${response.mensaje}")
                        blockService(response.mensaje ?: "Blocked by server")
                    }
                    "suspendido" -> {
                        Timber.w("Service suspended: ${response.mensaje}")
                        disableSelf()
                    }
                }
            }

            result.onFailure { error ->
                Timber.e(error, "Authorization check failed for action: $accion")
                // En caso de error de conexión, NO ejecutar la acción (fail-safe)
            }
        }
    }

    /**
     * Ejecuta la acción automatizada después de validación
     */
    private fun executeAction(accion: String, elementoId: String) {
        when (accion) {
            "click" -> performClick(elementoId)
            "text_input" -> Timber.d("Text input action for $elementoId")
            else -> Timber.w("Unknown action: $accion")
        }
    }

    /**
     * Realiza un clic en el elemento especificado
     */
    private fun performClick(elementoId: String) {
        try {
            val rootNode = rootInActiveWindow ?: return
            val targetNode = findNodeById(rootNode, elementoId)
            if (targetNode?.isClickable == true) {
                val clicked = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Timber.d("Click performed: $clicked on $elementoId")
            } else {
                Timber.w("Target node not found or not clickable: $elementoId")
            }
            rootNode.recycle()
        } catch (e: Exception) {
            Timber.e(e, "Error performing click on $elementoId")
        }
    }

    /**
     * Busca un nodo por su ID de recurso
     */
    private fun findNodeById(
        root: AccessibilityNodeInfo,
        elementoId: String
    ): AccessibilityNodeInfo? {
        if (root.viewIdResourceName == elementoId) {
            return root
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeById(child, elementoId)
            if (found != null) return found
        }
        return null
    }

    /**
     * Bloquea el servicio (respuesta "bloqueado" del backend)
     */
    private fun blockService(reason: String) {
        isServiceBlocked = true
        Timber.e("Service blocked: $reason")
        // Opcionalmente: mostrar notificación al usuario
        disableSelf()
    }

    /**
     * Obtiene el ID único del teléfono (telefono_id)
     * Puede venir de SharedPreferences, Device ID, o serial
     */
    private fun getTelefonoId(): String {
        val sharedPref = getSharedPreferences("assiten", Context.MODE_PRIVATE)
        return sharedPref.getString("telefono_id", null) ?: run {
            val newId = UUID.randomUUID().toString()
            sharedPref.edit().putString("telefono_id", newId).apply()
            newId
        }
    }

    /**
     * Configura la información del servicio de accesibilidad
     */
    private fun setupServiceInfo() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        serviceInfo = info
    }

    override fun onInterrupt() {
        Timber.d("AutomationAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("AutomationAccessibilityService destroyed")
    }
}
