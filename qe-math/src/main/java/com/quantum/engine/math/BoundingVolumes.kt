package com.quantum.engine.math

import kotlin.math.*

/**
 * AABB - Axis-Aligned Bounding Box
 * Caja delimitadora alineada con los ejes
 */
data class AABB(
    var min: Vector3 = Vector3.ZERO,
    var max: Vector3 = Vector3.ZERO
) {
    
    /**
     * Centro del AABB
     */
    val center: Vector3 get() = (min + max) * 0.5f
    
    /**
     * Tamaño del AABB
     */
    val size: Vector3 get() = max - min
    
    /**
     * Extents (mitad del tamaño)
     */
    val extents: Vector3 get() = (max - min) * 0.5f
    
    /**
     * Volumen del AABB
     */
    val volume: Float get() {
        val s = size
        return s.x * s.y * s.z
    }
    
    /**
     * Superficie del AABB
     */
    val surfaceArea: Float get() {
        val s = size
        return 2f * (s.x * s.y + s.x * s.z + s.y * s.z)
    }
    
    constructor(center: Vector3, extents: Vector3) : this(
        center - extents,
        center + extents
    )
    
    /**
     * Comprueba si contiene un punto
     */
    fun contains(point: Vector3): Boolean {
        return point.x >= min.x && point.x <= max.x &&
               point.y >= min.y && point.y <= max.y &&
               point.z >= min.z && point.z <= max.z
    }
    
    /**
     * Comprueba si intersecta con otro AABB
     */
    fun intersects(other: AABB): Boolean {
        return !(max.x < other.min.x || min.x > other.max.x ||
                 max.y < other.min.y || min.y > other.max.y ||
                 max.z < other.min.z || min.z > other.max.z)
    }
    
    /**
     * Expande el AABB para incluir un punto
     */
    fun encapsulate(point: Vector3) {
        min = Vector3.min(min, point)
        max = Vector3.max(max, point)
    }
    
    /**
     * Expande el AABB para incluir otro AABB
     */
    fun encapsulate(other: AABB) {
        min = Vector3.min(min, other.min)
        max = Vector3.max(max, other.max)
    }
    
    /**
     * Expande el AABB por una cantidad
     */
    fun expand(amount: Float) {
        val expansion = Vector3(amount, amount, amount)
        min -= expansion
        max += expansion
    }
    
    /**
     * Punto más cercano en el AABB
     */
    fun closestPoint(point: Vector3): Vector3 {
        return Vector3(
            point.x.coerceIn(min.x, max.x),
            point.y.coerceIn(min.y, max.y),
            point.z.coerceIn(min.z, max.z)
        )
    }
    
    /**
     * Distancia al cuadrado desde un punto
     */
    fun sqrDistance(point: Vector3): Float {
        val closest = closestPoint(point)
        return Vector3.sqrDistance(point, closest)
    }
    
    /**
     * Obtiene las 8 esquinas del AABB
     */
    fun getCorners(): Array<Vector3> = arrayOf(
        Vector3(min.x, min.y, min.z),
        Vector3(min.x, min.y, max.z),
        Vector3(min.x, max.y, min.z),
        Vector3(min.x, max.y, max.z),
        Vector3(max.x, min.y, min.z),
        Vector3(max.x, min.y, max.z),
        Vector3(max.x, max.y, min.z),
        Vector3(max.x, max.y, max.z)
    )
    
    /**
     * Transforma el AABB por una matriz
     */
    fun transform(matrix: Matrix4): AABB {
        val corners = getCorners()
        val result = AABB(
            matrix.transformPoint(corners[0]),
            matrix.transformPoint(corners[0])
        )
        
        for (i in 1 until corners.size) {
            result.encapsulate(matrix.transformPoint(corners[i]))
        }
        
        return result
    }
    
    companion object {
        /**
         * Crea un AABB desde un conjunto de puntos
         */
        fun fromPoints(points: List<Vector3>): AABB {
            if (points.isEmpty()) return AABB()
            
            val aabb = AABB(points[0], points[0])
            for (i in 1 until points.size) {
                aabb.encapsulate(points[i])
            }
            return aabb
        }
        
        /**
         * Unión de dos AABBs
         */
        fun union(a: AABB, b: AABB): AABB {
            return AABB(
                Vector3.min(a.min, b.min),
                Vector3.max(a.max, b.max)
            )
        }
        
        /**
         * Intersección de dos AABBs
         */
        fun intersection(a: AABB, b: AABB): AABB? {
            val min = Vector3.max(a.min, b.min)
            val max = Vector3.min(a.max, b.max)
            
            return if (min.x <= max.x && min.y <= max.y && min.z <= max.z) {
                AABB(min, max)
            } else {
                null
            }
        }
    }
    
    override fun toString(): String = "AABB(min=$min, max=$max)"
}

