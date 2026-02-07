package com.quantum.engine.physics

import com.quantum.engine.core.ecs.Component
import com.quantum.engine.math.Vector3
import com.quantum.engine.math.Quaternion

/**
 * Rigidbody Component - Cuerpo rígido para física 3D
 */
data class RigidbodyComponent(
    // Propiedades físicas
    var mass: Float = 1f,
    var drag: Float = 0f,
    var angularDrag: Float = 0.05f,
    
    // Estado dinámico
    var velocity: Vector3 = Vector3.ZERO,
    var angularVelocity: Vector3 = Vector3.ZERO,
    
    // Fuerzas acumuladas
    var force: Vector3 = Vector3.ZERO,
    var torque: Vector3 = Vector3.ZERO,
    
    // Configuración
    var useGravity: Boolean = true,
    var isKinematic: Boolean = false,
    var freezeRotation: Boolean = false,
    
    // Constraints
    var constraints: RigidbodyConstraints = RigidbodyConstraints.NONE,
    
    // Collision
    var collisionDetection: CollisionDetectionMode = CollisionDetectionMode.DISCRETE,
    var layer: Int = 0
) : Component {
    
    val inverseMass: Float
        get() = if (isKinematic || mass <= 0f) 0f else 1f / mass
    
    /**
     * Añade fuerza al rigidbody
     */
    fun addForce(force: Vector3, mode: ForceMode = ForceMode.FORCE) {
        when (mode) {
            ForceMode.FORCE -> this.force += force
            ForceMode.IMPULSE -> velocity += force * inverseMass
            ForceMode.ACCELERATION -> velocity += force
            ForceMode.VELOCITY_CHANGE -> velocity += force
        }
    }
    
    /**
     * Añade torque (rotación)
     */
    fun addTorque(torque: Vector3) {
        this.torque += torque
    }
    
    /**
     * Limpia fuerzas acumuladas
     */
    fun clearForces() {
        force = Vector3.ZERO
        torque = Vector3.ZERO
    }
    
    override fun clone(): Component = copy()
}

/**
 * Collider Component - Base para colisionadores
 */
sealed class ColliderComponent : Component {
    abstract var isTrigger: Boolean
    abstract var center: Vector3
    abstract var material: PhysicMaterial?
}

/**
 * Box Collider - Colisionador de caja
 */
data class BoxColliderComponent(
    override var isTrigger: Boolean = false,
    override var center: Vector3 = Vector3.ZERO,
    override var material: PhysicMaterial? = null,
    var size: Vector3 = Vector3.ONE
) : ColliderComponent() {
    override fun clone(): Component = copy()
}

/**
 * Sphere Collider - Colisionador esférico
 */
data class SphereColliderComponent(
    override var isTrigger: Boolean = false,
    override var center: Vector3 = Vector3.ZERO,
    override var material: PhysicMaterial? = null,
    var radius: Float = 0.5f
) : ColliderComponent() {
    override fun clone(): Component = copy()
}

/**
 * Capsule Collider - Colisionador de cápsula
 */
data class CapsuleColliderComponent(
    override var isTrigger: Boolean = false,
    override var center: Vector3 = Vector3.ZERO,
    override var material: PhysicMaterial? = null,
    var radius: Float = 0.5f,
    var height: Float = 2f,
    var direction: CapsuleDirection = CapsuleDirection.Y_AXIS
) : ColliderComponent() {
    override fun clone(): Component = copy()
}

/**
 * Mesh Collider - Colisionador de malla
 */
data class MeshColliderComponent(
    override var isTrigger: Boolean = false,
    override var center: Vector3 = Vector3.ZERO,
    override var material: PhysicMaterial? = null,
    var convex: Boolean = false,
    var meshId: Long = 0 // Reference to mesh asset
) : ColliderComponent() {
    override fun clone(): Component = copy()
}

/**
 * Material físico - Define propiedades de fricción y rebote
 */
data class PhysicMaterial(
    var dynamicFriction: Float = 0.6f,
    var staticFriction: Float = 0.6f,
    var bounciness: Float = 0f,
    var frictionCombine: PhysicMaterialCombine = PhysicMaterialCombine.AVERAGE,
    var bounceCombine: PhysicMaterialCombine = PhysicMaterialCombine.AVERAGE
)

/**
 * Character Controller - Para control de personajes
 */
data class CharacterControllerComponent(
    var height: Float = 2f,
    var radius: Float = 0.5f,
    var center: Vector3 = Vector3.ZERO,
    var slopeLimit: Float = 45f,
    var stepOffset: Float = 0.3f,
    var skinWidth: Float = 0.08f,
    
    // Estado
    var isGrounded: Boolean = false,
    var velocity: Vector3 = Vector3.ZERO,
    var collisionFlags: CollisionFlags = CollisionFlags.NONE
) : Component {
    
    /**
     * Mueve el controller
     */
    fun move(motion: Vector3, deltaTime: Float): CollisionFlags {
        // Implementado por PhysicsSystem
        return collisionFlags
    }
    
    override fun clone(): Component = copy()
}

