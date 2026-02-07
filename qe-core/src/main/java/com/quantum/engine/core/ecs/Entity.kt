package com.quantum.engine.core.ecs

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

/**
 * Entity - Identificador único en el sistema ECS
 * 
 * Un Entity es simplemente un ID único que agrupa componentes.
 * Usa generación atómica para thread-safety.
 */
@Serializable
@JvmInline
value class Entity(val id: Long) {
    
    companion object {
        private val nextId = AtomicLong(1)
        
        /**
         * Crea una nueva entidad con ID único
         */
        fun create(): Entity = Entity(nextId.getAndIncrement())
        
        /**
         * Entity nula para representar ausencia
         */
        val NULL = Entity(0)
    }
    
    /**
     * Verifica si la entidad es válida
     */
    val isValid: Boolean get() = id > 0
    
    /**
     * Versión de la entidad para detección de reutilización
     */
    val version: Int get() = (id shr 32).toInt()
    
    /**
     * Índice real de la entidad
     */
    val index: Int get() = id.toInt()
    
    override fun toString(): String = "Entity($id)"
}

/**
 * Generación de entidad con versionado para detectar entidades destruidas
 */
data class EntityGeneration(
    val entity: Entity,
    val generation: Int
) {
    fun isValid(currentGeneration: Int): Boolean = generation == currentGeneration
}

/**
 * Metadatos de una entidad
 */
data class EntityMetadata(
    val entity: Entity,
    var name: String = "Entity_${entity.id}",
    var tag: String = "",
    var layer: Int = 0,
    var active: Boolean = true,
    var parent: Entity? = null,
    val children: MutableSet<Entity> = mutableSetOf()
) {
    /**
     * Comprueba si la entidad está activa en la jerarquía
     */
    fun isActiveInHierarchy(entityManager: EntityManager): Boolean {
        if (!active) return false
        
        var current = parent
        while (current != null && current != Entity.NULL) {
            val parentMeta = entityManager.getMetadata(current) ?: return false
            if (!parentMeta.active) return false
            current = parentMeta.parent
        }
        
        return true
    }
}