/**
 * Sphere - Esfera delimitadora
 */
data class Sphere(
    var center: Vector3 = Vector3.ZERO,
    var radius: Float = 1f
) {
    
    /**
     * Volumen de la esfera
     */
    val volume: Float get() = (4f / 3f) * MathUtils.PI * radius * radius * radius
    
    /**
     * Superficie de la esfera
     */
    val surfaceArea: Float get() = 4f * MathUtils.PI * radius * radius
    
    /**
     * Comprueba si contiene un punto
     */
    fun contains(point: Vector3): Boolean {
        return Vector3.sqrDistance(center, point) <= radius * radius
    }
    
    /**
     * Comprueba si intersecta con otra esfera
     */
    fun intersects(other: Sphere): Boolean {
        val totalRadius = radius + other.radius
        return Vector3.sqrDistance(center, other.center) <= totalRadius * totalRadius
    }
    
    /**
     * Comprueba si intersecta con un AABB
     */
    fun intersects(aabb: AABB): Boolean {
        val sqrDistance = aabb.sqrDistance(center)
        return sqrDistance <= radius * radius
    }
    
    /**
     * Punto más cercano en la superficie
     */
    fun closestPoint(point: Vector3): Vector3 {
        val direction = (point - center).normalized
        return center + direction * radius
    }
    
    /**
     * Expande la esfera para incluir un punto
     */
    fun encapsulate(point: Vector3) {
        val distance = Vector3.distance(center, point)
        if (distance > radius) {
            val direction = (point - center).normalized
            val newCenter = center + direction * ((distance - radius) * 0.5f)
            center = newCenter
            radius = (distance + radius) * 0.5f
        }
    }
    
    /**
     * Transforma la esfera por una matriz
     */
    fun transform(matrix: Matrix4): Sphere {
        val newCenter = matrix.transformPoint(center)
        
        // Calcular el radio transformado
        val scale = matrix.getScale()
        val maxScale = maxOf(scale.x, scale.y, scale.z)
        
        return Sphere(newCenter, radius * maxScale)
    }
    
    companion object {
        /**
         * Crea una esfera desde un conjunto de puntos
         */
        fun fromPoints(points: List<Vector3>): Sphere {
            if (points.isEmpty()) return Sphere()
            
            // Algoritmo de Ritter's
            var minX = points[0]
            var maxX = points[0]
            var minY = points[0]
            var maxY = points[0]
            var minZ = points[0]
            var maxZ = points[0]
            
            for (p in points) {
                if (p.x < minX.x) minX = p
                if (p.x > maxX.x) maxX = p
                if (p.y < minY.y) minY = p
                if (p.y > maxY.y) maxY = p
                if (p.z < minZ.z) minZ = p
                if (p.z > maxZ.z) maxZ = p
            }
            
            val distX = Vector3.sqrDistance(minX, maxX)
            val distY = Vector3.sqrDistance(minY, maxY)
            val distZ = Vector3.sqrDistance(minZ, maxZ)
            
            val (p1, p2) = when {
                distX >= distY && distX >= distZ -> minX to maxX
                distY >= distZ -> minY to maxY
                else -> minZ to maxZ
            }
            
            val center = (p1 + p2) * 0.5f
            val radius = Vector3.distance(center, p1)
            
            val sphere = Sphere(center, radius)
            
            // Expandir para incluir todos los puntos
            for (p in points) {
                sphere.encapsulate(p)
            }
            
            return sphere
        }
    }
    
    override fun toString(): String = "Sphere(center=$center, radius=$radius)"
}

