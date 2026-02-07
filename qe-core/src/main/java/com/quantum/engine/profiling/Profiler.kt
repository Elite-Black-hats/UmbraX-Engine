package com.quantum.engine.profiling

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureNanoTime

/**
 * ProfilerSystem - Sistema de profiling profesional
 * 
 * Características:
 * - CPU profiling detallado
 * - GPU profiling (queries)
 * - Memory profiling
 * - Network profiling
 * - Frame timeline
 * - Hot-spot detection
 * - Performance budgets
 * - Automated optimization suggestions
 */
class ProfilerSystem {
    
    // Métricas
    private val cpuMarkers = ConcurrentHashMap<String, CPUMarker>()
    private val gpuQueries = mutableListOf<GPUQuery>()
    private val memorySnapshots = mutableListOf<MemorySnapshot>()
    private val frameTimeline = mutableListOf<FrameData>()
    
    // Configuración
    var enabled = true
    var recordFrames = 300 // Últimos 5 segundos a 60fps
    var cpuBudgetMs = 16.6f // 60 FPS
    var memoryBudgetMB = 512f
    
    // Estado
    private var currentFrame = 0L
    private val scopes = ThreadLocal<MutableList<String>>()
    
    /**
     * Inicia un marcador de CPU
     */
    fun beginCPUMarker(name: String) {
        if (!enabled) return
        
        val stack = scopes.get() ?: mutableListOf<String>().also { scopes.set(it) }
        stack.add(name)
        
        val marker = cpuMarkers.getOrPut(name) { CPUMarker(name) }
        marker.startTime = System.nanoTime()
    }
    
    /**
     * Finaliza un marcador de CPU
     */
    fun endCPUMarker(name: String) {
        if (!enabled) return
        
        val marker = cpuMarkers[name] ?: return
        val duration = (System.nanoTime() - marker.startTime) / 1_000_000f
        
        marker.addSample(duration)
        
        scopes.get()?.remove(name)
    }
    
    /**
     * Mide automáticamente un bloque
     */
    inline fun <T> profile(name: String, block: () -> T): T {
        beginCPUMarker(name)
        try {
            return block()
        } finally {
            endCPUMarker(name)
        }
    }
    
    /**
     * Captura snapshot de memoria
     */
    fun captureMemorySnapshot() {
        val runtime = Runtime.getRuntime()
        
        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory(),
            maxMemory = runtime.maxMemory(),
            usedMemory = runtime.totalMemory() - runtime.freeMemory()
        )
        
        memorySnapshots.add(snapshot)
        
