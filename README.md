# ⚡ SENSIS GOOD FF — APK Nativo Kotlin

APK nativo Android compilado automáticamente por GitHub Actions.

---

## 🔐 Secrets que debes configurar en GitHub

Ve a tu repo → **Settings → Secrets and variables → Actions → New repository secret**

| Secret | Qué es |
|--------|--------|
| `KEYSTORE_BASE64` | Tu keystore.jks codificado en base64 |
| `KEYSTORE_PASS` | Contraseña del keystore |
| `KEY_ALIAS` | Alias de la clave (ej: `sensisgoodff`) |
| `KEY_PASS` | Contraseña de la clave |

### Cómo convertir tu keystore a base64:
```bash
base64 -w 0 keystore.jks > keystore_b64.txt
```
Copia el contenido de `keystore_b64.txt` como el Secret `KEYSTORE_BASE64`.

---

## 📧 Configurar EmailJS

1. Ve a https://www.emailjs.com y crea cuenta
2. Conecta tu Gmail (`charlysaenzx2657@gmail.com`)
3. Crea un template con estas variables:
   - `{{otp_code}}` — el código de 6 dígitos
   - `{{device}}` — modelo del dispositivo
   - `{{device_id}}` — ID del dispositivo
4. Abre `app/src/main/java/com/sensis/goodff/auth/EmailOtpManager.kt`
5. Rellena:
```kotlin
private const val EMAILJS_SERVICE_ID  = "service_xxxxxxx"
private const val EMAILJS_TEMPLATE_ID = "template_xxxxxxx"
private const val EMAILJS_PUBLIC_KEY  = "tu_public_key"
```

---

## 🚀 Cómo compilar

1. Sube este proyecto a tu repo en GitHub
2. Configura los 4 Secrets
3. Haz push a `main`
4. GitHub Actions compila automáticamente
5. Descarga el APK desde **Actions → tu build → Artifacts**
   o desde **Releases** (se crea automático)

---

## 🛡️ Capas de seguridad incluidas

| Capa | Descripción |
|------|-------------|
| **Firma digital** | El APK solo corre si está firmado con TU keystore. Si alguien lo modifica y lo re-firma, la firma cambia → app bloqueada |
| **Hash del certificado** | El SHA-256 del certificado se inyecta en el código en tiempo de compilación. Al arrancar, compara la firma real del APK con ese hash |
| **OTP por email** | Nadie entra sin un código que llega a tu Gmail. El código expira en 10 minutos |
| **Sesión cifrada** | La sesión se guarda en `EncryptedSharedPreferences` (AES-256-GCM), no legible fuera del app |
| **ProGuard R8** | Código ofuscado y comprimido. Dificulta reverse engineering |
| **Solo HTTPS** | `network_security_config.xml` bloquea HTTP plano |
| **Detección de emulador** | Detecta Android SDK, Genymotion, etc. |

---

## 📁 Estructura del proyecto

```
SensisGoodFF/
├── .github/workflows/build.yml        ← CI/CD GitHub Actions
├── app/
│   ├── build.gradle.kts               ← Config compilación + firma
│   ├── proguard-rules.pro             ← Ofuscación
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/sensis/goodff/
│       │   ├── MainActivity.kt        ← Activity principal
│       │   ├── auth/
│       │   │   └── EmailOtpManager.kt ← Sistema OTP
│       │   ├── generator/
│       │   │   └── SensibilidadEngine.kt ← Lógica generador
│       │   ├── security/
│       │   │   └── AppIntegrity.kt    ← Verificación de firma
│       │   └── ui/
│       │       └── MarcaSpinnerAdapter.kt
│       └── res/
│           ├── layout/                ← XMLs de pantallas
│           ├── values/                ← Colores, strings, temas
│           └── xml/network_security_config.xml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## ⚠️ Importante

- Nunca subas el `keystore.jks` al repo — usa el Secret en base64
- El APK debug (sin keystore) funciona para probar, pero la verificación de firma está desactivada en debug
- En release, si alguien descompila el APK y lo re-firma → **bloqueado**
