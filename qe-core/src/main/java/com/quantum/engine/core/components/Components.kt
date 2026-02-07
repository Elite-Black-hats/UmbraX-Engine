package com.quantum.engine.core.components

import com.quantum.engine.core.ecs.Component
import com.quantum.engine.core.ecs.Entity
import com.quantum.engine.math.*

/**
 * TransformComponent - Componente de transformación 3D
 * 
 * Maneja posición, rotación y escala con soporte para jerarquías
 */
data class TransformComponent(
    // Transformación local (relativa al padre)
    var localPosition: Vector3 = Vector3.ZERO,
    var localRotation: Quaternion = Quaternion.IDENTITY,
    var localScale: Vector3 = Vector3.ONE,
    
    // Transformación world (calculada)
    internal var worldPosition: Vector3 = Vector3.ZERO,
    internal var worldRotation: Quaternion = Quaternion.IDENTITY,
    internal var worldScale: Vector3 = Vector3.ONE,
    
    // Matrices
    internal var localMatrix: Matrix4 = Matrix4.identity(),
    internal var worldMatrix: Matrix4 = Matrix4.identity(),
    
    // Estado
    var isDirty: Boolean = true
) : Component {
    
    // Vectores de dirección (calculados)
    val forward: Vector3 get() = worldRotation.rotate(Vector3.FORWARD)
    val up: Vector3 get() = worldRotation.rotate(Vector3.UP)
    val right: Vector3 get() = worldRotation.rotate(Vector3.RIGHT)
    
    /**
     * Actualiza las matrices de transformación
     */
    fun updateMatrix() {
        localMatrix = Matrix4.trs(localPosition, localRotation, localScale)
        isDirty = false
    }
    
    /**
     * Rota mirando hacia un punto
     */
    fun lookAt(target: Vector3, up: Vector3 = Vector3.UP) {
        val direction = (target - worldPosition).normalized
        localRotation = Quaternion.lookRotation(direction, up)
        isDirty = true
    }
    
    /**
     * Traslada en espacio local
     */
    fun translate(translation: Vector3) {
        localPosition += translation
        isDirty = true
    }
    
    /**
     * Rota en espacio local
     */
    fun rotate(eulerAngles: Vector3) {
        val rotation = Quaternion.fromEulerAngles(eulerAngles.x, eulerAngles.y, eulerAngles.z)
        localRotation *= rotation
        isDirty = true
    }
    
    /**
     * Rota alrededor de un eje
     */
    fun rotateAround(point: Vector3, axis: Vector3, angle: Float) {
        val rotation = Quaternion.fromAxisAngle(axis, angle)
        val direction = worldPosition - point
        worldPosition = point + rotation.rotate(direction)
        localRotation *= rotation
        isDirty = true
    }
    
    override fun clone(): Component = copy()
}

/**
 * CameraComponent - Componente de cámara para rendering
 */
