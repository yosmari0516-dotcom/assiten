package com.assiten.accessibility.presentation.helper

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import timber.log.Timber
import java.util.LinkedList
import java.util.Queue

/**
 * AccessibilityServiceHelper: Utilidades para el AccessibilityService
 *
 * RESPONSABILIDADES:
 * - Búsqueda eficiente de elementos en el árbol de accesibilidad
 * - Caché de nodos frecuentemente accedidos
 * - Validación de elementos clickeables
 * - Extracción de información de pantalla
 */
class AccessibilityServiceHelper(private val context: Context) {

    companion object {
        private const val TAG = "AccessibilityServiceHelper"
        private const val MAX_CACHE_SIZE = 100
    }

    // Caché LRU simple para nodos accedidos recientemente
    private val nodeCache = LinkedHashMap<String, AccessibilityNodeInfo>(16, 0.75f, true) {
        if (size > MAX_CACHE_SIZE) {
            val iterator = entries.iterator()
            iterator.next()
            iterator.remove()
        }
    }

    /**
     * Busca un nodo por su ID de recurso de forma BFS (Breadth-First Search)
     * BFS es más eficiente que DFS para encontrar elementos en niveles superficiales
     *
     * @param root Nodo raíz del árbol de accesibilidad
     * @param elementoId ID del elemento a buscar
     * @return El nodo encontrado o null
     */
    fun findNodeById(
        root: AccessibilityNodeInfo?,
        elementoId: String
    ): AccessibilityNodeInfo? {
        if (root == null) return null

        // Verifica caché primero
        val cached = nodeCache[elementoId]
        if (cached != null && cached.isVisibleToUser) {
            Timber.tag(TAG).d("Found node in cache: $elementoId")
            return cached
        }

        val queue: Queue<AccessibilityNodeInfo> = LinkedList()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: continue

            try {
                if (current.viewIdResourceName == elementoId) {
                    Timber.tag(TAG).d("Found node by ID: $elementoId")
                    nodeCache[elementoId] = current
                    return current
                }

                // Agrega todos los hijos a la cola
                for (i in 0 until current.childCount) {
                    val child = current.getChild(i)
                    if (child != null) {
                        queue.add(child)
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Error during BFS traversal")
            }
        }

        Timber.tag(TAG).w("Node not found: $elementoId")
        return null
    }

    /**
     * Busca nodos por texto (parcial o completo)
     *
     * @param root Nodo raíz
     * @param text Texto a buscar
     * @param exactMatch true para coincidencia exacta, false para parcial
     * @return Lista de nodos que contienen el texto
     */
    fun findNodesByText(
        root: AccessibilityNodeInfo?,
        text: String,
        exactMatch: Boolean = false
    ): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()

        val results = mutableListOf<AccessibilityNodeInfo>()
        val queue: Queue<AccessibilityNodeInfo> = LinkedList()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: continue

            try {
                val nodeText = current.text?.toString() ?: ""

                val matches = if (exactMatch) {
                    nodeText.equals(text, ignoreCase = true)
                } else {
                    nodeText.contains(text, ignoreCase = true)
                }

                if (matches && current.isClickable) {
                    results.add(current)
                    Timber.tag(TAG).d("Found clickable node with text: $text")
                }

                // Agrega hijos a la cola
                for (i in 0 until current.childCount) {
                    val child = current.getChild(i)
                    if (child != null) {
                        queue.add(child)
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Error searching by text")
            }
        }

        return results
    }

    /**
     * Valida si un nodo es seguro para clickear
     * Verifica múltiples condiciones de seguridad
     */
    fun isNodeSafeToClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        try {
            return node.isClickable &&
                    node.isVisibleToUser &&
                    node.isEnabled &&
                    !node.isPassword  // Evita clics en campos de contraseña
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error validating node safety")
            return false
        }
    }

    /**
     * Obtiene toda la información visible en pantalla para debugging
     */
    fun captureScreenInfo(root: AccessibilityNodeInfo?): String {
        if (root == null) return "No root node"

        val sb = StringBuilder()
        captureScreenInfoRecursive(root, 0, sb)
        return sb.toString()
    }

    /**
     * Captura información de pantalla de forma recursiva
     */
    private fun captureScreenInfoRecursive(
        node: AccessibilityNodeInfo?,
        depth: Int,
        sb: StringBuilder
    ) {
        if (node == null || depth > 20) return  // Límite de profundidad

        try {
            val indent = "  ".repeat(depth)
            sb.append("$indent${node.className}")
            
            if (node.text.isNotEmpty()) {
                sb.append(" - Text: ${node.text}")
            }
            
            if (node.isClickable) {
                sb.append(" [CLICKABLE]")
            }
            
            sb.append("\n")

            // Procesa hijos
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    captureScreenInfoRecursive(child, depth + 1, sb)
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error capturing screen info")
        }
    }

    /**
     * Limpia el caché de nodos
     * Evita memory leaks
     */
    fun clearNodeCache() {
        try {
            nodeCache.clear()
            Timber.tag(TAG).d("Node cache cleared")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error clearing node cache")
        }
    }

    /**
     * Obtiene estadísticas del caché
     */
    fun getCacheStats(): String {
        return "Cache size: ${nodeCache.size}, max size: $MAX_CACHE_SIZE"
    }
}
