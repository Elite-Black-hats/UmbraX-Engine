package com.quantum.engine.core

import com.quantum.engine.core.ecs.EntityManager
import com.quantum.engine.core.ecs.SystemManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureNanoTime

/**
 * QuantumEngine - Motor principal del juego
 * 
 * Arquitectura de nivel AAA con:
 * - Game loop de alta precisión
 * - Fixed timestep para física
 * - Variable timestep para rendering
 * - Multi-threading optimizado
 * - Memory pooling
 * - Performance profiling integrado
 */
class QuantumEngine private constructor(
    private val config: EngineConfig
) {
    
    // Core systems
    val entityManager = EntityManager()
    val systemManager = SystemManager(entityManager)
    
    // Engine state
    private val _state = MutableStateFlow(EngineState.STOPPED)
    val state: StateFlow<EngineState> = _state
    
    private val isRunning = AtomicBoolean(false)
    private var engineScope: CoroutineScope? = null
    
    // Timing
    private var lastFrameTime: Long = 0
    private var accumulator: Float = 0f
    private var currentTime: Long = 0
    
    // Performance metrics
    private val metrics = PerformanceMetrics()
    
    // Frame callback
    var onFrame: ((FrameInfo) -> Unit)? = null
    
    /**
     * Inicializa el motor
     */
    fun initialize() {
        if (_state.value != EngineState.STOPPED) {
            Timber.w("Engine already initialized")
            return
        }
        
        Timber.i("Initializing Quantum Engine...")
        Timber.i("Configuration: $config")
        
        // Inicializar subsistemas
        initializeSubsystems()
        
        _state.value = EngineState.INITIALIZED
        Timber.i("Engine initialized successfully")
    }
    
    private fun initializeSubsystems() {
        // TODO: Inicializar subsistemas (renderer, audio, physics, etc)
        Timber.d("Initializing subsystems...")
    }
    
    /**
     * Inicia el game loop
     */
    fun start() {
        if (isRunning.get()) {
            Timber.w("Engine already running")
            return
        }
        
        if (_state.value != EngineState.INITIALIZED) {
            throw IllegalStateException("Engine must be initialized before starting")
        }
        
        Timber.i("Starting engine...")
        
        isRunning.set(true)
        _state.value = EngineState.RUNNING
        
        // Crear scope de coroutines para el engine
        engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        
        // Iniciar game loop
        engineScope?.launch {
            runGameLoop()
        }
        
        Timber.i("Engine started")
    }
    
    /**
     * Pausa el motor
     */
    fun pause() {
        if (_state.value != EngineState.RUNNING) return
        
        _state.value = EngineState.PAUSED
        Timber.i("Engine paused")
    }
    
    /**
     * Reanuda el motor
     */
    fun resume() {
        if (_state.value != EngineState.PAUSED) return
        
        _state.value = EngineState.RUNNING
        lastFrameTime = System.nanoTime()
        Timber.i("Engine resumed")
    }
    
    /**
     * Detiene el motor
     */
    fun stop() {
        if (!isRunning.get()) return
        
        Timber.i("Stopping engine...")
        
        isRunning.set(false)
        engineScope?.cancel()
        engineScope = null
        
        _state.value = EngineState.STOPPED
        Timber.i("Engine stopped")
    }
    
    /**
     * Cierra el motor completamente
     */
    fun shutdown() {
        stop()
        
        Timber.i("Shutting down engine...")
        
        // Limpiar sistemas
        systemManager.shutdown()
        entityManager.clear()
        
        Timber.i("Engine shutdown complete")
    }
    
    /**
     * Game Loop principal - Arquitectura AAA
     * 
     * Usa un fixed timestep para física y variable timestep para rendering
     * siguiendo el patrón "Fix Your Timestep" de Glenn Fiedler
     */
    private suspend fun runGameLoop() {
        lastFrameTime = System.nanoTime()
        currentTime = lastFrameTime
        
        while (isRunning.get()) {
            val frameStartTime = System.nanoTime()
            
            // Calcular delta time
            val newTime = System.nanoTime()
            val frameTime = (newTime - currentTime) / 1_000_000_000f
            currentTime = newTime
            
            // Limitar delta time para evitar spiral of death
            val deltaTime = frameTime.coerceAtMost(config.maxFrameTime)
            
            if (_state.value == EngineState.RUNNING) {
                // Actualizar con fixed timestep
                accumulator += deltaTime
                
                var fixedUpdateCount = 0
                while (accumulator >= config.fixedTimeStep) {
                    // Fixed update (física, etc)
                    fixedUpdate(config.fixedTimeStep)
                    
                    accumulator -= config.fixedTimeStep
                    fixedUpdateCount++
                    
                    // Prevenir spiral of death
                    if (fixedUpdateCount >= config.maxFixedUpdatesPerFrame) {
                        accumulator = 0f
                        Timber.w("Spiral of death detected, resetting accumulator")
                        break
                    }
                }
                
                // Variable update (input, IA, etc)
                update(deltaTime)
                
                // Frame callback (rendering, etc)
                val alpha = accumulator / config.fixedTimeStep
                val frameInfo = FrameInfo(
                    deltaTime = deltaTime,
                    fixedDeltaTime = config.fixedTimeStep,
                    alpha = alpha,
                    frameNumber = metrics.frameCount,
                    fps = metrics.fps
                )
                onFrame?.invoke(frameInfo)
                
                // Destruir entidades marcadas
                entityManager.processDestroyedEntities()
            }
            
            // Actualizar métricas
            val frameEndTime = System.nanoTime()
            val frameDuration = (frameEndTime - frameStartTime) / 1_000_000f // ms
            
            metrics.update(frameDuration, deltaTime)
            
            // Frame rate limiting
            if (config.targetFPS > 0) {
                val targetFrameTime = 1000f / config.targetFPS
                val sleepTime = (targetFrameTime - frameDuration).toLong()
                if (sleepTime > 0) {
                    delay(sleepTime)
                }
            } else {
                // Yield para evitar bloquear el thread
                yield()
            }
        }
    }
    
    /**
     * Actualización de física (fixed timestep)
     */
    private fun fixedUpdate(fixedDeltaTime: Float) {
        val time = measureNanoTime {
            systemManager.fixedUpdate(fixedDeltaTime)
        }
        metrics.fixedUpdateTime = time / 1_000_000f
    }
    
    /**
     * Actualización general (variable timestep)
     */
    private fun update(deltaTime: Float) {
        val time = measureNanoTime {
            systemManager.update(deltaTime)
        }
        metrics.updateTime = time / 1_000_000f
    }
    
    /**
     * Obtiene las métricas de performance
     */
    fun getMetrics(): PerformanceMetrics = metrics.copy()
    
    /**
     * Builder pattern para crear el engine
     */
    companion object {
        fun builder(): Builder = Builder()
    }
    
    class Builder {
        private var config = EngineConfig()
        
        fun config(block: EngineConfig.() -> Unit): Builder {
            config.apply(block)
            return this
        }
        
        fun build(): QuantumEngine {
            return QuantumEngine(config)
        }
    }
}

