# 🚀 Quantum Engine - Compilación en CodeAssist

## ✅ Proyecto Listo para CodeAssist

Este proyecto está completamente configurado para compilarse en **CodeAssist** en tu dispositivo Android.

---

## 📱 Requisitos

- **CodeAssist** instalado en tu Android
- **Android SDK** configurado en CodeAssist
- **Espacio**: ~500MB libres
- **RAM**: Mínimo 4GB (recomendado 6GB+)

---

## 🔧 Configuración en CodeAssist

### 1. Abrir Proyecto

1. Abre **CodeAssist**
2. Toca en **"Open Project"**
3. Navega a la carpeta `QuantumEngine`
4. Selecciona la carpeta

### 2. Configurar SDK Path

Si CodeAssist no encuentra el SDK automáticamente:

1. Abre `local.properties`
2. Actualiza la ruta del SDK:
```properties
sdk.dir=/storage/emulated/0/Android/Sdk
```
O la ruta donde tengas instalado el Android SDK en tu dispositivo.

### 3. Sincronizar Gradle

1. En CodeAssist, toca el botón **"Sync"** o **"Build"**
2. Espera a que descargue las dependencias (primera vez: ~10-15 minutos)
3. Si hay errores, revisa la sección de Troubleshooting

---

## 🏗️ Compilar el Proyecto

### Opción 1: Compilar APK Debug

```
En CodeAssist:
1. Menu → Build → Build APK
2. Espera a que compile (~5-10 minutos primera vez)
3. APK estará en: app/build/outputs/apk/debug/
```

### Opción 2: Ejecutar en Dispositivo

```
1. Conecta otro dispositivo Android via ADB
2. O usa el mismo dispositivo (compilar e instalar)
3. Menu → Run → Run 'app'
```

### Opción 3: Línea de Comandos

Si CodeAssist tiene terminal:
```bash
./gradlew assembleDebug
```

---

## 📦 Estructura del Proyecto

```
QuantumEngine/
├── app/                    # Aplicación demo
│   └── MainActivity.kt     # Actividad principal con rendering 3D
│
├── qe-core/               # Motor central (ECS, Game Loop)
├── qe-math/               # Matemáticas (Vector, Matrix, Quaternion)
├── qe-physics/            # Física (Colisiones, Rigidbody)
├── qe-renderer-common/    # Interfaces de rendering
└── qe-renderer-gles/      # Renderer OpenGL ES 3.0
```

---

## 🎮 Funcionalidades de la Demo

La app de demostración muestra:

✅ **Rendering 3D con OpenGL ES**
- Cubo giratorio naranja
- Esfera azul
- Plano gris (suelo)

✅ **Sistema de Física**
- Gravedad funcionando
- Colisiones entre objetos
- Rigidbody dinámicos

✅ **Interacción**
- Toca la pantalla para aplicar impulso al cubo

✅ **Cámara 3D**
- Proyección perspectiva
- Look-at automático

✅ **Performance**
- 60 FPS objetivo
- Stats en logcat

---

## 🐛 Troubleshooting

### Error: SDK not found

**Solución:**
1. Instala Android SDK en tu dispositivo
2. Actualiza `local.properties` con la ruta correcta
3. Común: `/storage/emulated/0/Android/Sdk`

### Error: Out of memory

**Solución:**
1. Cierra otras apps
2. Limpia cache de Gradle:
   ```
   Menu → Build → Clean Project
   ```
3. Reduce `org.gradle.jvmargs` en `gradle.properties`:
   ```
   org.gradle.jvmargs=-Xmx1536m
   ```

### Error: Compilation failed

**Solución:**
1. Verifica que todos los módulos estén sincronizados
2. Build → Rebuild Project
3. Revisa logcat para errores específicos

### APK no instala

**Solución:**
1. Habilita "Unknown Sources" en ajustes
2. Verifica que el APK no esté corrupto
3. Reinstala si es necesario

---

## 📊 Configuración Recomendada

