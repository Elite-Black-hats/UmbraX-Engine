package com.quantum.engine.physics

import com.quantum.engine.core.ecs.*
import com.quantum.engine.math.Vector3
import timber.log.Timber
import kotlin.math.sqrt

/**
 * PhysicsSystem - Sistema de física 3D
 * 
 * Maneja:
 * - Integración de velocidad y posición
 * - Detección de colisiones
 * - Resolución de colisiones
 * - Gravedad
 * - Fuerzas y torques
 */
class PhysicsSystem : IteratingSystem() {
    
    override val systemName = "PhysicsSystem"
    override val requiredComponents = listOf(
        ComponentType.of<TransformComponent>(),
        ComponentType.of<RigidbodyComponent>()
    )
    override val priority = -50 // Alta prioridad, después de Transform
    
    // Configuración de física
    var gravity = Vector3(0f, -9.81f, 0f)
    var fixedTimeStep = 1f / 60f
    
    // Spatial partitioning para optimización
    private val spatialGrid = SpatialGrid()
    
    // Collision pairs para esta frame
    private val collisionPairs = mutableSetOf<Pair<Long, Long>>()
    
    override fun onInitialize(entityManager: EntityManager) {
        Timber.i("PhysicsSystem initialized - Gravity: $gravity")
    }
    
    override fun onFixedUpdate(entityManager: EntityManager, fixedDeltaTime: Float) {
        // Fase 1: Aplicar fuerzas
        entities.forEach { entity ->
            applyForces(entity, entityManager, fixedDeltaTime)
        }
        
        // Fase 2: Integración (actualizar velocidades y posiciones)
        entities.forEach { entity ->
            integrate(entity, entityManager, fixedDeltaTime)
        }
        
        // Fase 3: Detección de colisiones
        spatialGrid.clear()
        entities.forEach { entity ->
            spatialGrid.insert(entity, entityManager)
        }
        
        detectCollisions(entityManager)
        
        // Fase 4: Resolución de colisiones
        resolveCollisions(entityManager)
        
        // Fase 5: Limpiar fuerzas
        entities.forEach { entity ->
            val rb = entityManager.getComponent<RigidbodyComponent>(entity)
            rb?.clearForces()
        }
    }
    
    override fun processEntity(
        entity: Entity,
        entityManager: EntityManager,
        deltaTime: Float
    ) {
        // El procesamiento se hace en onFixedUpdate
    }
    
    /**
     * Aplica fuerzas acumuladas y gravedad
     */
    private fun applyForces(
        entity: Entity,
        entityManager: EntityManager,
        deltaTime: Float
    ) {
        val rb = entityManager.getComponent<RigidbodyComponent>(entity) ?: return
        
        if (rb.isKinematic) return
        
        // Gravedad
        if (rb.useGravity) {
            rb.addForce(gravity * rb.mass, ForceMode.FORCE)
        }
        
        // Aplicar fuerzas acumuladas
        val acceleration = rb.force * rb.inverseMass
        rb.velocity += acceleration * deltaTime
        
        // Drag (resistencia del aire)
        rb.velocity *= (1f - rb.drag * deltaTime).coerceAtLeast(0f)
        
        // Angular drag
        rb.angularVelocity *= (1f - rb.angularDrag * deltaTime).coerceAtLeast(0f)
    }
    
    /**
     * Integración de Euler semi-implícita
     */
    private fun integrate(
        entity: Entity,
        entityManager: EntityManager,
        deltaTime: Float
    ) {
        val rb = entityManager.getComponent<RigidbodyComponent>(entity) ?: return
        val transform = entityManager.getComponent<TransformComponent>(entity) ?: return
        
        if (rb.isKinematic) return
        
        // Actualizar posición
        transform.localPosition += rb.velocity * deltaTime
        
        // Actualizar rotación (simplificado)
        if (!rb.freezeRotation) {
            // TODO: Integración de rotación con quaternions
        }
        
        transform.isDirty = true
    }
    
    /**
     * Detecta colisiones entre entidades
     */
    private fun detectCollisions(entityManager: EntityManager) {
        collisionPairs.clear()
        
        entities.forEach { entityA ->
            val potentialCollisions = spatialGrid.query(entityA, entityManager)
            
            potentialCollisions.forEach { entityB ->
                if (entityA.id < entityB.id) { // Evitar duplicados
                    if (checkCollision(entityA, entityB, entityManager)) {
                        collisionPairs.add(Pair(entityA.id, entityB.id))
                    }
                }
            }
        }
    }
    
