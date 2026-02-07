package com.quantum.engine.core.ecs

import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * System - Lógica de procesamiento del ECS
 * 
 * Los Systems procesan entidades con componentes específicos.
 * Pueden ejecutarse en paralelo o secuencialmente.
 */
abstract class System {
    
    var enabled: Boolean = true
    var priority: Int = 0 // Menor número = mayor prioridad
    
    /**
     * Nombre del sistema para debugging
     */
    open val systemName: String = this::class.simpleName ?: "Unknown"
    
    /**
     * Define qué componentes requiere este sistema
     */
    abstract val requiredComponents: List<ComponentType>
    
    /**
     * Componentes que excluyen entidades de este sistema
     */
    open val excludedComponents: List<ComponentType> = emptyList()
    
    /**
     * ¿Puede ejecutarse en paralelo?
     */
    open val parallelizable: Boolean = false
    
    /**
     * Inicialización del sistema
     */
    open fun onInitialize(entityManager: EntityManager) {}
    
    /**
     * Actualización del sistema (llamado cada frame)
     */
    abstract fun onUpdate(
        entityManager: EntityManager,
        deltaTime: Float
    )
    
    /**
     * Actualización fija (para física, etc)
     */
    open fun onFixedUpdate(
        entityManager: EntityManager,
        fixedDeltaTime: Float
    ) {}
    
    /**
     * Limpieza del sistema
     */
    open fun onShutdown(entityManager: EntityManager) {}
    
    /**
     * Llamado cuando una entidad cumple los requisitos
     */
    open fun onEntityAdded(entity: Entity, entityManager: EntityManager) {}
    
    /**
     * Llamado cuando una entidad deja de cumplir los requisitos
     */
    open fun onEntityRemoved(entity: Entity, entityManager: EntityManager) {}
}

/**
 * Sistema que procesa entidades de forma iterativa
 */
abstract class IteratingSystem : System() {
    
    // Cache de entidades que coinciden con este sistema
    protected val entities = mutableSetOf<Entity>()
    
    override fun onUpdate(entityManager: EntityManager, deltaTime: Float) {
        if (!enabled) return
        
        // Actualizar lista de entidades
        updateEntityList(entityManager)
        
        // Procesar cada entidad
        if (parallelizable && entities.size > 100) {
            // Procesamiento paralelo para muchas entidades
            processParallel(entityManager, deltaTime)
        } else {
            // Procesamiento secuencial
            processSequential(entityManager, deltaTime)
        }
    }
    
    private fun processSequential(entityManager: EntityManager, deltaTime: Float) {
        entities.forEach { entity ->
            if (entityManager.isValid(entity)) {
                processEntity(entity, entityManager, deltaTime)
            }
        }
    }
    
    private fun processParallel(entityManager: EntityManager, deltaTime: Float) {
        runBlocking {
            entities.map { entity ->
                async(Dispatchers.Default) {
                    if (entityManager.isValid(entity)) {
                        processEntity(entity, entityManager, deltaTime)
                    }
                }
            }.awaitAll()
        }
    }
    
    /**
     * Procesa una entidad individual
     */
    protected abstract fun processEntity(
        entity: Entity,
        entityManager: EntityManager,
        deltaTime: Float
    )
    
    private fun updateEntityList(entityManager: EntityManager) {
        // TODO: Implementar actualización eficiente basada en archetypes
        // Por ahora es una implementación básica
    }
    
    override fun onEntityAdded(entity: Entity, entityManager: EntityManager) {
        entities.add(entity)
    }
    
    override fun onEntityRemoved(entity: Entity, entityManager: EntityManager) {
        entities.remove(entity)
    }
}

/**
 * SystemManager - Gestiona todos los sistemas del motor
 */
class SystemManager(private val entityManager: EntityManager) {
    
    private val systems = mutableListOf<System>()
    private val systemsByType = mutableMapOf<Class<*>, System>()
    
    // Grupos de sistemas
    private val updateSystems = mutableListOf<System>()
    private val fixedUpdateSystems = mutableListOf<System>()
    
    // Métricas de performance
    private val systemTimings = mutableMapOf<String, Float>()
    
    /**
     * Registra un nuevo sistema
     */
    fun <T : System> registerSystem(system: T): T {
        systems.add(system)
        systemsByType[system::class.java] = system
        
        // Clasificar por tipo de update
        if (system::class.java.declaredMethods.any { 
            it.name == "onUpdate" && !it.isSynthetic 
        }) {
            updateSystems.add(system)
        }
        
        if (system::class.java.declaredMethods.any { 
            it.name == "onFixedUpdate" && !it.isSynthetic 
        }) {
            fixedUpdateSystems.add(system)
        }
        
        // Ordenar por prioridad
        updateSystems.sortBy { it.priority }
        fixedUpdateSystems.sortBy { it.priority }
        
        // Inicializar
        system.onInitialize(entityManager)
        
        Timber.d("Registered system: ${system.systemName}")
        return system
    }
    
