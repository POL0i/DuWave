# Requisitos y Lecciones Aprendidas para F-Droid (DuWave)

Este documento contiene un resumen de los requisitos de F-Droid y los problemas que enfrentamos durante la publicación de la versión 2.0, para tenerlos en cuenta en futuras actualizaciones.

## 1. Reproducibilidad (Reproducible Builds)
Para que F-Droid pueda usar tus firmas (`Binaries` y `AllowedAPKSigningKeys`), el código debe ser 100% determinista.
- **El Problema:** La inclusión del archivo `youtube_config.json` en el `.gitignore` provocó que el servidor de F-Droid no pudiera descargarlo, rompiendo la compilación o generando un APK distinto al tuyo.
- **La Solución:** Todo archivo necesario para la compilación DEBE estar en el repositorio público. Eliminamos la dependencia de archivos locales ignorados migrando directamente a usar `NewPipeExtractor`.

## 2. Firmado de APKs (v3 Scheme & Padding)
F-Droid utiliza `apksigcopier` para extraer la firma del APK que tú provees en GitHub y pegarla en el APK que ellos compilan.
- **El Problema:** Al compilar y firmar el APK en tu computadora con herramientas recientes (`build-tools 35`), `apksigner` inyecta un relleno de 4096 bytes (16K page alignment). `apksigcopier` es un script antiguo que no sabe manejar ese relleno de 4K, por lo que las firmas se descuadran y produce el error `CHUNKED_SHA256 digest mismatch`.
- **La Solución:** Al firmar el APK de *Release* para F-Droid/GitHub, asegúrate de **siempre** usar el flag `--alignment-preserved` si usas build-tools 35+.
  ```bash
  /home/denis/Android/Sdk/build-tools/35.0.0/apksigner sign --ks release.keystore --ks-key-alias duwave --ks-pass pass:'TU_CLAVE' --key-pass pass:'TU_CLAVE' --alignment-preserved --out app-release.apk app/build/outputs/apk/release/app-release.apk
  ```

## 3. Metadatos de F-Droid (yml)
F-Droid utiliza el archivo `com.polonio.duwave.yml` en su repositorio `fdroiddata`.
- Asegúrate de que las opciones de actualización automática estén activas para no tener que crear un Merge Request manual cada vez:
  - `AutoUpdateMode: Version`
  - `UpdateCheckMode: Tags`
- De este modo, basta con subir la nueva versión a tu GitHub, crear un **Release** con la etiqueta correcta (e.g. `v2.0.1`), adjuntar el APK firmado correctamente, y el bot de F-Droid lo hará todo automáticamente.

## 4. Imágenes y fastlane
- F-Droid escanea la carpeta `fastlane/metadata/android/en-US/images/phoneScreenshots/` de tu repositorio para mostrar capturas de pantalla.
- Si cambias el diseño de la app, simplemente actualiza las imágenes en esa carpeta, haz el commit y súbelo junto con la nueva etiqueta de versión. F-Droid actualizará las imágenes de la tienda automáticamente.

## 5. Toolchains de Java y F-Droid
F-Droid deshabilita la autodescarga de JDKs (Toolchain auto-provisioning is not enabled) por motivos de seguridad.
- **El Problema:** Al usar `jvmToolchain(17)` en `build.gradle.kts`, F-Droid falla porque no puede descargar el JDK 17 y su servidor por defecto usa JDK 21. Esto también provoca que el archivo de metadatos de Kotlin (`kotlin-tooling-metadata.json`) tenga un hash distinto al compilar.
- **La Solución:** No uses `jvmToolchain`. En su lugar, usa el compilador por defecto del entorno y fuerza el `jvmTarget` a 17 en todas tus compilaciones de Kotlin (Android y Desktop).
  ```kotlin
  compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
  }
  ```

## 6. Metadatos de Dependencias de AGP (F-Droid Scanner)
Las versiones recientes de Android Gradle Plugin (AGP 8.x) inyectan automáticamente los metadatos de tus dependencias dentro del bloque de firmas del APK.
- **El Problema:** El escáner de F-Droid (`fdroidscanner`) detecta este bloque y hace fallar la compilación con el error `Found extra signing block 'Dependency metadata'`.
- **La Solución:** Desactiva esta inyección agregando este bloque dentro de `android {}` en tu `build.gradle.kts`:
  ```kotlin
  dependenciesInfo {
      includeInApk = false
      includeInBundle = false
  }
  ```

## 7. Desincronización del Hash del Commit en el APK
Android incrusta el hash exacto del commit actual (HEAD) dentro de un archivo oculto del APK llamado `version-control-info.textproto`.
- **El Problema:** Si compilas el APK (`./gradlew assembleRelease`) ANTES de hacer el `git commit`, el APK resultante guardará el hash antiguo o quedará marcado como `-dirty`. Cuando F-Droid descargue tu nuevo commit y lo compile, el hash no coincidirá y fallará la prueba de reproducibilidad (`revision: hash_viejo` vs `revision: hash_nuevo`).
- **La Solución:** El orden estricto para generar el APK de referencia debe ser siempre:
  1. Guardar todos los cambios.
  2. Hacer el `git commit` y el `git tag`.
  3. Ejecutar `./gradlew clean` y `./gradlew assembleRelease` para generar el APK con el hash definitivo.
