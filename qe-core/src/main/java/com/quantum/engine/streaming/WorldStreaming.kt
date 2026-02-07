package com.quantum.engine.streaming

import com.quantum.engine.core.ecs.*
import com.quantum.engine.math.Vector3
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * WorldStreamingSystem - Sistema de streaming para mundos abiertos masivos
 * 
 * Características:
 * - Streaming dinámico de chunks (como Minecraft, GTA)
 * - LOD automático por distancia
 * - Memory budget management
 * - Async loading sin lag
 * - Prioridad por distancia al jugador
 * - Unload automático de chunks lejanos
 */
class WorldStreamingSystem : System() {
    
    override val systemName = "WorldStreamingSystem"
    override val requiredComponents = emptyList<ComponentType>()
    override val priority = -200 // Muy alta prioridad
    
    // Configuración
    var chunkSize = 100f // Tamaño de cada chunk en metros
    var viewDistance = 500f // Distancia de visión
    var memoryBudgetMB = 512f // Budget de memoria
    var loadingThreads = 4 // Threads para carga async
    
    // Estado
    private val loadedChunks = ConcurrentHashMap<ChunkCoordinate, Chunk>()
    private val loadingQueue = mutableListOf<ChunkCoordinate>()
    private val unloadQueue = mutableListOf<ChunkCoordinate>()
    
    private var playerPosition = Vector3.ZERO
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    override fun onUpdate(entityManager: EntityManager, deltaTime: Float) {
        updatePlayerPosition(entityManager)
        updateVisibleChunks()
        processLoadingQueue()
        processUnloadQueue()
        updateLOD()
    }
    
    private fun updatePlayerPosition(entityManager: EntityManager) {
        // TODO: Encontrar entidad del jugador
        // playerPosition = playerTransform.worldPosition
    }
    
    /**
     * Determina qué chunks deben estar cargados
     */
    private fun updateVisibleChunks() {
        val currentChunk = worldToChunk(playerPosition)
        val chunksToLoad = mutableSetOf<ChunkCoordinate>()
        
        // Calcular chunks visibles en un radio
        val chunkRadius = (viewDistance / chunkSize).toInt()
        
        for (x in -chunkRadius..chunkRadius) {
            for (z in -chunkRadius..chunkRadius) {
                val chunkCoord = ChunkCoordinate(
                    currentChunk.x + x,
                    currentChunk.z + z
                )
                
                val distance = chunkDistance(currentChunk, chunkCoord)
                if (distance <= chunkRadius) {
                    chunksToLoad.add(chunkCoord)
                }
            }
        }
        
        // Marcar chunks para cargar
        chunksToLoad.forEach { coord ->
            if (!loadedChunks.containsKey(coord) && coord !in loadingQueue) {
                loadingQueue.add(coord)
            }
        }
        
        // Marcar chunks para descargar
        loadedChunks.keys.forEach { coord ->
            if (coord !in chunksToLoad && coord !in unloadQueue) {
                unloadQueue.add(coord)
            }
        }
        
        // Ordenar por prioridad (más cerca = mayor prioridad)
        loadingQueue.sortBy { chunkDistance(currentChunk, it) }
    }
    
    /**
     * Procesa cola de carga de forma asíncrona
     */
    private fun processLoadingQueue() {
        val toLoad = loadingQueue.take(loadingThreads)
        
        toLoad.forEach { coord ->
            scope.launch {
                try {
                    val chunk = loadChunk(coord)
                    loadedChunks[coord] = chunk
                    loadingQueue.remove(coord)
                } catch (e: Exception) {
                    // Log error
                    loadingQueue.remove(coord)
                }
            }
        }
    }
    
    /**
     * Procesa cola de descarga
     */
    private fun processUnloadQueue() {
        unloadQueue.forEach { coord ->
            loadedChunks.remove(coord)?.also { chunk ->
                unloadChunk(chunk)
            }
        }
        unloadQueue.clear()
    }
    