data class CameraComponent(
    // Tipo de proyección
    var projectionType: ProjectionType = ProjectionType.PERSPECTIVE,
    
    // Perspectiva
    var fieldOfView: Float = 60f,
    var nearClipPlane: Float = 0.1f,
    var farClipPlane: Float = 1000f,
    
    // Ortográfica
    var orthographicSize: Float = 5f,
    
    // Viewport
    var viewportRect: Rect = Rect(0f, 0f, 1f, 1f),
    
    // Rendering
    var depth: Int = 0, // Orden de rendering
    var clearFlags: CameraClearFlags = CameraClearFlags.SKYBOX,
    var backgroundColor: Color = Color.BLACK,
    var cullingMask: Int = -1, // Layers a renderizar
    
    // Target
    var targetTexture: Long? = null, // RenderTexture ID
    
    // Configuración
    var allowHDR: Boolean = true,
    var allowMSAA: Boolean = true,
    
    // Matrices (calculadas)
    internal var projectionMatrix: Matrix4 = Matrix4.identity(),
    internal var viewMatrix: Matrix4 = Matrix4.identity()
) : Component {
    
    /**
     * Aspect ratio actual
     */
    var aspect: Float = 16f / 9f
        internal set
    
    /**
     * Actualiza las matrices de la cámara
     */
    fun updateMatrices(transform: TransformComponent) {
        // View matrix
        viewMatrix = Matrix4.lookAt(
            eye = transform.worldPosition,
            target = transform.worldPosition + transform.forward,
            up = transform.up
        )
        
        // Projection matrix
        projectionMatrix = when (projectionType) {
            ProjectionType.PERSPECTIVE -> {
                Matrix4.perspectiveVulkan(fieldOfView, aspect, nearClipPlane, farClipPlane)
            }
            ProjectionType.ORTHOGRAPHIC -> {
                val height = orthographicSize
                val width = height * aspect
                Matrix4.orthographic(-width, width, -height, height, nearClipPlane, farClipPlane)
            }
        }
    }
    
    /**
     * Convierte punto de mundo a viewport
     */
    fun worldToViewportPoint(worldPoint: Vector3): Vector3 {
        val clipSpace = projectionMatrix * viewMatrix * worldPoint
        return Vector3(
            (clipSpace.x + 1f) * 0.5f,
            (clipSpace.y + 1f) * 0.5f,
            clipSpace.z
        )
    }
    
    /**
     * Convierte punto de viewport a mundo (en el plano)
     */
    fun viewportToWorldPoint(viewportPoint: Vector3, distance: Float): Vector3 {
        // TODO: Implementación completa
        return Vector3.ZERO
    }
    
    /**
     * Ray desde la cámara
     */
    fun viewportPointToRay(viewportPoint: Vector2): Ray {
        // TODO: Implementación completa
        return Ray(Vector3.ZERO, Vector3.FORWARD)
    }
    
    override fun clone(): Component = copy()
}

/**
 * MeshFilterComponent - Contiene la geometría de un objeto
 */
data class MeshFilterComponent(
    var meshId: Long = 0 // Reference to mesh asset
) : Component {
    override fun clone(): Component = copy()
}

/**
 * MeshRendererComponent - Renderiza una malla con materiales
 */
data class MeshRendererComponent(
    var materialIds: List<Long> = emptyList(), // Material IDs
    var castShadows: Boolean = true,
    var receiveShadows: Boolean = true,
    var lightmapIndex: Int = -1,
    var layer: Int = 0
) : Component {
    override fun clone(): Component = copy()
}

/**
 * LightComponent - Componente de iluminación
 */
data class LightComponent(
    var type: LightType = LightType.DIRECTIONAL,
    var color: Color = Color.WHITE,
    var intensity: Float = 1f,
    var range: Float = 10f,
    var spotAngle: Float = 30f,
    var castShadows: Boolean = true,
    var shadowResolution: ShadowResolution = ShadowResolution.MEDIUM,
    var shadowBias: Float = 0.05f,
    var shadowNormalBias: Float = 0.4f,
    var layer: Int = 0
) : Component {
    override fun clone(): Component = copy()
}

/**
 * AudioSourceComponent - Fuente de audio
 */
data class AudioSourceComponent(
    var audioClipId: Long = 0,
    var volume: Float = 1f,
    var pitch: Float = 1f,
    var loop: Boolean = false,
    var playOnAwake: Boolean = false,
    var spatialBlend: Float = 1f, // 0 = 2D, 1 = 3D
    var minDistance: Float = 1f,
    var maxDistance: Float = 500f,
    var dopplerLevel: Float = 1f,
    var priority: Int = 128,
    var isPlaying: Boolean = false,
    var currentTime: Float = 0f
) : Component {
    override fun clone(): Component = copy()
}

/**
 * AudioListenerComponent - Escucha de audio (solo una por escena)
 */