/**
 * Configuración del motor
 */
data class EngineConfig(
    /**
     * Timestep fijo para física (60 FPS = 0.0166s)
     */
    var fixedTimeStep: Float = 1f / 60f,
    
    /**
     * FPS objetivo (0 = sin límite)
     */
    var targetFPS: Int = 60,
    
    /**
     * Máximo frame time para prevenir spiral of death
     */
    var maxFrameTime: Float = 0.25f,
    
    /**
     * Máximo número de fixed updates por frame
     */
    var maxFixedUpdatesPerFrame: Int = 5,
    
    /**
     * Habilitar multi-threading
     */
    var enableMultiThreading: Boolean = true,
    
    /**
     * Número de worker threads
     */
    var workerThreads: Int = Runtime.getRuntime().availableProcessors() - 1,
    
    /**
     * Tamaño inicial del entity pool
     */
    var entityPoolSize: Int = 1024,
    
    /**
     * Habilitar profiling
     */
    var enableProfiling: Boolean = true,
    
    /**
     * Habilitar VSync
     */
    var enableVSync: Boolean = true,
    
    /**
     * API de renderizado preferida
     */
    var renderAPI: RenderAPI = RenderAPI.VULKAN
)

/**
 * Estados del motor
 */
enum class EngineState {
    STOPPED,
    INITIALIZED,
    RUNNING,
    PAUSED
}

/**
 * API de renderizado
 */
enum class RenderAPI {
    VULKAN,
    OPENGL_ES
}

/**
 * Información de frame para interpolación
 */
data class FrameInfo(
    val deltaTime: Float,
    val fixedDeltaTime: Float,
    val alpha: Float, // Factor de interpolación para rendering
    val frameNumber: Long,
    val fps: Float
)

/**
 * Métricas de performance
 */
data class PerformanceMetrics(
    var fps: Float = 0f,
    var frameTime: Float = 0f,
    var updateTime: Float = 0f,
    var fixedUpdateTime: Float = 0f,
    var renderTime: Float = 0f,
    var frameCount: Long = 0,
    
    // Métricas de memoria
    var usedMemoryMB: Float = 0f,
    var allocatedMemoryMB: Float = 0f,
    
    // Contadores
    var drawCalls: Int = 0,
    var triangles: Int = 0,
    var entities: Int = 0
) {
    private var fpsAccumulator = 0f
    private var fpsFrameCount = 0
    private val fpsUpdateInterval = 0.5f // Actualizar FPS cada 0.5s
    
    fun update(frameDurationMs: Float, deltaTime: Float) {
        frameTime = frameDurationMs
        frameCount++
        
        // Calcular FPS promedio
        fpsAccumulator += deltaTime
        fpsFrameCount++
        
        if (fpsAccumulator >= fpsUpdateInterval) {
            fps = fpsFrameCount / fpsAccumulator
            fpsAccumulator = 0f
            fpsFrameCount = 0
        }
        
        // Actualizar memoria
        val runtime = Runtime.getRuntime()
        usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)
        allocatedMemoryMB = runtime.totalMemory() / (1024f * 1024f)
    }
    
    fun copy(): PerformanceMetrics = PerformanceMetrics(
        fps, frameTime, updateTime, fixedUpdateTime, renderTime,
        frameCount, usedMemoryMB, allocatedMemoryMB,
        drawCalls, triangles, entities
    )
}

/**
 * Eventos del motor
 */
sealed class EngineEvent {
    object Initialized : EngineEvent()
    object Started : EngineEvent()
    object Paused : EngineEvent()
    object Resumed : EngineEvent()
    object Stopped : EngineEvent()
    data class Error(val exception: Exception) : EngineEvent()
}