    /**
     * Verifica colisión entre dos entidades
     */
    private fun checkCollision(
        entityA: Entity,
        entityB: Entity,
        entityManager: EntityManager
    ): Boolean {
        val colliderA = getCollider(entityA, entityManager) ?: return false
        val colliderB = getCollider(entityB, entityManager) ?: return false
        
        val transformA = entityManager.getComponent<TransformComponent>(entityA) ?: return false
        val transformB = entityManager.getComponent<TransformComponent>(entityB) ?: return false
        
        // Detección por tipo de collider
        return when {
            colliderA is SphereColliderComponent && colliderB is SphereColliderComponent ->
                sphereVsSphere(colliderA, transformA, colliderB, transformB)
            
            colliderA is BoxColliderComponent && colliderB is BoxColliderComponent ->
                boxVsBox(colliderA, transformA, colliderB, transformB)
            
            colliderA is SphereColliderComponent && colliderB is BoxColliderComponent ->
                sphereVsBox(colliderA, transformA, colliderB, transformB)
            
            colliderA is BoxColliderComponent && colliderB is SphereColliderComponent ->
                sphereVsBox(colliderB, transformB, colliderA, transformA)
            
            else -> false // Otros tipos no implementados aún
        }
    }
    
    /**
     * Colisión esfera vs esfera
     */
    private fun sphereVsSphere(
        a: SphereColliderComponent,
        transformA: TransformComponent,
        b: SphereColliderComponent,
        transformB: TransformComponent
    ): Boolean {
        val centerA = transformA.localPosition + a.center
        val centerB = transformB.localPosition + b.center
        val distance = Vector3.distance(centerA, centerB)
        val radiusSum = a.radius + b.radius
        
        return distance < radiusSum
    }
    
    /**
     * Colisión caja vs caja (AABB simplificado)
     */
    private fun boxVsBox(
        a: BoxColliderComponent,
        transformA: TransformComponent,
        b: BoxColliderComponent,
        transformB: TransformComponent
    ): Boolean {
        val minA = transformA.localPosition + a.center - a.size * 0.5f
        val maxA = transformA.localPosition + a.center + a.size * 0.5f
        
        val minB = transformB.localPosition + b.center - b.size * 0.5f
        val maxB = transformB.localPosition + b.center + b.size * 0.5f
        
        return (minA.x <= maxB.x && maxA.x >= minB.x) &&
               (minA.y <= maxB.y && maxA.y >= minB.y) &&
               (minA.z <= maxB.z && maxA.z >= minB.z)
    }
    
    /**
     * Colisión esfera vs caja
     */
    private fun sphereVsBox(
        sphere: SphereColliderComponent,
        sphereTransform: TransformComponent,
        box: BoxColliderComponent,
        boxTransform: TransformComponent
    ): Boolean {
        val sphereCenter = sphereTransform.localPosition + sphere.center
        val boxCenter = boxTransform.localPosition + box.center
        val halfSize = box.size * 0.5f
        
        // Punto más cercano en la caja
        val closestPoint = Vector3(
            sphereCenter.x.coerceIn(boxCenter.x - halfSize.x, boxCenter.x + halfSize.x),
            sphereCenter.y.coerceIn(boxCenter.y - halfSize.y, boxCenter.y + halfSize.y),
            sphereCenter.z.coerceIn(boxCenter.z - halfSize.z, boxCenter.z + halfSize.z)
        )
        
        val distance = Vector3.distance(sphereCenter, closestPoint)
        return distance < sphere.radius
    }
    
    /**
     * Resuelve colisiones aplicando impulsos
     */
    private fun resolveCollisions(entityManager: EntityManager) {
        collisionPairs.forEach { (idA, idB) ->
            val entityA = Entity(idA)
            val entityB = Entity(idB)
            
            val rbA = entityManager.getComponent<RigidbodyComponent>(entityA)
            val rbB = entityManager.getComponent<RigidbodyComponent>(entityB)
            
            if (rbA == null || rbB == null) return@forEach
            if (rbA.isKinematic && rbB.isKinematic) return@forEach
            
            // Calcular normal y penetración (simplificado)
            val transformA = entityManager.getComponent<TransformComponent>(entityA) ?: return@forEach
            val transformB = entityManager.getComponent<TransformComponent>(entityB) ?: return@forEach
            
            val normal = (transformB.localPosition - transformA.localPosition).normalized
            
            // Velocidad relativa
            val relativeVelocity = rbB.velocity - rbA.velocity
            val velocityAlongNormal = relativeVelocity dot normal
            
            // No resolver si se están separando
            if (velocityAlongNormal > 0) return@forEach
            
            // Coeficiente de restitución (bounciness)
            val restitution = 0.5f // TODO: Obtener del material
            
            // Calcular impulso
            val invMassSum = rbA.inverseMass + rbB.inverseMass
            if (invMassSum < 0.0001f) return@forEach
            
            val j = -(1 + restitution) * velocityAlongNormal / invMassSum
            val impulse = normal * j
            
            // Aplicar impulso
            if (!rbA.isKinematic) {
                rbA.velocity -= impulse * rbA.inverseMass
            }
            if (!rbB.isKinematic) {
                rbB.velocity += impulse * rbB.inverseMass
            }
        }
    }
    