    /**
     * Actualiza LOD de los chunks basado en distancia
     */
    private fun updateLOD() {
        val currentChunk = worldToChunk(playerPosition)
        
        loadedChunks.forEach { (coord, chunk) ->
            val distance = chunkDistance(currentChunk, coord)
            
            chunk.currentLOD = when {
                distance < 2 -> LODLevel.HIGHEST
                distance < 5 -> LODLevel.HIGH
                distance < 10 -> LODLevel.MEDIUM
                distance < 15 -> LODLevel.LOW
                else -> LODLevel.LOWEST
            }
        }
    }
    
    /**
     * Carga un chunk de forma asíncrona
     */
    private suspend fun loadChunk(coord: ChunkCoordinate): Chunk = withContext(Dispatchers.IO) {
        // Simular carga (en producción: cargar de disco/red)
        delay(100)
        
        Chunk(
            coordinate = coord,
            entities = mutableListOf(),
            currentLOD = LODLevel.LOWEST
        )
    }
    
    /**
     * Descarga un chunk liberando recursos
     */
    private fun unloadChunk(chunk: Chunk) {
        // Liberar entidades
        chunk.entities.clear()
        
        // Liberar meshes, texturas, etc
        // TODO: Implementar
    }
    
    private fun worldToChunk(position: Vector3): ChunkCoordinate {
        return ChunkCoordinate(
            (position.x / chunkSize).toInt(),
            (position.z / chunkSize).toInt()
        )
    }
    
    private fun chunkDistance(a: ChunkCoordinate, b: ChunkCoordinate): Float {
        val dx = (a.x - b.x).toFloat()
        val dz = (a.z - b.z).toFloat()
        return sqrt(dx * dx + dz * dz)
    }
    
    override fun onShutdown(entityManager: EntityManager) {
        scope.cancel()
        loadedChunks.clear()
    }
}

/**
 * Coordenada de chunk en el mundo
 */
data class ChunkCoordinate(val x: Int, val z: Int)

/**
 * Chunk - Porción del mundo
 */
data class Chunk(
    val coordinate: ChunkCoordinate,
    val entities: MutableList<Entity>,
    var currentLOD: LODLevel
)

/**
 * Niveles de LOD
 */
enum class LODLevel {
    HIGHEST,  // 100% detail
    HIGH,     // 75% detail
    MEDIUM,   // 50% detail
    LOW,      // 25% detail
    LOWEST    // 10% detail
}

/**
 * LODComponent - Componente para objetos con LOD
 */
data class LODComponent(
    val levels: List<LODLevelData>,
    var currentLevel: Int = 0,
    var transitionSpeed: Float = 5f
) : Component {
    override fun clone() = copy()
}

data class LODLevelData(
    val meshId: Long,
    val minDistance: Float,
    val maxDistance: Float
)

/**
 * StreamingVolumeComponent - Define área de streaming
 */
data class StreamingVolumeComponent(
    val bounds: com.quantum.engine.core.components.Bounds,
    val priority: Int = 0,
    val alwaysLoaded: Boolean = false
) : Component {
    override fun clone() = copy()
}

/**
 * MemoryManager - Gestión de memoria para streaming
 */
class StreamingMemoryManager(
    private val budgetMB: Float
) {
    private var usedMemoryMB = 0f
    private val allocations = mutableMapOf<String, Float>()
    
    fun canAllocate(sizeMB: Float): Boolean {
        return (usedMemoryMB + sizeMB) <= budgetMB
    }
    
    fun allocate(id: String, sizeMB: Float): Boolean {
        if (!canAllocate(sizeMB)) return false
        
        allocations[id] = sizeMB
        usedMemoryMB += sizeMB
        return true
    }
    
    fun free(id: String) {
        allocations.remove(id)?.let { size ->
            usedMemoryMB -= size
        }
    }
    
    fun getUsagePercent(): Float = (usedMemoryMB / budgetMB) * 100f
    
    fun forceGC() {
        // Forzar liberación de memoria menos importante
        val sorted = allocations.entries.sortedByDescending { it.value }
        
        val target = budgetMB * 0.8f // Liberar hasta 80%
        var freed = 0f
        
        for ((id, size) in sorted) {
            if (usedMemoryMB - freed <= target) break
            
            free(id)
            freed += size
        }
        
        System.gc()
    }
}