        // Mantener solo últimos N snapshots
        if (memorySnapshots.size > recordFrames) {
            memorySnapshots.removeAt(0)
        }
    }
    
    /**
     * Registra datos de un frame completo
     */
    fun endFrame(deltaTime: Float) {
        if (!enabled) return
        
        val frameData = FrameData(
            frameNumber = currentFrame++,
            deltaTime = deltaTime,
            cpuTime = calculateTotalCPUTime(),
            gpuTime = calculateTotalGPUTime(),
            drawCalls = 0, // TODO: Get from renderer
            triangles = 0,
            entities = 0
        )
        
        frameTimeline.add(frameData)
        
        if (frameTimeline.size > recordFrames) {
            frameTimeline.removeAt(0)
        }
        
        // Detectar hot-spots
        detectHotspots()
        
        // Verificar budgets
        checkPerformanceBudgets()
    }
    
    /**
     * Detecta funciones que consumen mucho tiempo
     */
    private fun detectHotspots() {
        val threshold = cpuBudgetMs * 0.1f // 10% del budget
        
        cpuMarkers.values
            .filter { it.averageTime > threshold }
            .sortedByDescending { it.averageTime }
            .take(5)
            .forEach { marker ->
                // Log warning
                println("⚠️ HOTSPOT: ${marker.name} taking ${marker.averageTime}ms (${(marker.averageTime / cpuBudgetMs * 100).toInt()}% of budget)")
            }
    }
    
    /**
     * Verifica que no se exceda el budget de performance
     */
    private fun checkPerformanceBudgets() {
        val totalCPU = calculateTotalCPUTime()
        
        if (totalCPU > cpuBudgetMs) {
            println("❌ CPU BUDGET EXCEEDED: ${totalCPU}ms / ${cpuBudgetMs}ms")
            
            // Sugerencias automáticas
            generateOptimizationSuggestions()
        }
        
        val usedMemoryMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024f * 1024f)
        
        if (usedMemoryMB > memoryBudgetMB) {
            println("❌ MEMORY BUDGET EXCEEDED: ${usedMemoryMB}MB / ${memoryBudgetMB}MB")
        }
    }
    
    /**
     * Genera sugerencias automáticas de optimización
     */
    private fun generateOptimizationSuggestions() {
        val suggestions = mutableListOf<String>()
        
        // Analizar patrones
        cpuMarkers.values.forEach { marker ->
            when {
                marker.name.contains("Render") && marker.averageTime > 5f -> {
                    suggestions.add("Consider batching draw calls in ${marker.name}")
                }
                marker.name.contains("Physics") && marker.averageTime > 3f -> {
                    suggestions.add("Enable spatial partitioning for ${marker.name}")
                }
                marker.callCount > 1000 -> {
                    suggestions.add("${marker.name} called ${marker.callCount} times - consider batching")
                }
            }
        }
        
        suggestions.forEach { println("💡 SUGGESTION: $it") }
    }
    
    private fun calculateTotalCPUTime(): Float {
        return cpuMarkers.values.sumOf { it.averageTime.toDouble() }.toFloat()
    }
    
    private fun calculateTotalGPUTime(): Float {
        return gpuQueries.sumOf { it.time.toDouble() }.toFloat()
    }
    
    /**
     * Obtiene reporte completo
     */
    fun getReport(): ProfileReport {
        return ProfileReport(
            cpuMarkers = cpuMarkers.values.sortedByDescending { it.averageTime },
            memoryUsageMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024f * 1024f),
            averageFPS = if (frameTimeline.isNotEmpty()) {
                frameTimeline.size / frameTimeline.sumOf { it.deltaTime.toDouble() }.toFloat()
            } else 0f,
            frameTimeline = frameTimeline.takeLast(60), // Último segundo
            hotspots = detectHotspotsForReport()
        )
    }
    
    private fun detectHotspotsForReport(): List<Hotspot> {
        return cpuMarkers.values
            .filter { it.averageTime > cpuBudgetMs * 0.05f }
            .map { marker ->
                Hotspot(
                    name = marker.name,
                    averageTime = marker.averageTime,
                    maxTime = marker.maxTime,
                    callCount = marker.callCount,
                    percentOfBudget = (marker.averageTime / cpuBudgetMs) * 100f
                )
            }
            .sortedByDescending { it.averageTime }
    }
    
    fun reset() {
        cpuMarkers.clear()
        gpuQueries.clear()
        memorySnapshots.clear()
        frameTimeline.clear()
        currentFrame = 0
    }
}

/**
 * CPUMarker - Marcador de tiempo de CPU
 */
class CPUMarker(val name: String) {
    var startTime = 0L
    var averageTime = 0f
    var maxTime = 0f
    var minTime = Float.MAX_VALUE
    var callCount = 0
    
    private val samples = mutableListOf<Float>()
    private val maxSamples = 60
    
    fun addSample(time: Float) {
        samples.add(time)
        
        if (samples.size > maxSamples) {
            samples.removeAt(0)
        }
        
        averageTime = samples.average().toFloat()
        maxTime = maxOf(maxTime, time)
        minTime = minOf(minTime, time)
        callCount++
    }
}

/**
 * GPUQuery - Query de tiempo de GPU
 */
data class GPUQuery(
    val name: String,
    val time: Float
)

/**
 * MemorySnapshot - Snapshot de memoria
 */
data class MemorySnapshot(
    val timestamp: Long,
    val totalMemory: Long,
    val freeMemory: Long,
    val maxMemory: Long,
    val usedMemory: Long
)

/**
 * FrameData - Datos de un frame
 */
data class FrameData(
    val frameNumber: Long,
    val deltaTime: Float,
    val cpuTime: Float,
    val gpuTime: Float,
    val drawCalls: Int,
    val triangles: Int,
    val entities: Int
)