    /**
     * Obtiene el collider de una entidad
     */
    private fun getCollider(entity: Entity, entityManager: EntityManager): ColliderComponent? {
        return entityManager.getComponent<SphereColliderComponent>(entity)
            ?: entityManager.getComponent<BoxColliderComponent>(entity)
            ?: entityManager.getComponent<CapsuleColliderComponent>(entity)
    }
    
    /**
     * Realiza un raycast en el mundo físico
     */
    fun raycast(
        origin: Vector3,
        direction: Vector3,
        maxDistance: Float = Float.POSITIVE_INFINITY,
        layerMask: LayerMask = LayerMask.ALL,
        entityManager: EntityManager
    ): RaycastHit? {
        var closestHit: RaycastHit? = null
        var closestDistance = maxDistance
        
        entities.forEach { entity ->
            val rb = entityManager.getComponent<RigidbodyComponent>(entity) ?: return@forEach
            if (!layerMask.contains(rb.layer)) return@forEach
            
            val collider = getCollider(entity, entityManager) ?: return@forEach
            val transform = entityManager.getComponent<TransformComponent>(entity) ?: return@forEach
            
            // Raycast por tipo de collider
            val hit = when (collider) {
                is SphereColliderComponent -> {
                    raycastSphere(origin, direction, collider, transform)
                }
                is BoxColliderComponent -> {
                    raycastBox(origin, direction, collider, transform)
                }
                else -> null
            }
            
            if (hit != null && hit.distance < closestDistance) {
                closestDistance = hit.distance
                closestHit = hit.copy(
                    entity = entity.id,
                    collider = collider
                )
            }
        }
        
        return closestHit
    }
    
    /**
     * Raycast contra esfera
     */
    private fun raycastSphere(
        origin: Vector3,
        direction: Vector3,
        sphere: SphereColliderComponent,
        transform: TransformComponent
    ): RaycastHit? {
        val center = transform.localPosition + sphere.center
        val oc = origin - center
        
        val a = direction dot direction
        val b = 2f * (oc dot direction)
        val c = (oc dot oc) - sphere.radius * sphere.radius
        
        val discriminant = b * b - 4 * a * c
        if (discriminant < 0) return null
        
        val t = (-b - sqrt(discriminant)) / (2f * a)
        if (t < 0) return null
        
        val point = origin + direction * t
        val normal = (point - center).normalized
        
        return RaycastHit(
            entity = null,
            point = point,
            normal = normal,
            distance = t,
            collider = null
        )
    }
    
    /**
     * Raycast contra caja (simplificado)
     */
    private fun raycastBox(
        origin: Vector3,
        direction: Vector3,
        box: BoxColliderComponent,
        transform: TransformComponent
    ): RaycastHit? {
        // Implementación simplificada AABB
        // TODO: Implementación completa
        return null
    }
}

/**
 * Spatial Grid para optimización de detección de colisiones
 * Divide el espacio en celdas para reducir chequeos O(n²) a O(n)
 */
private class SpatialGrid(
    val cellSize: Float = 10f
) {
    private val grid = mutableMapOf<GridCell, MutableList<Entity>>()
    
    data class GridCell(val x: Int, val y: Int, val z: Int)
    
    fun clear() {
        grid.clear()
    }
    
    fun insert(entity: Entity, entityManager: EntityManager) {
        val transform = entityManager.getComponent<TransformComponent>(entity) ?: return
        val cell = getCell(transform.localPosition)
        
        grid.getOrPut(cell) { mutableListOf() }.add(entity)
    }
    
    fun query(entity: Entity, entityManager: EntityManager): List<Entity> {
        val transform = entityManager.getComponent<TransformComponent>(entity) ?: return emptyList()
        val cell = getCell(transform.localPosition)
        
        val result = mutableListOf<Entity>()
        
        // Buscar en celda actual y vecinas
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    val neighborCell = GridCell(cell.x + dx, cell.y + dy, cell.z + dz)
                    grid[neighborCell]?.let { entities ->
                        result.addAll(entities.filter { it.id != entity.id })
                    }
                }
            }
        }
        
        return result
    }
    
    private fun getCell(position: Vector3): GridCell {
        return GridCell(
            (position.x / cellSize).toInt(),
            (position.y / cellSize).toInt(),
            (position.z / cellSize).toInt()
        )
    }
}