### Para dispositivos con 4GB RAM:
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx1536m
org.gradle.parallel=false
```

### Para dispositivos con 6GB+ RAM:
```properties
# gradle.properties (actual)
org.gradle.jvmargs=-Xmx2048m
org.gradle.parallel=true
org.gradle.caching=true
```

---

## 🚀 Optimizaciones

### Compilación Más Rápida

1. **Habilitar Gradle Daemon:**
   Ya configurado en `gradle.properties`

2. **Configuración Bajo Demanda:**
   ```
   org.gradle.configureondemand=true
   ```

3. **No compilar módulos innecesarios:**
   Si solo quieres la demo, comenta módulos en `settings.gradle.kts`

### Reducir Tamaño del APK

1. **Habilitar ProGuard** (ya configurado para release)
2. **Habilitar R8:**
   ```
   android.enableR8=true
   ```

---

## 📝 Logs y Debugging

### Ver Logs en Tiempo Real

En CodeAssist:
```
Menu → Logcat
```

Filtra por:
- `QuantumEngine` - Logs del motor
- `GLESRenderer` - Logs de rendering
- `PhysicsSystem` - Logs de física

### Logs Importantes

```
I/QuantumEngine: Initializing Quantum Engine...
I/QuantumEngine: Engine initialized successfully
I/GLESRenderer: Initializing GLES Renderer
I/GLESRenderer: GLES Renderer initialized
D/GameRenderer: FPS: 60.0 | DrawCalls: 3 | Triangles: 72
```

---

## 🎯 Próximos Pasos

Una vez compilado exitosamente:

1. **Modifica la escena** en `MainActivity.kt`
2. **Añade más objetos** 3D
3. **Experimenta con física** (cambiar gravity, mass, etc)
4. **Crea tus propios shaders** en `GLESRenderer.kt`
5. **Añade texturas** y materiales

---

## 💡 Tips para CodeAssist

### Productividad

1. **Auto-completado**: Usa Ctrl+Space
2. **Imports**: Ctrl+Shift+O
3. **Formatear código**: Ctrl+Alt+L
4. **Buscar**: Ctrl+F

### Performance

1. **Cierra archivos** no usados
2. **Limpia build/** periódicamente
3. **Reinicia CodeAssist** si se pone lento

---

## 🆘 Soporte

### Recursos

- **Documentación**: Ver `ARCHITECTURE.md`
- **Ejemplos**: Ver `MainActivity.kt`
- **API Reference**: KDoc en código

### Problemas Comunes

| Problema | Solución |
|----------|----------|
| Gradle sync falla | Verifica conexión a internet |
| Build muy lento | Reduce Xmx en gradle.properties |
| App crashea al abrir | Revisa permisos en manifest |
| Pantalla negra | Verifica que OpenGL ES 3.0 esté soportado |

---

## ✅ Checklist de Verificación

Antes de compilar, verifica:

- [ ] CodeAssist instalado y actualizado
- [ ] Android SDK configurado
- [ ] Ruta SDK correcta en `local.properties`
- [ ] Espacio suficiente (~500MB)
- [ ] Internet para dependencias (primera vez)

---

## 🎉 ¡Listo!

Si todo está correcto, deberías poder:

1. ✅ Abrir proyecto en CodeAssist
2. ✅ Sincronizar Gradle exitosamente
3. ✅ Compilar APK sin errores
4. ✅ Instalar y ejecutar la demo
5. ✅ Ver objetos 3D con física funcionando

**¡Disfruta desarrollando con Quantum Engine!** 🚀

---

## 📞 Información Adicional

**Versión**: 0.3.0-alpha  
**Target SDK**: 34 (Android 14)  
**Min SDK**: 24 (Android 7.0)  
**OpenGL ES**: 3.0  
**Lenguaje**: 100% Kotlin  

**Tamaño APK Debug**: ~15-20MB  
**Tamaño APK Release**: ~8-10MB  

**Tiempo Primera Compilación**: 10-15 minutos  
**Tiempo Compilaciones Incrementales**: 1-3 minutos  

---

**Nota**: Si encuentras algún problema no cubierto aquí, revisa los logs detallados en Logcat para diagnosticar el issue específico.