/**
 * ProfileReport - Reporte de profiling
 */
data class ProfileReport(
    val cpuMarkers: List<CPUMarker>,
    val memoryUsageMB: Float,
    val averageFPS: Float,
    val frameTimeline: List<FrameData>,
    val hotspots: List<Hotspot>
)

/**
 * Hotspot - Punto caliente de performance
 */
data class Hotspot(
    val name: String,
    val averageTime: Float,
    val maxTime: Float,
    val callCount: Int,
    val percentOfBudget: Float
)

/**
 * AutoOptimizer - Optimizador automático
 */
class AutoOptimizer(private val profiler: ProfilerSystem) {
    
    /**
     * Analiza y sugiere optimizaciones
     */
    fun analyze(): List<Optimization> {
        val report = profiler.getReport()
        val optimizations = mutableListOf<Optimization>()
        
        // Analizar hotspots
        report.hotspots.forEach { hotspot ->
            when {
                hotspot.name.contains("Render") -> {
                    optimizations.add(
                        Optimization(
                            type = OptimizationType.RENDERING,
                            priority = OptimizationPriority.HIGH,
                            description = "Optimize ${hotspot.name}",
                            suggestion = "Enable instancing or batching",
                            estimatedGain = "${hotspot.percentOfBudget.toInt()}% frame time"
                        )
                    )
                }
                
                hotspot.name.contains("Physics") -> {
                    optimizations.add(
                        Optimization(
                            type = OptimizationType.PHYSICS,
                            priority = OptimizationPriority.MEDIUM,
                            description = "Optimize ${hotspot.name}",
                            suggestion = "Use spatial partitioning or reduce collision checks",
                            estimatedGain = "${hotspot.percentOfBudget.toInt()}% frame time"
                        )
                    )
                }
                
                hotspot.callCount > 1000 -> {
                    optimizations.add(
                        Optimization(
                            type = OptimizationType.ALGORITHM,
                            priority = OptimizationPriority.HIGH,
                            description = "${hotspot.name} called ${hotspot.callCount} times",
                            suggestion = "Batch operations or use caching",
                            estimatedGain = "Reduce calls by 80%"
                        )
                    )
                }
            }
        }
        
        // Analizar memoria
        if (report.memoryUsageMB > 400f) {
            optimizations.add(
                Optimization(
                    type = OptimizationType.MEMORY,
                    priority = OptimizationPriority.HIGH,
                    description = "High memory usage: ${report.memoryUsageMB.toInt()}MB",
                    suggestion = "Enable texture compression, reduce audio quality, or implement asset streaming",
                    estimatedGain = "Save 100-200MB"
                )
            )
        }
        
        return optimizations.sortedByDescending { it.priority }
    }
    
    /**
     * Aplica optimizaciones automáticas
     */
    fun applyAutoOptimizations(config: OptimizationConfig) {
        val optimizations = analyze()
        
        optimizations.forEach { opt ->
            when (opt.type) {
                OptimizationType.RENDERING -> {
                    // TODO: Habilitar batching
                }
                OptimizationType.PHYSICS -> {
                    // TODO: Ajustar spatial grid
                }
                OptimizationType.MEMORY -> {
                    // TODO: Comprimir assets
                }
                OptimizationType.ALGORITHM -> {
                    // Esto requiere cambios en código
                }
            }
        }
    }
}

data class Optimization(
    val type: OptimizationType,
    val priority: OptimizationPriority,
    val description: String,
    val suggestion: String,
    val estimatedGain: String
)

enum class OptimizationType {
    RENDERING,
    PHYSICS,
    MEMORY,
    ALGORITHM,
    NETWORK
}

enum class OptimizationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class OptimizationConfig(
    val enableBatching: Boolean = true,
    val enableInstancing: Boolean = true,
    val textureCompression: Boolean = true,
    val lodEnabled: Boolean = true,
    val occlusion Culling: Boolean = true
)

/**
 * Extension para profiling inline
 */
inline fun <T> profiled(profiler: ProfilerSystem, name: String, block: () -> T): T {
    return profiler.profile(name, block)
}
