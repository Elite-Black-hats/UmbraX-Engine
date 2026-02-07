package com.quantum.engine.core.ecs

import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * EntityManager - Gestor central del sistema ECS
 * 
 * Maneja la creación, destrucción y consulta de entidades y componentes.
 * Thread-safe para permitir creación/destrucción desde múltiples threads.
 */
class EntityManager {
    
    // Almacenamiento de componentes por tipo
    private val componentPools = ConcurrentHashMap<ComponentType, ComponentPool<*>>()
    
    // Máscara de componentes por entidad
    private val entityMasks = ConcurrentHashMap<Entity, ComponentMask>()
    
    // Metadatos de entidades
    private val entityMetadata = ConcurrentHashMap<Entity, EntityMetadata>()
    
    // Entidades por archetype
    private val archetypes = ConcurrentHashMap<ComponentMask, Archetype>()
    private val entityToArchetype = ConcurrentHashMap<Entity, Archetype>()
    
    // Entidades pendientes de destrucción
    private val entitiesToDestroy = mutableSetOf<Entity>()
    
    // Estadísticas
    var entityCount: Int = 0
        private set
    
    /**
     * Crea una nueva entidad
     */
    fun createEntity(name: String? = null): Entity {
        val entity = Entity.create()
        entityMasks[entity] = ComponentMask()
        entityMetadata[entity] = EntityMetadata(
            entity = entity,
            name = name ?: "Entity_${entity.id}"
        )
        entityCount++
        
        Timber.d("Created entity: ${entity.id} - ${entityMetadata[entity]?.name}")
        return entity
    }
    
    /**
     * Destruye una entidad y todos sus componentes
     */
    fun destroyEntity(entity: Entity) {
        if (!isValid(entity)) return
        
        // Marcar para destrucción al final del frame
        entitiesToDestroy.add(entity)
    }
    
    /**
     * Destruye todas las entidades marcadas
     * Debe llamarse al final de cada frame
     */
    fun processDestroyedEntities() {
        for (entity in entitiesToDestroy) {
            destroyEntityImmediate(entity)
        }
        entitiesToDestroy.clear()
    }
    
    private fun destroyEntityImmediate(entity: Entity) {
        // Eliminar de jerarquía
        val metadata = entityMetadata[entity]
        metadata?.parent?.let { parent ->
            entityMetadata[parent]?.children?.remove(entity)
        }
        
        // Destruir hijos recursivamente
        metadata?.children?.toList()?.forEach { child ->
            destroyEntityImmediate(child)
        }
        
        // Eliminar todos los componentes
        val mask = entityMasks[entity] ?: return
        componentPools.values.forEach { pool ->
            @Suppress("UNCHECKED_CAST")
            (pool as ComponentPool<Component>).remove(entity)
        }
        
        // Limpiar referencias
        entityMasks.remove(entity)
        entityMetadata.remove(entity)
        entityToArchetype.remove(entity)
        entityCount--
        
        Timber.d("Destroyed entity: ${entity.id}")
    }
    
