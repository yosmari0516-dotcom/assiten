<?php
/**
 * API Endpoint: POST /api/v1/authorize
 * 
 * Valida si un dispositivo (telefono_id) está autorizado para realizar una acción
 * 
 * Arquitectura:
 * - Valida API Key
 * - Consulta base de datos MariaDB (tabla conductores)
 * - Retorna estado de autorización inmediatamente
 * - Implementa rate limiting
 */

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');

// Configuración de seguridad
define('API_KEY_VALID', getenv('API_KEY') ?: 'your-secure-api-key');
define('DB_HOST', getenv('DB_HOST') ?: 'localhost');
define('DB_USER', getenv('DB_USER') ?: 'root');
define('DB_PASS', getenv('DB_PASS') ?: '');
define('DB_NAME', getenv('DB_NAME') ?: 'assiten');
define('MAX_REQUESTS_PER_MINUTE', 60);

// Log para debugging
define('LOG_FILE', '/var/log/assiten/authorization.log');

/**
 * Respuestas HTTP
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
        return false;
    }
    return true;
}

/**
 * Rate limiting: máximo de requests por IP y minuto
 */
function checkRateLimit($telefono_id) {
    $redisKey = "ratelimit:auth:{$telefono_id}";
    $redis = new Redis();
    
    try {
        $redis->connect('127.0.0.1', 6379);
        $current = $redis->incr($redisKey);
        
        if ($current === 1) {
            $redis->expire($redisKey, 60);
        }
        
        $redis->close();
        return $current <= MAX_REQUESTS_PER_MINUTE;
    } catch (Exception $e) {
        error_log("Redis error: " . $e->getMessage());
        // Fallback: permitir si Redis no está disponible
        return true;
    }
}

/**
 * Conecta a la base de datos MariaDB
 */
function getDbConnection() {
    try {
        $conn = new mysqli(
            DB_HOST,
            DB_USER,
            DB_PASS,
            DB_NAME
        );

        if ($conn->connect_error) {
            throw new Exception("Connection failed: " . $conn->connect_error);
        }

        $conn->set_charset("utf8mb4");
        return $conn;
    } catch (Exception $e) {
        error_log("Database connection error: " . $e->getMessage());
        sendResponse([
            'authorized' => false,
            'estado' => 'error',
            'mensaje' => 'Database unavailable'
        ], 503);
    }
}

/**
 * Obtiene el estado del conductor desde la base de datos
 */
function getConductorStatus($conn, $telefono_id) {
    $query = "SELECT telefono_id, estado FROM conductores WHERE telefono_id = ? LIMIT 1";
    
    $stmt = $conn->prepare($query);
    if (!$stmt) {
        throw new Exception("Prepare failed: " . $conn->error);
    }

    $stmt->bind_param("s", $telefono_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    $stmt->close();

    return $row;
}

/**
 * Registra acciones en audit log para debugging
 */
function logAction($telefono_id, $accion, $authorized, $estado) {
    $log_message = sprintf(
        "[%s] telefono_id=%s accion=%s authorized=%s estado=%s\n",
        date('Y-m-d H:i:s'),
        $telefono_id,
        $accion,
        $authorized ? 'true' : 'false',
        $estado
    );
    
    error_log($log_message, 3, LOG_FILE);
}

/**
 * ============================================
 * MAIN LOGIC
 * ============================================
 */

// Valida método HTTP
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse([
        'error' => 'Method not allowed'
    ], 405);
}

// Valida API Key desde headers
$apiKey = $_SERVER['HTTP_X_API_KEY'] ?? '';
if (!validateApiKey($apiKey)) {
    error_log("Invalid API Key attempt");
    sendResponse([
        'authorized' => false,
        'estado' => 'unauthorized',
        'mensaje' => 'Invalid API Key'
    ], 401);
}

// Parse JSON body
$rawInput = file_get_contents('php://input');
$input = json_decode($rawInput, true);

if (json_last_error() !== JSON_ERROR_NONE) {
    sendResponse([
        'error' => 'Invalid JSON'
    ], 400);
}

// Valida parámetros requeridos
$telefono_id = $input['telefono_id'] ?? '';
$accion = $input['accion'] ?? '';
$elemento_id = $input['elemento_id'] ?? null;

if (empty($telefono_id) || empty($accion)) {
    sendResponse([
        'error' => 'Missing required parameters: telefono_id, accion'
    ], 400);
}

// Rate limiting
if (!checkRateLimit($telefono_id)) {
    error_log("Rate limit exceeded for telefono_id: $telefono_id");
    sendResponse([
        'authorized' => false,
        'estado' => 'rate_limited',
        'mensaje' => 'Too many requests'
    ], 429);
}

try {
    // Conecta a la base de datos
    $conn = getDbConnection();

    // Obtiene estado del conductor
    $conductor = getConductorStatus($conn, $telefono_id);

    if (!$conductor) {
        error_log("Conductor not found: $telefono_id");
        sendResponse([
            'authorized' => false,
            'estado' => 'no_encontrado',
            'mensaje' => 'Conductor not registered'
        ], 404);
    }

    $estado = $conductor['estado'];
    $authorized = false;
    $mensaje = null;

    // Lógica de autorización
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
    logAction($telefono_id, $accion, $authorized, $estado);

    // Cierra la conexión
    $conn->close();

    // Retorna respuesta
    sendResponse([
        'authorized' => $authorized,
        'estado' => $estado,
        'mensaje' => $mensaje,
        'timestamp' => time() * 1000 // milliseconds
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
