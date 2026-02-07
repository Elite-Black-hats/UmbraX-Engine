package com.quantum.engine.core.ecs

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * Interfaz base para todos los componentes del ECS
 * 
 * Los componentes son datos puros sin lógica.
 * La lógica reside en los Systems.
 */
interface Component {
    /**
     * Permite clonar componentes para prefabs
     */
    fun clone(): Component = this
}

/**
 * ID de tipo de componente único generado en compile-time
 */
@JvmInline
value class ComponentType(val id: Int) {
    companion object {
        private var nextId = 0
        private val typeMap = mutableMapOf<KClass<out Component>, ComponentType>()
        
        /**
         * Obtiene o crea el ComponentType para una clase
         */
        inline fun <reified T : Component> of(): ComponentType {
            return of(T::class)
        }
        
        fun <T : Component> of(klass: KClass<T>): ComponentType {
            return typeMap.getOrPut(klass) {
                ComponentType(nextId++)
            }
        }
        
        /**
         * Número total de tipos de componentes registrados
         */
        val count: Int get() = nextId
    }
}

/**
 * Máscara de bits para componentes (hasta 64 tipos diferentes por máscara)
 * Para más de 64 componentes, se usan múltiples máscaras
 */
data class ComponentMask(
    private val bits: LongArray = LongArray(4) // Soporta hasta 256 tipos de componentes
) {
    constructor(vararg types: ComponentType) : this() {
        types.forEach { set(it) }
    }
    
    /**
     * Activa un tipo de componente
     */
    fun set(type: ComponentType) {
        val index = type.id / 64
        val bit = type.id % 64
        if (index < bits.size) {
            bits[index] = bits[index] or (1L shl bit)
        }
    }
    
    /**
     * Desactiva un tipo de componente
     */
    fun unset(type: ComponentType) {
        val index = type.id / 64
        val bit = type.id % 64
        if (index < bits.size) {
            bits[index] = bits[index] and (1L shl bit).inv()
        }
    }
    
    /**
     * Verifica si contiene un tipo
     */
    fun has(type: ComponentType): Boolean {
        val index = type.id / 64
        val bit = type.id % 64
        return if (index < bits.size) {
            (bits[index] and (1L shl bit)) != 0L
        } else false
    }
    
    /**
     * Verifica si contiene todos los tipos
     */
    fun hasAll(mask: ComponentMask): Boolean {
        for (i in bits.indices) {
            if ((bits[i] and mask.bits[i]) != mask.bits[i]) {
                return false
            }
        }
        return true
    }
    
    /**
     * Verifica si contiene alguno de los tipos
     */
    fun hasAny(mask: ComponentMask): Boolean {
        for (i in bits.indices) {
            if ((bits[i] and mask.bits[i]) != 0L) {
                return true
            }
        }
        return false
    }
    
    /**
     * Verifica si no contiene ninguno de los tipos
     */
    fun hasNone(mask: ComponentMask): Boolean = !hasAny(mask)
    
    fun clear() {
        bits.fill(0L)
    }
    
    fun copy(): ComponentMask = ComponentMask(bits.copyOf())
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComponentMask) return false
        return bits.contentEquals(other.bits)
    }
    
    override fun hashCode(): Int = bits.contentHashCode()
}

/**
 * Archetype - Patrón de componentes compartido por múltiples entidades
 * 
 * Las entidades con los mismos componentes comparten el mismo Archetype
 * para optimizar el cache y permitir procesamiento por lotes.
 */
data class Archetype(
    val mask: ComponentMask,
    val types: List<ComponentType>
) {
    val id: Int = nextId++
    
    companion object {
        private var nextId = 0
    }
    
    /**
     * Verifica si este archetype coincide con los requisitos de un sistema
     */
    fun matches(
        required: ComponentMask,
        excluded: ComponentMask = ComponentMask()
    ): Boolean {
        return mask.hasAll(required) && mask.hasNone(excluded)
    }
}

/**
 * Pool de componentes para un tipo específico
 * Almacenamiento denso para mejor cache locality
 */
class ComponentPool<T : Component>(
    val componentType: ComponentType,
    initialCapacity: Int = 256
) {
    private val components = ArrayList<T>(initialCapacity)
    private val entityToIndex = mutableMapOf<Entity, Int>()
    private val indexToEntity = mutableMapOf<Int, Entity>()
    
    val size: Int get() = components.size
    
    /**
     * Añade un componente para una entidad
     */
    fun add(entity: Entity, component: T) {
        val index = components.size
        components.add(component)
        entityToIndex[entity] = index
        indexToEntity[index] = entity
    }
    
    /**
     * Obtiene el componente de una entidad
     */
    fun get(entity: Entity): T? {
        val index = entityToIndex[entity] ?: return null
        return components.getOrNull(index)
    }
    
    /**
     * Elimina el componente de una entidad
     */
    fun remove(entity: Entity): T? {
        val index = entityToIndex.remove(entity) ?: return null
        val component = components[index]
        
        // Swap con el último elemento para mantener densidad
        val lastIndex = components.lastIndex
        if (index != lastIndex) {
            val lastEntity = indexToEntity[lastIndex]!!
            components[index] = components[lastIndex]
            entityToIndex[lastEntity] = index
            indexToEntity[index] = lastEntity
        }
        
        components.removeAt(lastIndex)
        indexToEntity.remove(lastIndex)
        
        return component
    }
    
    /**
     * Verifica si una entidad tiene este componente
     */
    fun has(entity: Entity): Boolean = entity in entityToIndex
    
    /**
     * Itera sobre todos los componentes
     */
    inline fun forEach(action: (Entity, T) -> Unit) {
        for (i in components.indices) {
            val entity = indexToEntity[i] ?: continue
            action(entity, components[i])
        }
    }
    
    /**
     * Obtiene todos los componentes como lista
     */
    fun getAll(): List<Pair<Entity, T>> {
        return components.indices.mapNotNull { i ->
            indexToEntity[i]?.let { entity ->
                entity to components[i]
            }
        }
    }
    
    fun clear() {
        components.clear()
        entityToIndex.clear()
        indexToEntity.clear()
    }
}