/**
 * Plane - Plano 3D
 */
data class Plane(
    var normal: Vector3 = Vector3.UP,
    var distance: Float = 0f
) {
    
    constructor(normal: Vector3, point: Vector3) : this(
        normal.normalized,
        Vector3.dot(normal.normalized, point)
    )
    
    constructor(a: Vector3, b: Vector3, c: Vector3) : this() {
        val ab = b - a
        val ac = c - a
        normal = Vector3.cross(ab, ac).normalized
        distance = Vector3.dot(normal, a)
    }
    
    /**
     * Distancia signada desde un punto al plano
     */
    fun getDistanceToPoint(point: Vector3): Float {
        return Vector3.dot(normal, point) - distance
    }
    
    /**
     * Lado del plano en el que está un punto
     * Retorna: positivo = frente, negativo = detrás, 0 = en el plano
     */
    fun getSide(point: Vector3): Float = getDistanceToPoint(point)
    
    /**
     * Comprueba si un punto está en el lado frontal del plano
     */
    fun isOnFront(point: Vector3): Boolean = getSide(point) > 0f
    
    /**
     * Punto más cercano en el plano
     */
    fun closestPoint(point: Vector3): Vector3 {
        val dist = getDistanceToPoint(point)
        return point - normal * dist
    }
    
    /**
     * Proyecta un punto sobre el plano
     */
    fun project(point: Vector3): Vector3 = closestPoint(point)
    
    /**
     * Flip del plano
     */
    fun flip() {
        normal = -normal
        distance = -distance
    }
    
    /**
     * Normaliza el plano
     */
    fun normalize() {
        val mag = normal.magnitude
        if (mag > MathUtils.EPSILON) {
            normal = normal / mag
            distance /= mag
        }
    }
    
    companion object {
        /**
         * Intersección rayo-plano
         * Retorna el parámetro t del rayo, o null si no hay intersección
         */
        fun raycast(plane: Plane, ray: Ray): Float? {
            val denominator = Vector3.dot(plane.normal, ray.direction)
            
            if (abs(denominator) < MathUtils.EPSILON) {
                return null // Paralelo
            }
            
            val t = (plane.distance - Vector3.dot(plane.normal, ray.origin)) / denominator
            return if (t >= 0f) t else null
        }
    }
    
    override fun toString(): String = "Plane(normal=$normal, distance=$distance)"
}

/**
 * Ray - Rayo 3D
 */
data class Ray(
    var origin: Vector3 = Vector3.ZERO,
    var direction: Vector3 = Vector3.FORWARD
) {
    
    init {
        direction = direction.normalized
    }
    
    /**
     * Obtiene un punto a lo largo del rayo
     */
    fun getPoint(distance: Float): Vector3 = origin + direction * distance
    
    /**
     * Intersección con esfera
     * Retorna la distancia t, o null si no hay intersección
     */
    fun intersectSphere(sphere: Sphere): Float? {
        val oc = origin - sphere.center
        val a = Vector3.dot(direction, direction)
        val b = 2f * Vector3.dot(oc, direction)
        val c = Vector3.dot(oc, oc) - sphere.radius * sphere.radius
        
        val discriminant = b * b - 4f * a * c
        
        if (discriminant < 0f) {
            return null
        }
        
        val t = (-b - sqrt(discriminant)) / (2f * a)
        return if (t >= 0f) t else null
    }
    
    /**
     * Intersección con AABB
     * Retorna la distancia t, o null si no hay intersección
     */
    fun intersectAABB(aabb: AABB): Float? {
        val invDir = Vector3(
            if (abs(direction.x) > MathUtils.EPSILON) 1f / direction.x else Float.MAX_VALUE,
            if (abs(direction.y) > MathUtils.EPSILON) 1f / direction.y else Float.MAX_VALUE,
            if (abs(direction.z) > MathUtils.EPSILON) 1f / direction.z else Float.MAX_VALUE
        )
        
        val t1 = (aabb.min.x - origin.x) * invDir.x
        val t2 = (aabb.max.x - origin.x) * invDir.x
        val t3 = (aabb.min.y - origin.y) * invDir.y
        val t4 = (aabb.max.y - origin.y) * invDir.y
        val t5 = (aabb.min.z - origin.z) * invDir.z
        val t6 = (aabb.max.z - origin.z) * invDir.z
        
        val tmin = maxOf(minOf(t1, t2), minOf(t3, t4), minOf(t5, t6))
        val tmax = minOf(maxOf(t1, t2), maxOf(t3, t4), maxOf(t5, t6))
        
        if (tmax < 0f || tmin > tmax) {
            return null
        }
        
        return if (tmin >= 0f) tmin else tmax
    }
    
    /**
     * Intersección con plano
     */
    fun intersectPlane(plane: Plane): Float? = Plane.raycast(plane, this)
    
    override fun toString(): String = "Ray(origin=$origin, direction=$direction)"
}

