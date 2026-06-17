# Security & Reliability Improvements

## 🔴 Mejora #1: Validación en Background Thread (Coroutines)

### Problema
```kotlin
// ❌ MAL: Bloquea el UI thread
val result = authRepository.checkAction(...)
if (result.authorized) { executeAction() }
```

### Solución
```kotlin
// ✅ BIEN: Usa Dispatchers.IO para no bloquear UI
private fun validateAndExecuteActionAsync(accion: String, elementoId: String) {
    serviceScope.launch {
        val result = withContext(Dispatchers.IO) {
            validateWithRetry(accion, elementoId)
        }
        // Procesa resultado sin bloquear
    }
}
```

**Beneficios:**
- ✅ No bloquea el hilo principal
- ✅ Timeouts automáticos (10 segundos)
- ✅ Reintentos con backoff exponencial (1s, 2s, 4s)
- ✅ Operaciones de red seguras

---

## 🔴 Mejora #2: Manejo Robusto de Errores

### Problema
```php
// ❌ Si el servidor cae, el servicio sigue intentando
while (true) {
    try {
        validateAction()  // Puede timeout indefinidamente
    } catch (e) {
        // Sin manejo específico
    }
}
```

### Solución

**En Android:**
```kotlin
private fun handleAuthorizationError(error: Exception, accion: String) {
    when (error) {
        is SocketTimeoutException -> blockServiceImmediately("Server timeout")
        is ConnectException -> blockServiceImmediately("Server down")
        is IOException -> blockServiceImmediately("Network error")
        else -> blockServiceImmediately("Unexpected error")
    }
}
```

**Componentes:**
- ✅ `SupervisorJob`: Si una corrutina falla, las otras continúan
- ✅ Health checks cada 30 segundos
- ✅ Detección inmediata de servidor caído
- ✅ Detiene el servicio de forma segura

**En Backend:**
```php
// ✅ Manejo robusto de BD
try {
    $conn = getDbConnection();
    $conductor = getConductorStatus($conn, $deviceId);
    // Validación segura
} catch (Exception $e) {
    logError($e);
    sendResponse(['error' => 'Database unavailable'], 503);
}
```

---

## 🔴 Mejora #3: Seguridad del DeviceId

### Problema
```kotlin
// ❌ DeviceId simple es fácil de falsificar
val deviceId = UUID.randomUUID().toString()
api.checkAuthorization(deviceId)  // Cualquiera puede inventar un ID
```

### Solución Implementada

#### **A) Firma Digital (HMAC-SHA256)**
```kotlin
private fun generateSignature(deviceId: String): String {
    val secretKey = deriveSigningKey()  // De Android Device ID
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secretKey, "HmacSHA256"))
    val signature = mac.doFinal(deviceId.toByteArray())
    return signature.joinToString("") { "%02x".format(it) }
}

// 📤 Se envía: "deviceId:signature"
```

**¿Por qué es seguro?**
- ✅ La clave se deriva del Android Device ID (hardware-specific)
- ✅ Sin acceso a esa clave, es imposible generar firma válida
- ✅ El servidor valida la firma antes de autorizar

#### **B) Token Rotativo**
```kotlin
fun generateAuthToken(): String {
    val newToken = UUID.randomUUID().toString()
    encryptedPrefs.putString("auth_token", newToken)
    encryptedPrefs.putLong("token_expiry", System.currentTimeMillis() + 24h)
    return newToken  // Expira en 24 horas
}
```

**Ventajas:**
- ✅ Token de corta duración (24 horas)
- ✅ Se puede revocar desde el servidor
- ✅ Detecta tokens robados rápidamente

#### **C) Encriptación de Datos Sensibles**
```kotlin
// ✅ Usa EncryptedSharedPreferences (Android Security Library)
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context, PREF_NAME, masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

**Protección:**
- ✅ Datos encriptados con AES-256-GCM
- ✅ Hardware-backed security si está disponible
- ✅ Si el dispositivo es comprometido, los datos sigue encriptados

#### **D) Validación en el Servidor**
```php
// ✅ Valida firma
function validateDeviceIdSignature($deviceIdWithSignature) {
    list($deviceId, $providedSignature) = explode(':', $deviceIdWithSignature);
    $serverSigningKey = getDeviceSigningKey($deviceId);  // De BD
    $expectedSignature = hash_hmac('sha256', $deviceId, $serverSigningKey);
    return hash_equals($expectedSignature, $providedSignature);  // Timing-safe
}

// ✅ Valida token
function validateAuthToken($conn, $deviceId, $providedToken) {
    $storedToken = $conductor['auth_token'];
    $tokenExpiry = $conductor['token_expiry'];
    
    // Compara de forma segura contra timing attacks
    $tokenMatches = hash_equals($storedToken, $providedToken);
    $notExpired = time() < $tokenExpiry;
    
    return $tokenMatches && $notExpired;
}
```

#### **E) Detección de Fraude**
```php
// ✅ Registra intentos de fraude
if (!$signatureValidation['valid']) {
    logFraudAttempt(
        $deviceId,
        'Device signature mismatch',
        $_SERVER['REMOTE_ADDR']
    );
    sendResponse(['estado' => 'fraude_detectado'], 403);
}
```

---

## 📊 Tabla Comparativa

| Aspecto | Antes | Después |
|--------|-------|--------|
| **Validación** | Síncrona (bloquea UI) | Asincronía (background thread) |
| **Error Servidor Caído** | Cuelga indefinidamente | Detiene servicio inmediatamente |
| **DeviceId** | UUID aleatorio (falsificable) | Firmado + token rotativo |
| **Encriptación** | SharedPreferences plano | EncryptedSharedPreferences (AES-256) |
| **Reintentos** | Sin reintentos | 3 reintentos con backoff exponencial |
| **Timeout** | Sin límite | 10 segundos explícito |
| **Detección Fraude** | No | Logging de intentos + IP |
| **Health Check** | No | Cada 30 segundos |

---

## 🚀 Configuración Recomendada

### Android (build.gradle.kts)
```kotlin
implementations(
    "androidx.security:security-crypto:1.1.0-alpha06",  // EncryptedSharedPreferences
    "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
)
```

### Backend (.env)
```bash
# Variables para firma de dispositivos
DEVICE_SIGNING_ENABLED=true
TOKEN_EXPIRY_HOURS=24
MAX_FAILED_AUTH_ATTEMPTS=5
```

### Base de Datos (migrate)
```bash
mysql -u assiten_user -p assiten < migrations/002_add_security_fields.sql
```

---

## 🧪 Testing

```bash
# Test unitarios
./gradlew test

# Prueba con deviceId falsificado (debe fallar)
curl -X POST https://your-domain.com/api/v1/authorize \
  -H "X-API-Key: key" \
  -H "X-Auth-Token: fake-token" \
  -H "Content-Type: application/json" \
  -d '{"telefono_id":"fake-device:invalid-sig","accion":"click"}'
# Response: 403 Forbidden - "fraude_detectado"
```

---

## ✅ Resumen

✔️ **Mejora #1**: Validaciones en background thread (no bloquean UI)
✔️ **Mejora #2**: Manejo robusto de errores (servidor caído = servicio se detiene)
✔️ **Mejora #3**: DeviceId seguro (firma + token + encriptación)
