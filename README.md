# Assiten: AccessibilityService Automation

**Arquitectura para automatizar interacciones en pantalla Android con validación remota de seguridad.**

## 🏗️ Componentes

### 1. Cliente Android (Kotlin)
- **AccessibilityService**: Intercepta eventos de pantalla
- **API Client**: Comunica con backend para validar acciones
- **Authorization Repository**: Patrón Repository para desacoplar lógica

### 2. Backend API (cPanel + PHP)
- **POST /api/v1/authorize**: Valida si una acción está autorizada
- **Rate Limiting**: Máximo 60 requests/minuto por dispositivo
- **Audit Logging**: Registra todas las acciones intentadas

### 3. Base de Datos (MariaDB)
- Tabla `conductores`: Estado de autorización por dispositivo
- Tabla `action_logs`: Auditoría de acciones

## 📋 Flujo de Autorización

```
1. AccessibilityService intercepta evento (click, text input, etc)
   ↓
2. Consulta API: POST /api/v1/authorize
   ├─ telefono_id (UUID del dispositivo)
   ├─ accion (tipo de evento)
   └─ elemento_id (ID del elemento en pantalla)
   ↓
3. Backend busca conductor en MariaDB
   ├─ Si estado = "activo" → autorizado ✅
   ├─ Si estado = "bloqueado" → detiene servicio inmediatamente ⛔
   └─ Si estado = "suspendido" → rechaza sin ejecutar 🚫
   ↓
4. Ejecuta acción solo si está autorizada
```

## 🚀 Instalación

### Android

1. **Clona el repositorio**
   ```bash
   git clone https://github.com/yosmari0516-dotcom/assiten.git
   cd assiten
   ```

2. **Configura BuildConfig.kt**
   ```kotlin
   object BuildConfig {
       const val API_BASE_URL = "https://your-domain.com"
       const val API_KEY = "your-secure-api-key"
       const val DEBUG = true
   }
   ```

3. **Compila la app**
   ```bash
   ./gradlew build
   ```

4. **Instala en dispositivo**
   ```bash
   ./gradlew installDebug
   ```

### Backend (cPanel)

1. **Sube archivos vía FTP/SFTP**
   ```
   /public_html/api/v1/authorize.php
   /public_html/config/database.php
   ```

2. **Configura variables de entorno**
   ```bash
   # En cPanel o .htaccess
   SetEnv DB_HOST localhost
   SetEnv DB_USER assiten_user
   SetEnv DB_PASS secure_password
   SetEnv DB_NAME assiten
   SetEnv API_KEY your-secure-api-key
   ```

3. **Ejecuta migraciones de BD**
   ```bash
   mysql -u assiten_user -p assiten < backend/database/migrations/001_create_conductores_table.sql
   ```

## 🔒 Seguridad

### Cliente Android
- ✅ Timeout en requests HTTP (10 segundos)
- ✅ Fail-safe: Si no hay respuesta, NO ejecuta acción
- ✅ Rate limiting en backend (60 req/min por dispositivo)
- ✅ API Key en BuildConfig (no hardcoded en strings.xml)

### Backend
- ✅ Validación de API Key en cada request
- ✅ Prepared statements contra SQL injection
- ✅ HTTPS obligatorio en producción
- ✅ Audit logging de todas las acciones
- ✅ Respuesta inmediata a "bloqueado" (detiene servicio)

## 📊 Monitoreo

### Logs del Backend
```bash
# Ver logs en tiempo real
tail -f /var/log/assiten/authorization.log

# Buscar acciones bloqueadas
grep "bloqueado" /var/log/assiten/authorization.log
```

### Auditoría en BD
```sql
-- Ver últimas 100 acciones de un dispositivo
SELECT * FROM action_logs 
WHERE telefono_id = 'device-uuid'
ORDER BY created_at DESC
LIMIT 100;

-- Dispositivos bloqueados
SELECT telefono_id, numero_telefono, estado, updated_at
FROM conductores
WHERE estado = 'bloqueado'
ORDER BY updated_at DESC;
```

## 🧪 Testing

### Android
```bash
# Tests unitarios
./gradlew test

# Tests instrumentados
./gradlew connectedAndroidTest
```

### Backend (API)
```bash
# Test autorización exitosa
curl -X POST https://your-domain.com/api/v1/authorize \
  -H "X-API-Key: your-secure-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "telefono_id": "test-device-123",
    "accion": "click",
    "elemento_id": "com.example:id/button"
  }'

# Test dispositivo bloqueado
curl -X POST https://your-domain.com/api/v1/authorize \
  -H "X-API-Key: your-secure-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "telefono_id": "blocked-device",
    "accion": "click"
  }'
```

## 📦 Estructura del Proyecto

```
assiten/
├── android/
│   ├── app/
│   │   ├── src/main/kotlin/com/assiten/accessibility/
│   │   │   ├── data/
│   │   │   │   ├── remote/
│   │   │   │   │   ├── ApiClient.kt
│   │   │   │   │   └── AuthorizationApiService.kt
│   │   │   │   └── repository/
│   │   │   │       └── AuthorizationRepositoryImpl.kt
│   │   │   ├── domain/
│   │   │   │   └── repository/
│   │   │   │       └── AuthorizationRepository.kt
│   │   │   ├── presentation/
│   │   │   │   └── service/
│   │   │   │       └── AutomationAccessibilityService.kt
│   │   │   └── di/
│   │   │       └── RepositoryModule.kt
│   │   └── src/test/kotlin/.../
│   └── build.gradle.kts
├── backend/
│   ├── api/
│   │   └── v1/
│   │       └── authorize.php
│   ├── config/
│   │   ├── database.php
│   │   └── .env.example
│   └── database/
│       └── migrations/
│           └── 001_create_conductores_table.sql
└── README.md
```

## 🛠️ Troubleshooting

### "Service is blocked"
- ✅ Verifica que `telefono_id` exista en tabla `conductores`
- ✅ Confirma que `estado = 'activo'` en la BD
- ✅ Revisa logs: `tail -f /var/log/assiten/authorization.log`

### "Connection timeout"
- ✅ Verifica que `API_BASE_URL` sea correcto
- ✅ Comprueba conectividad: `ping your-domain.com`
- ✅ Revisa firewall en cPanel

### "Rate limit exceeded"
- ✅ Espera 60 segundos antes de reintentar
- ✅ O ajusta `MAX_REQUESTS_PER_MINUTE` en `authorize.php`

## 📝 Licencia

MIT License

## 👨‍💼 Contacto

Para soporte y preguntas: [yosmari0516@gmail.com](mailto:yosmari0516@gmail.com)
