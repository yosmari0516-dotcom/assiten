<?php
/**
 * API Endpoint: POST /api/v1/authorize
 * VERSIÓN MEJORADA CON SEGURIDAD
 * 
 * Mejoras implementadas:
 * 1. ✅ Validación de deviceId firmado (HMAC-SHA256)
 * 2. ✅ Token de autenticación rotativo
 * 3. ✅ Detección de dispositivos falsificados
 * 4. ✅ Registro de intentos de fraude
 */

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');

// Configuración
define('API_KEY_VALID', getenv('API_KEY') ?: 'your-secure-api-key');
define('DB_HOST', getenv('DB_HOST') ?: 'localhost');
define('DB_USER', getenv('DB_USER') ?: 'root');
define('DB_PASS', getenv('DB_PASS') ?: '');
define('DB_NAME', getenv('DB_NAME') ?: 'assiten');
define('MAX_REQUESTS_PER_MINUTE', 60);
define('HMAC_ALGORITHM', 'sha256');
define('LOG_FILE', '/var/log/assiten/authorization.log');
define('FRAUD_LOG_FILE', '/var/log/assiten/fraud_attempts.log');

/**
 * Envía respuesta JSON
 */
function sendResponse($data, $httpCode = 200) {
    http_response_code($httpCode);
    echo json_encode($data, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

/**
 * Valida el API Key
 */
function validateApiKey($apiKey) {
    if (empty($apiKey) || $apiKey !== API_KEY_VALID) {
        error_log("Invalid API Key attempt: $apiKey", 3, '/var/log/assiten/security.log');
        return false;
    }
    return true;
}

/**
 * ✨ MEJORA #3: Valida la firma del deviceId
 * 
 * La firma se genera en el cliente con:
 * HMAC-SHA256(deviceId, derived_key_from_android_device_id)
 * 
 * En el servidor, validamos que la firma sea correcta
 * Si no coincide, es un intento de fraude
 */
function validateDeviceIdSignature($deviceIdWithSignature) {
    list($deviceId, $providedSignature) = explode(':', $deviceIdWithSignature);
    
    if (empty($deviceId) || empty($providedSignature)) {
        return [
            'valid' => false,
            'fraud' => true,
            'reason' => 'Invalid format'
        ];
    }

    // 🔐 Reconstruye la clave de firma del servidor
    // En producción, esta clave debe obtenerse de la BD
    // asociada al deviceId registrado
    $serverSigningKey = getDeviceSigningKey($deviceId);
    
    if (!$serverSigningKey) {
        return [
            'valid' => false,
            'fraud' => true,
            'reason' => 'Device not registered'
        ];
    }

    // Genera la firma esperada
    $expectedSignature = hash_hmac(HMAC_ALGORITHM, $deviceId, $serverSigningKey);
    
    // Compara de forma segura (timing attack resistant)
    $isValid = hash_equals($expectedSignature, $providedSignature);

    return [
        'valid' => $isValid,
        'fraud' => !$isValid,
        'reason' => $isValid ? 'OK' : 'Signature mismatch'
    ];
}

/**
 * ✨ MEJORA #3: Obtiene la clave de firma del dispositivo
 * Derivada del Android Device ID registrado
 */
function getDeviceSigningKey($deviceId) {
    try {
        $conn = getDbConnection();
        $query = "SELECT signing_key FROM conductores WHERE telefono_id = ? LIMIT 1";
        
        $stmt = $conn->prepare($query);
        $stmt->bind_param("s", $deviceId);
        $stmt->execute();
        $result = $stmt->get_result();
        $row = $result->fetch_assoc();
        $stmt->close();
        $conn->close();

        return $row['signing_key'] ?? null;
    } catch (Exception $e) {
        error_log("Error getting signing key: " . $e->getMessage());
        return null;
    }
}

/**
 * ✨ MEJORA #3: Valida token de autenticación rotativo
 * Los tokens expiran en 24 horas
 */
function validateAuthToken($conn, $deviceId, $providedToken) {
    try {
        $query = "SELECT auth_token, token_expiry FROM conductores WHERE telefono_id = ? LIMIT 1";
        
        $stmt = $conn->prepare($query);
        $stmt->bind_param("s", $deviceId);
        $stmt->execute();
        $result = $stmt->get_result();
        $row = $result->fetch_assoc();
        $stmt->close();

        if (!$row) {
            return [
                'valid' => false,
                'reason' => 'Device not found'
            ];
        }

        $storedToken = $row['auth_token'];
        $tokenExpiry = $row['token_expiry'];

        // Compara tokens de forma segura
        $tokenMatches = hash_equals($storedToken, $providedToken);
        
        // Verifica expiración
        $notExpired = time() < $tokenExpiry;

        return [
            'valid' => $tokenMatches && $notExpired,
            'reason' => $tokenMatches ? ($notExpired ? 'OK' : 'Token expired') : 'Token mismatch'
        ];

    } catch (Exception $e) {
        error_log("Error validating token: " . $e->getMessage());
        return [
            'valid' => false,
            'reason' => 'Validation error'
        ];
    }
}

/**
 * Registra intento de fraude
 */
function logFraudAttempt($deviceId, $reason, $ipAddress = '') {
    $message = sprintf(
        "[%s] FRAUD_ATTEMPT device=%s reason=%s ip=%s\n",
        date('Y-m-d H:i:s'),
        $deviceId,
        $reason,
        $ipAddress ?: $_SERVER['REMOTE_ADDR']
    );
    error_log($message, 3, FRAUD_LOG_FILE);
}

/**
 * Conecta a la base de datos
 */
function getDbConnection() {
    try {
        $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);
        if ($conn->connect_error) {
            throw new Exception("Connection failed: " . $conn->connect_error);
        }
        $conn->set_charset("utf8mb4");
        return $conn;
    } catch (Exception $e) {
        error_log("Database error: " . $e->getMessage());
        sendResponse([
            'authorized' => false,
            'estado' => 'error',
            'mensaje' => 'Database unavailable'
        ], 503);
    }
}

/**
 * Obtiene estado del conductor
 */
function getConductorStatus($conn, $deviceId) {
    $query = "SELECT telefono_id, estado FROM conductores WHERE telefono_id = ? LIMIT 1";
    
    $stmt = $conn->prepare($query);
    $stmt->bind_param("s", $deviceId);
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    $stmt->close();

    return $row;
}

/**
 * Registra la acción
 */
function logAction($deviceId, $accion, $authorized, $estado) {
    $message = sprintf(
        "[%s] device=%s action=%s authorized=%s status=%s\n",
        date('Y-m-d H:i:s'),
        $deviceId,
        $accion,
        $authorized ? 'true' : 'false',
        $estado
    );
    error_log($message, 3, LOG_FILE);
}

/**
 * ============================================
 * MAIN LOGIC
 * ============================================
 */

// Valida método HTTP
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(['error' => 'Method not allowed'], 405);
}