data class AudioListenerComponent(
    var velocityUpdateMode: AudioVelocityUpdateMode = AudioVelocityUpdateMode.AUTO
) : Component {
    override fun clone(): Component = copy()
}

/**
 * ParticleSystemComponent - Sistema de partículas
 */
data class ParticleSystemComponent(
    var duration: Float = 5f,
    var looping: Boolean = true,
    var startLifetime: Float = 5f,
    var startSpeed: Float = 5f,
    var startSize: Float = 1f,
    var startColor: Color = Color.WHITE,
    var gravityModifier: Float = 0f,
    var simulationSpace: SimulationSpace = SimulationSpace.LOCAL,
    var maxParticles: Int = 1000,
    var emissionRate: Float = 10f,
    var isPlaying: Boolean = false
) : Component {
    override fun clone(): Component = copy()
}

// ========== Enums ==========

enum class ProjectionType {
    PERSPECTIVE,
    ORTHOGRAPHIC
}

enum class CameraClearFlags {
    SKYBOX,
    SOLID_COLOR,
    DEPTH_ONLY,
    NOTHING
}

enum class LightType {
    DIRECTIONAL,
    POINT,
    SPOT,
    AREA
}

enum class ShadowResolution {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

enum class AudioVelocityUpdateMode {
    AUTO,
    FIXED,
    DYNAMIC
}

enum class SimulationSpace {
    LOCAL,
    WORLD
}

// ========== Data Classes ==========

/**
 * Rectángulo para viewport
 */
data class Rect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * Color RGBA
 */
data class Color(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float = 1f
) {
    companion object {
        val WHITE = Color(1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f)
        val RED = Color(1f, 0f, 0f)
        val GREEN = Color(0f, 1f, 0f)
        val BLUE = Color(0f, 0f, 1f)
        val YELLOW = Color(1f, 1f, 0f)
        val CYAN = Color(0f, 1f, 1f)
        val MAGENTA = Color(1f, 0f, 1f)
        val GRAY = Color(0.5f, 0.5f, 0.5f)
        val CLEAR = Color(0f, 0f, 0f, 0f)
    }
    
    operator fun plus(other: Color) = Color(r + other.r, g + other.g, b + other.b, a + other.a)
    operator fun times(scalar: Float) = Color(r * scalar, g * scalar, b * scalar, a * scalar)
    
    fun toHex(): String {
        val ri = (r * 255).toInt().coerceIn(0, 255)
        val gi = (g * 255).toInt().coerceIn(0, 255)
        val bi = (b * 255).toInt().coerceIn(0, 255)
        val ai = (a * 255).toInt().coerceIn(0, 255)
        return "#%02X%02X%02X%02X".format(ri, gi, bi, ai)
    }
}

/**
 * Ray para raycasting
 */
data class Ray(
    val origin: Vector3,
    val direction: Vector3
) {
    fun getPoint(distance: Float): Vector3 = origin + direction * distance
}

/**
 * Bounds para culling
 */
data class Bounds(
    var center: Vector3,
    var size: Vector3
) {
    val min: Vector3 get() = center - size * 0.5f
    val max: Vector3 get() = center + size * 0.5f
    
    fun contains(point: Vector3): Boolean {
        return point.x >= min.x && point.x <= max.x &&
               point.y >= min.y && point.y <= max.y &&
               point.z >= min.z && point.z <= max.z
    }
    
    fun intersects(other: Bounds): Boolean {
        return min.x <= other.max.x && max.x >= other.min.x &&
               min.y <= other.max.y && max.y >= other.min.y &&
               min.z <= other.max.z && max.z >= other.min.z
    }
    
    fun encapsulate(point: Vector3) {
        val newMin = Vector3.min(min, point)
        val newMax = Vector3.max(max, point)
        center = (newMin + newMax) * 0.5f
        size = newMax - newMin
    }
    
    fun expand(amount: Float) {
        size += Vector3.ONE * amount
    }
}