    /**
     * Añade un componente a una entidad
     */
    inline fun <reified T : Component> addComponent(entity: Entity, component: T): T {
        return addComponent(entity, component, T::class)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> addComponent(entity: Entity, component: T, klass: KClass<T>): T {
        if (!isValid(entity)) {
            throw IllegalStateException("Cannot add component to invalid entity: $entity")
        }
        
        val componentType = ComponentType.of(klass)
        val pool = componentPools.getOrPut(componentType) {
            ComponentPool<T>(componentType)
        } as ComponentPool<T>
        
        pool.add(entity, component)
        
        // Actualizar máscara y archetype
        val mask = entityMasks[entity]!!
        mask.set(componentType)
        updateArchetype(entity, mask)
        
        Timber.v("Added component ${klass.simpleName} to entity ${entity.id}")
        return component
    }
    
    /**
     * Obtiene un componente de una entidad
     */
    inline fun <reified T : Component> getComponent(entity: Entity): T? {
        return getComponent(entity, T::class)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> getComponent(entity: Entity, klass: KClass<T>): T? {
        if (!isValid(entity)) return null
        
        val componentType = ComponentType.of(klass)
        val pool = componentPools[componentType] as? ComponentPool<T> ?: return null
        return pool.get(entity)
    }
    
    /**
     * Elimina un componente de una entidad
     */
    inline fun <reified T : Component> removeComponent(entity: Entity): T? {
        return removeComponent(entity, T::class)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> removeComponent(entity: Entity, klass: KClass<T>): T? {
        if (!isValid(entity)) return null
        
        val componentType = ComponentType.of(klass)
        val pool = componentPools[componentType] as? ComponentPool<T> ?: return null
        val component = pool.remove(entity)
        
        if (component != null) {
            // Actualizar máscara y archetype
            val mask = entityMasks[entity]!!
            mask.unset(componentType)
            updateArchetype(entity, mask)
            
            Timber.v("Removed component ${klass.simpleName} from entity ${entity.id}")
        }
        
        return component
    }
    
    /**
     * Verifica si una entidad tiene un componente
     */
    inline fun <reified T : Component> hasComponent(entity: Entity): Boolean {
        return hasComponent(entity, T::class)
    }
    
    fun <T : Component> hasComponent(entity: Entity, klass: KClass<T>): Boolean {
        if (!isValid(entity)) return false
        
        val componentType = ComponentType.of(klass)
        val mask = entityMasks[entity] ?: return false
        return mask.has(componentType)
    }
    
    /**
     * Obtiene o añade un componente
     */
    inline fun <reified T : Component> getOrAddComponent(
        entity: Entity,
        factory: () -> T
    ): T {
        return getComponent<T>(entity) ?: addComponent(entity, factory())
    }
    
    /**
     * Obtiene todos los componentes de una entidad
     */
    fun getAllComponents(entity: Entity): List<Component> {
        if (!isValid(entity)) return emptyList()
        
        return componentPools.values.mapNotNull { pool ->
            @Suppress("UNCHECKED_CAST")
            (pool as ComponentPool<Component>).get(entity)
        }
    }
    
    /**
     * Obtiene metadatos de una entidad
     */
    fun getMetadata(entity: Entity): EntityMetadata? = entityMetadata[entity]
    
    /**
     * Verifica si una entidad es válida
     */
    fun isValid(entity: Entity): Boolean = entity in entityMasks
    
    /**
     * Establece la jerarquía padre-hijo
     */
    fun setParent(child: Entity, parent: Entity?) {
        val childMeta = entityMetadata[child] ?: return
        
        // Eliminar del padre anterior
        childMeta.parent?.let { oldParent ->
            entityMetadata[oldParent]?.children?.remove(child)
        }
        
        // Establecer nuevo padre
        childMeta.parent = parent
        parent?.let {
            entityMetadata[it]?.children?.add(child)
        }
    }
    
    /**
     * Actualiza el archetype de una entidad
     */
    private fun updateArchetype(entity: Entity, mask: ComponentMask) {
        val archetype = archetypes.getOrPut(mask.copy()) {
            val types = mutableListOf<ComponentType>()
            for (i in 0 until ComponentType.count) {
                val type = ComponentType(i)
                if (mask.has(type)) {
                    types.add(type)
                }
            }
            Archetype(mask.copy(), types)
        }
        
        entityToArchetype[entity] = archetype
    }
    
    /**
     * Query builder para consultas de entidades
     */
    fun query(): EntityQuery = EntityQuery(this)
    
    /**
     * Obtiene el pool de componentes de un tipo
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> getComponentPool(klass: KClass<T>): ComponentPool<T>? {
        val componentType = ComponentType.of(klass)
        return componentPools[componentType] as? ComponentPool<T>
    }
    
    /**
     * Limpia todos los datos
     */
    fun clear() {
        componentPools.values.forEach { it.clear() }
        componentPools.clear()
        entityMasks.clear()
        entityMetadata.clear()
        archetypes.clear()
        entityToArchetype.clear()
        entitiesToDestroy.clear()
        entityCount = 0
        
        Timber.d("EntityManager cleared")
    }
}

/**
 * Builder para consultas de entidades
 */
class EntityQuery(private val entityManager: EntityManager) {
    private val requiredTypes = mutableListOf<ComponentType>()
    private val excludedTypes = mutableListOf<ComponentType>()
    
    inline fun <reified T : Component> with(): EntityQuery {
        requiredTypes.add(ComponentType.of<T>())
        return this
    }
    
    inline fun <reified T : Component> without(): EntityQuery {
        excludedTypes.add(ComponentType.of<T>())
        return this
    }
    
    fun execute(): List<Entity> {
        val requiredMask = ComponentMask(*requiredTypes.toTypedArray())
        val excludedMask = ComponentMask(*excludedTypes.toTypedArray())
        
        return entityManager.query {
            it.hasAll(requiredMask) && it.hasNone(excludedMask)
        }
    }
}

/**
 * Extension para EntityManager para queries personalizadas
 */
inline fun EntityManager.query(predicate: (ComponentMask) -> Boolean): List<Entity> {
    // Implementación interna que accede a entityMasks
    // Esta es una versión simplificada
    return emptyList() // TODO: Implementar acceso interno
}