// Valida API Key
$apiKey = $_SERVER['HTTP_X_API_KEY'] ?? '';
if (!validateApiKey($apiKey)) {
    sendResponse([
        'authorized' => false,
        'estado' => 'unauthorized',
        'mensaje' => 'Invalid API Key'
    ], 401);
}

// Obtiene token de autenticación
$authToken = $_SERVER['HTTP_X_AUTH_TOKEN'] ?? '';

// Parse JSON body
$rawInput = file_get_contents('php://input');
$input = json_decode($rawInput, true);

if (json_last_error() !== JSON_ERROR_NONE) {
    sendResponse(['error' => 'Invalid JSON'], 400);
}

// Extrae parámetros
$deviceIdWithSignature = $input['telefono_id'] ?? '';  // Ahora: "deviceId:signature"
$accion = $input['accion'] ?? '';
$elementoId = $input['elemento_id'] ?? null;

if (empty($deviceIdWithSignature) || empty($accion)) {
    sendResponse(['error' => 'Missing required parameters'], 400);
}

// ✨ MEJORA #3: Extrae deviceId sin firma
$deviceIdParts = explode(':', $deviceIdWithSignature);
$deviceId = $deviceIdParts[0];

try {
    // ✨ MEJORA #3: Valida firma del deviceId
    $signatureValidation = validateDeviceIdSignature($deviceIdWithSignature);
    
    if (!$signatureValidation['valid']) {
        logFraudAttempt($deviceId, $signatureValidation['reason']);
        
        sendResponse([
            'authorized' => false,
            'estado' => 'fraude_detectado',
            'mensaje' => 'Device signature mismatch'
        ], 403);
    }

    // ✨ MEJORA #3: Valida token de autenticación
    $conn = getDbConnection();
    $tokenValidation = validateAuthToken($conn, $deviceId, $authToken);
    
    if (!$tokenValidation['valid']) {
        logFraudAttempt($deviceId, 'Invalid token: ' . $tokenValidation['reason']);
        
        $conn->close();
        sendResponse([
            'authorized' => false,
            'estado' => 'token_invalido',
            'mensaje' => 'Authentication token invalid or expired'
        ], 401);
    }

    // Obtiene estado del conductor
    $conductor = getConductorStatus($conn, $deviceId);

    if (!$conductor) {
        error_log("Conductor not found: $deviceId");
        $conn->close();
        sendResponse([
            'authorized' => false,
            'estado' => 'no_encontrado',
            'mensaje' => 'Conductor not registered'
        ], 404);
    }

    $estado = $conductor['estado'];
    $authorized = false;
    $mensaje = null;

    // Lógica de autorizacion
    switch ($estado) {
        case 'activo':
            $authorized = true;
            break;
        case 'bloqueado':
            $authorized = false;
            $mensaje = 'Driver is blocked';
            break;
        case 'suspendido':
            $authorized = false;
            $mensaje = 'Driver account suspended';
            break;
        default:
            $authorized = false;
            $mensaje = 'Unknown status: ' . $estado;
    }

    // Registra la acción
    logAction($deviceId, $accion, $authorized, $estado);

    $conn->close();

    // Retorna respuesta
    sendResponse([
        'authorized' => $authorized,
        'estado' => $estado,
        'mensaje' => $mensaje,
        'timestamp' => time() * 1000
    ], 200);

} catch (Exception $e) {
    error_log("Authorization error: " . $e->getMessage());
    sendResponse([
        'authorized' => false,
        'estado' => 'error',
        'mensaje' => 'Internal server error'
    ], 500);
}
?>
