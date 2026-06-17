<?php
/**
 * test_authorize.php - Script de prueba del endpoint
 * 
 * Uso:
 * 1. Sube este archivo a cPanel
 * 2. Accede desde navegador: https://tu-dominio.com/assiten/test_authorize.php
 * 3. Verifica que el endpoint funciona
 */

header('Content-Type: text/html; charset=utf-8');
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Assiten API Test</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            max-width: 900px;
            margin: 0 auto;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
        }
        .container {
            background: white;
            border-radius: 10px;
            padding: 30px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        }
        h1 {
            color: #667eea;
            border-bottom: 2px solid #667eea;
            padding-bottom: 10px;
        }
        .form-group {
            margin: 20px 0;
        }
        label {
            display: block;
            font-weight: bold;
            margin-bottom: 5px;
            color: #333;
        }
        input, textarea {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-family: 'Courier New', monospace;
            box-sizing: border-box;
        }
        button {
            background: #667eea;
            color: white;
            padding: 12px 30px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 16px;
            font-weight: bold;
            margin-top: 20px;
        }
        button:hover {
            background: #764ba2;
        }
        .result {
            background: #f5f5f5;
            padding: 15px;
            border-radius: 5px;
            margin-top: 20px;
            border-left: 4px solid #667eea;
        }
        .success {
            background: #d4edda;
            border-left-color: #28a745;
            color: #155724;
        }
        .error {
            background: #f8d7da;
            border-left-color: #dc3545;
            color: #721c24;
        }
        .info {
            background: #d1ecf1;
            border-left-color: #17a2b8;
            color: #0c5460;
        }
        code {
            background: #fff3cd;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: 'Courier New', monospace;
        }
        pre {
            background: #272822;
            color: #f8f8f2;
            padding: 15px;
            border-radius: 5px;
            overflow-x: auto;
        }
        .test-buttons {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
            margin-top: 20px;
        }
        .test-buttons button {
            margin: 0;
            width: 100%;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🧪 Assiten API Test</h1>
        
        <div class="result info">
            <strong>ℹ️ Instrucciones:</strong>
            <ol>
                <li>Completa los campos abajo (o usa los valores por defecto)
                <li>Haz clic en "Test API"
                <li>Verifica la respuesta del servidor
            </ol>
        </div>

        <form id="testForm">
            <div class="form-group">
                <label for="apiKey">API Key:</label>
                <input type="text" id="apiKey" name="apiKey" 
                    value="your-secure-api-key-here" required>
                <small>Debe coincidir con API_KEY en config/.env</small>
            </div>

            <div class="form-group">
                <label for="authToken">Auth Token:</label>
                <input type="text" id="authToken" name="authToken" 
                    value="550e8400-e29b-41d4-a716-446655440000" required>
                <small>UUID válido del cliente</small>
            </div>

            <div class="form-group">
                <label for="telefonoId">Telefono ID (con firma):</label>
                <input type="text" id="telefonoId" name="telefonoId" 
                    value="device-123:abc123def456" required>
                <small>Formato: deviceId:signature (HMAC-SHA256)</small>
            </div>

            <div class="form-group">
                <label for="accion">Acción:</label>
                <input type="text" id="accion" name="accion" 
                    value="click" required>
                <small>Tipo de acción: click, text_input, health_check, etc.</small>
            </div>

            <div class="form-group">
                <label for="elementoId">Elemento ID (opcional):</label>
                <input type="text" id="elementoId" name="elementoId" 
                    value="btn_login" placeholder="Dejar vacío si no aplica">
            </div>

            <div class="test-buttons">
                <button type="button" onclick="testApi()">✅ Test API</button>
                <button type="button" onclick="testFraud()">🔴 Test Fraude</button>
            </div>
        </form>

        <div id="result"></div>
    </div>

    <script>
        async function testApi() {
            const apiKey = document.getElementById('apiKey').value;
            const authToken = document.getElementById('authToken').value;
            const telefonoId = document.getElementById('telefonoId').value;
            const accion = document.getElementById('accion').value;
            const elementoId = document.getElementById('elementoId').value;

            const data = {
                telefono_id: telefonoId,
                accion: accion,
                elemento_id: elementoId || null,
                timestamp: Date.now()
            };

            const resultDiv = document.getElementById('result');
            resultDiv.innerHTML = '<div class="result info">⏳ Enviando request...</div>';

            try {
                const response = await fetch('./api/v1/authorize.php', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-API-Key': apiKey,
                        'X-Auth-Token': authToken
                    },
                    body: JSON.stringify(data)
                });

                const responseData = await response.json();
                const status = response.ok ? 'success' : 'error';

                resultDiv.innerHTML = `
                    <div class="result ${status}">
                        <strong>${response.status} ${response.statusText}</strong>
                        <pre>${JSON.stringify(responseData, null, 2)}</pre>
                    </div>
                `;
            } catch (error) {
                resultDiv.innerHTML = `
                    <div class="result error">
                        <strong>❌ Error:</strong>
                        <p>${error.message}</p>
                        <small>Verifica que la URL sea correcta y el servidor esté corriendo</small>
                    </div>
                `;
            }
        }

        function testFraud() {
            document.getElementById('telefonoId').value = 'fake-device:invalid-signature';
            document.getElementById('result').innerHTML = `
                <div class="result info">
                    <strong>🔴 Test de Fraude Configurado</strong>
                    <p>Se enviará un deviceId con firma inválida.</p>
                    <p>Respuesta esperada: <code>403 Forbidden - fraude_detectado</code></p>
                </div>
            `;
        }

        // Test automático al cargar
        window.addEventListener('load', () => {
            const resultDiv = document.getElementById('result');
            resultDiv.innerHTML = `
                <div class="result info">
                    <strong>✅ Test Listo</strong>
                    <p>Haz clic en "Test API" para comenzar la prueba.</p>
                    <p><strong>Nota:</strong> Si ves error 404, primero configura config/.env en cPanel</p>
                </div>
            `;
        });
    </script>
</body>
</html>