    /**
     * Obtiene un sistema por tipo
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : System> getSystem(systemClass: Class<T>): T? {
        return systemsByType[systemClass] as? T
    }
    
    inline fun <reified T : System> getSystem(): T? {
        return getSystem(T::class.java)
    }
    
    /**
     * Elimina un sistema
     */
    fun removeSystem(system: System) {
        system.onShutdown(entityManager)
        systems.remove(system)
        systemsByType.remove(system::class.java)
        updateSystems.remove(system)
        fixedUpdateSystems.remove(system)
        
        Timber.d("Removed system: ${system.systemName}")
    }
    
    /**
     * Actualiza todos los sistemas (variable timestep)
     */
    fun update(deltaTime: Float) {
        for (system in updateSystems) {
            if (!system.enabled) continue
            
            val time = measureTimeMillis {
                try {
                    system.onUpdate(entityManager, deltaTime)
                } catch (e: Exception) {
                    Timber.e(e, "Error updating system: ${system.systemName}")
                }
            }
            
            systemTimings[system.systemName] = time / 1000f
        }
    }
    
    /**
     * Actualiza sistemas de física (fixed timestep)
     */
    fun fixedUpdate(fixedDeltaTime: Float) {
        for (system in fixedUpdateSystems) {
            if (!system.enabled) continue
            
            try {
                system.onFixedUpdate(entityManager, fixedDeltaTime)
            } catch (e: Exception) {
                Timber.e(e, "Error in fixed update for system: ${system.systemName}")
            }
        }
    }
    
    /**
     * Notifica a los sistemas sobre cambios de entidades
     */
    fun notifyEntityComponentsChanged(entity: Entity) {
        for (system in systems) {
            val matches = checkEntityMatchesSystem(entity, system)
            
            if (matches && system is IteratingSystem && entity !in system.entities) {
                system.onEntityAdded(entity, entityManager)
            } else if (!matches && system is IteratingSystem && entity in system.entities) {
                system.onEntityRemoved(entity, entityManager)
            }
        }
    }
    
    private fun checkEntityMatchesSystem(entity: Entity, system: System): Boolean {
        // Verificar componentes requeridos
        for (componentType in system.requiredComponents) {
            // TODO: Implementar verificación real
        }
        
        // Verificar componentes excluidos
        for (componentType in system.excludedComponents) {
            // TODO: Implementar verificación real
        }
        
        return true // Placeholder
    }
    
    /**
     * Obtiene las métricas de tiempo de cada sistema
     */
    fun getSystemTimings(): Map<String, Float> = systemTimings.toMap()
    
    /**
     * Limpia todos los sistemas
     */
    fun shutdown() {
        for (system in systems) {
            system.onShutdown(entityManager)
        }
        systems.clear()
        systemsByType.clear()
        updateSystems.clear()
        fixedUpdateSystems.clear()
        systemTimings.clear()
        
        Timber.d("SystemManager shutdown")
    }
}

/**
 * Ejemplos de sistemas básicos
 */

/**
 * Sistema de transformación jerárquica
 */
class TransformSystem : IteratingSystem() {
    
    override val systemName = "TransformSystem"
    override val requiredComponents = listOf(ComponentType.of<com.quantum.engine.core.components.TransformComponent>())
    override val priority = -100 // Alta prioridad
    
    override fun processEntity(
        entity: Entity,
        entityManager: EntityManager,
        deltaTime: Float
    ) {
        val transform = entityManager.getComponent<com.quantum.engine.core.components.TransformComponent>(entity) ?: return
        
        // Actualizar matriz world si el parent cambió
        val metadata = entityManager.getMetadata(entity)
        if (metadata?.parent != null && metadata.parent != Entity.NULL) {
            val parentTransform = entityManager.getComponent<com.quantum.engine.core.components.TransformComponent>(metadata.parent!!)
            if (parentTransform != null) {
                // Calcular transformación jerárquica
                transform.worldPosition = parentTransform.worldPosition + transform.localPosition
                transform.worldRotation = parentTransform.worldRotation * transform.localRotation
                transform.worldScale = com.quantum.engine.math.Vector3(
                    parentTransform.worldScale.x * transform.localScale.x,
                    parentTransform.worldScale.y * transform.localScale.y,
                    parentTransform.worldScale.z * transform.localScale.z
                )
                transform.isDirty = true
            }
        } else {
            // Sin parent, world = local
            transform.worldPosition = transform.localPosition
            transform.worldRotation = transform.localRotation
            transform.worldScale = transform.localScale
        }
        
        if (transform.isDirty) {
            transform.updateMatrix()
        }
    }
}