/**
 * Frustum - Frustum de visión (para culling)
 */
class Frustum {
    private val planes = Array(6) { Plane() }
    
    enum class PlaneIndex {
        LEFT, RIGHT, BOTTOM, TOP, NEAR, FAR
    }
    
    /**
     * Extrae los planos del frustum desde una matriz view-projection
     */
    fun extractPlanes(viewProjection: Matrix4) {
        val m = viewProjection
        
        // Left
        planes[0] = Plane(
            Vector3(m[0, 3] + m[0, 0], m[1, 3] + m[1, 0], m[2, 3] + m[2, 0]),
            m[3, 3] + m[3, 0]
        )
        
        // Right
        planes[1] = Plane(
            Vector3(m[0, 3] - m[0, 0], m[1, 3] - m[1, 0], m[2, 3] - m[2, 0]),
            m[3, 3] - m[3, 0]
        )
        
        // Bottom
        planes[2] = Plane(
            Vector3(m[0, 3] + m[0, 1], m[1, 3] + m[1, 1], m[2, 3] + m[2, 1]),
            m[3, 3] + m[3, 1]
        )
        
        // Top
        planes[3] = Plane(
            Vector3(m[0, 3] - m[0, 1], m[1, 3] - m[1, 1], m[2, 3] - m[2, 1]),
            m[3, 3] - m[3, 1]
        )
        
        // Near
        planes[4] = Plane(
            Vector3(m[0, 3] + m[0, 2], m[1, 3] + m[1, 2], m[2, 3] + m[2, 2]),
            m[3, 3] + m[3, 2]
        )
        
        // Far
        planes[5] = Plane(
            Vector3(m[0, 3] - m[0, 2], m[1, 3] - m[1, 2], m[2, 3] - m[2, 2]),
            m[3, 3] - m[3, 2]
        )
        
        // Normalizar planos
        planes.forEach { it.normalize() }
    }
    
    /**
     * Comprueba si un punto está dentro del frustum
     */
    fun containsPoint(point: Vector3): Boolean {
        return planes.all { it.getSide(point) >= 0f }
    }
    
    /**
     * Comprueba si una esfera está dentro del frustum
     */
    fun intersectsSphere(sphere: Sphere): Boolean {
        return planes.all { plane ->
            plane.getDistanceToPoint(sphere.center) >= -sphere.radius
        }
    }
    
    /**
     * Comprueba si un AABB está dentro del frustum
     */
    fun intersectsAABB(aabb: AABB): Boolean {
        for (plane in planes) {
            val center = aabb.center
            val extents = aabb.extents
            
            val r = abs(extents.x * plane.normal.x) +
                    abs(extents.y * plane.normal.y) +
                    abs(extents.z * plane.normal.z)
            
            val distance = plane.getDistanceToPoint(center)
            
            if (distance < -r) {
                return false
            }
        }
        return true
    }
}