/**
 * Joint Components - Uniones físicas entre objetos
 */
sealed class JointComponent : Component {
    abstract var connectedBody: Long? // Entity ID
    abstract var breakForce: Float
    abstract var breakTorque: Float
}

/**
 * Fixed Joint - Une dos objetos rígidamente
 */
data class FixedJointComponent(
    override var connectedBody: Long? = null,
    override var breakForce: Float = Float.POSITIVE_INFINITY,
    override var breakTorque: Float = Float.POSITIVE_INFINITY
) : JointComponent() {
    override fun clone(): Component = copy()
}

/**
 * Hinge Joint - Bisagra
 */
data class HingeJointComponent(
    override var connectedBody: Long? = null,
    override var breakForce: Float = Float.POSITIVE_INFINITY,
    override var breakTorque: Float = Float.POSITIVE_INFINITY,
    var axis: Vector3 = Vector3.RIGHT,
    var useMotor: Boolean = false,
    var motorSpeed: Float = 0f,
    var motorForce: Float = 0f,
    var useLimits: Boolean = false,
    var minAngle: Float = -90f,
    var maxAngle: Float = 90f
) : JointComponent() {
    override fun clone(): Component = copy()
}

/**
 * Spring Joint - Resorte
 */
data class SpringJointComponent(
    override var connectedBody: Long? = null,
    override var breakForce: Float = Float.POSITIVE_INFINITY,
    override var breakTorque: Float = Float.POSITIVE_INFINITY,
    var spring: Float = 0f,
    var damper: Float = 0f,
    var minDistance: Float = 0f,
    var maxDistance: Float = 0f
) : JointComponent() {
    override fun clone(): Component = copy()
}

// ========== Enums y Configuraciones ==========

/**
 * Modo de aplicación de fuerzas
 */
enum class ForceMode {
    FORCE,              // Fuerza continua
    IMPULSE,            // Impulso instantáneo
    ACCELERATION,       // Aceleración continua
    VELOCITY_CHANGE     // Cambio de velocidad instantáneo
}

/**
 * Modo de detección de colisiones
 */
enum class CollisionDetectionMode {
    DISCRETE,           // Detección discreta (más rápida)
    CONTINUOUS,         // Detección continua (más precisa)
    CONTINUOUS_DYNAMIC  // Continua solo con objetos dinámicos
}

/**
 * Constraints de rigidbody
 */
enum class RigidbodyConstraints {
    NONE,
    FREEZE_POSITION_X,
    FREEZE_POSITION_Y,
    FREEZE_POSITION_Z,
    FREEZE_POSITION,
    FREEZE_ROTATION_X,
    FREEZE_ROTATION_Y,
    FREEZE_ROTATION_Z,
    FREEZE_ROTATION,
    FREEZE_ALL
}

/**
 * Dirección de cápsula
 */
enum class CapsuleDirection {
    X_AXIS,
    Y_AXIS,
    Z_AXIS
}

/**
 * Combinación de materiales físicos
 */
enum class PhysicMaterialCombine {
    AVERAGE,
    MINIMUM,
    MAXIMUM,
    MULTIPLY
}

/**
 * Flags de colisión para character controller
 */
enum class CollisionFlags {
    NONE,
    SIDES,
    ABOVE,
    BELOW
}

/**
 * Información de colisión
 */
data class CollisionInfo(
    val entityA: Long,
    val entityB: Long,
    val point: Vector3,
    val normal: Vector3,
    val penetration: Float,
    val relativeVelocity: Vector3
)

/**
 * Información de contacto
 */
data class ContactPoint(
    val point: Vector3,
    val normal: Vector3,
    val separation: Float
)

/**
 * Raycast Hit - Información de un raycast
 */
data class RaycastHit(
    val entity: Long?,
    val point: Vector3,
    val normal: Vector3,
    val distance: Float,
    val collider: ColliderComponent?
)

/**
 * Layer Mask - Para filtrado de colisiones
 */
@JvmInline
value class LayerMask(val mask: Int) {
    
    fun contains(layer: Int): Boolean = (mask and (1 shl layer)) != 0
    
    companion object {
        val ALL = LayerMask(-1)
        val NONE = LayerMask(0)
        
        fun create(vararg layers: Int): LayerMask {
            var mask = 0
            for (layer in layers) {
                mask = mask or (1 shl layer)
            }
            return LayerMask(mask)
        }
    }
}
