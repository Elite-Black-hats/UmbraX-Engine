package com.quantum.engine.math

import kotlin.math.*

/**
 * Vector3 - Vector de 3 componentes optimizado
 * 
 * Inmutable para thread-safety y optimización del compilador.
 * Operaciones inline para eliminar overhead de función.
 */
@JvmInline
value class Vector3 private constructor(
    private val data: FloatArray
) {
    constructor(x: Float, y: Float, z: Float) : this(floatArrayOf(x, y, z))
    
    val x: Float get() = data[0]
    val y: Float get() = data[1]
    val z: Float get() = data[2]
    
    // Componentes por índice
    operator fun get(index: Int): Float = data[index]
    
    /**
     * Magnitud del vector
     */
    val magnitude: Float get() = sqrt(x * x + y * y + z * z)
    
    /**
     * Magnitud al cuadrado (más rápido, evita sqrt)
     */
    val sqrMagnitude: Float get() = x * x + y * y + z * z
    
    /**
     * Vector normalizado (longitud = 1)
     */
    val normalized: Vector3 get() {
        val mag = magnitude
        return if (mag > MathUtils.EPSILON) {
            Vector3(x / mag, y / mag, z / mag)
        } else {
            ZERO
        }
    }
    
    // ========== Operadores ==========
    
    /**
     * Suma de vectores
     */
    operator fun plus(other: Vector3): Vector3 =
        Vector3(x + other.x, y + other.y, z + other.z)
    
    /**
     * Resta de vectores
     */
    operator fun minus(other: Vector3): Vector3 =
        Vector3(x - other.x, y - other.y, z - other.z)
    
    /**
     * Multiplicación por escalar
     */
    operator fun times(scalar: Float): Vector3 =
        Vector3(x * scalar, y * scalar, z * scalar)
    
    /**
     * División por escalar
     */
    operator fun div(scalar: Float): Vector3 {
        val inv = 1f / scalar
        return Vector3(x * inv, y * inv, z * inv)
    }
    
    /**
     * Negación
     */
    operator fun unaryMinus(): Vector3 =
        Vector3(-x, -y, -z)
    
    // ========== Métodos Estáticos ==========
    
    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
        val UP = Vector3(0f, 1f, 0f)
        val DOWN = Vector3(0f, -1f, 0f)
        val LEFT = Vector3(-1f, 0f, 0f)
        val RIGHT = Vector3(1f, 0f, 0f)
        val FORWARD = Vector3(0f, 0f, 1f)
        val BACK = Vector3(0f, 0f, -1f)
        
        /**
         * Producto punto (dot product)
         */
        fun dot(a: Vector3, b: Vector3): Float =
            a.x * b.x + a.y * b.y + a.z * b.z
        
        /**
         * Producto cruz (cross product)
         */
        fun cross(a: Vector3, b: Vector3): Vector3 =
            Vector3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
            )
        
        /**
         * Distancia entre dos puntos
         */
        fun distance(a: Vector3, b: Vector3): Float =
            (b - a).magnitude
        
        /**
         * Distancia al cuadrado (más rápido)
         */
        fun sqrDistance(a: Vector3, b: Vector3): Float =
            (b - a).sqrMagnitude
        
        /**
         * Interpolación lineal
         */
        fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 {
            val clamped = t.coerceIn(0f, 1f)
            return Vector3(
                MathUtils.lerp(a.x, b.x, clamped),
                MathUtils.lerp(a.y, b.y, clamped),
                MathUtils.lerp(a.z, b.z, clamped)
            )
        }
        
        /**
         * Interpolación sin clamp
         */
        fun lerpUnclamped(a: Vector3, b: Vector3, t: Float): Vector3 =
            Vector3(
                MathUtils.lerp(a.x, b.x, t),
                MathUtils.lerp(a.y, b.y, t),
                MathUtils.lerp(a.z, b.z, t)
            )
        
        /**
         * Interpolación esférica (slerp)
         */
        fun slerp(a: Vector3, b: Vector3, t: Float): Vector3 {
            val clamped = t.coerceIn(0f, 1f)
            
            val dot = dot(a.normalized, b.normalized).coerceIn(-1f, 1f)
            val theta = acos(dot) * clamped
            
            val relative = (b - a * dot).normalized
            return a * cos(theta) + relative * sin(theta)
        }
        
        /**
         * Proyección de a sobre b
         */
        fun project(a: Vector3, b: Vector3): Vector3 {
            val sqrMag = b.sqrMagnitude
            if (sqrMag < MathUtils.EPSILON) return ZERO
            
            val dotProduct = dot(a, b)
            return b * (dotProduct / sqrMag)
        }
        
        /**
         * Proyección de a en el plano perpendicular a b
         */
        fun projectOnPlane(a: Vector3, planeNormal: Vector3): Vector3 =
            a - project(a, planeNormal)
        
        /**
         * Refleja un vector respecto a una normal
         */
        fun reflect(direction: Vector3, normal: Vector3): Vector3 =
            direction - normal * (2f * dot(direction, normal))
        
        /**
         * Ángulo entre dos vectores en radianes
         */
        fun angle(a: Vector3, b: Vector3): Float {
            val denominator = sqrt(a.sqrMagnitude * b.sqrMagnitude)
            if (denominator < MathUtils.EPSILON) return 0f
            
            val dotProduct = dot(a, b).coerceIn(-1f, 1f)
            return acos(dotProduct)
        }
        
        /**
         * Ángulo signado entre dos vectores
         */
        fun signedAngle(from: Vector3, to: Vector3, axis: Vector3): Float {
            val unsignedAngle = angle(from, to)
            val sign = sign(dot(axis, cross(from, to)))
            return unsignedAngle * sign
        }
        
        /**
         * Clamp de magnitud
         */
        fun clampMagnitude(vector: Vector3, maxLength: Float): Vector3 {
            val sqrMag = vector.sqrMagnitude
            if (sqrMag > maxLength * maxLength) {
                val mag = sqrt(sqrMag)
                return vector * (maxLength / mag)
            }
            return vector
        }
        
        /**
         * Vector con componentes mínimos
         */
        fun min(a: Vector3, b: Vector3): Vector3 =
            Vector3(
                min(a.x, b.x),
                min(a.y, b.y),
                min(a.z, b.z)
            )
        
        /**
         * Vector con componentes máximos
         */
        fun max(a: Vector3, b: Vector3): Vector3 =
            Vector3(
                max(a.x, b.x),
                max(a.y, b.y),
                max(a.z, b.z)
            )
        
        /**
         * Smooth damp para movimiento suave
         */
        fun smoothDamp(
            current: Vector3,
            target: Vector3,
            currentVelocity: Vector3,
            smoothTime: Float,
            maxSpeed: Float = Float.POSITIVE_INFINITY,
            deltaTime: Float
        ): Pair<Vector3, Vector3> {
            // Implementación simplificada
            val omega = 2f / smoothTime
            val x = omega * deltaTime
            val exp = 1f / (1f + x + 0.48f * x * x + 0.235f * x * x * x)
            
            var change = current - target
            val originalTo = target
            
            val maxChange = maxSpeed * smoothTime
            change = clampMagnitude(change, maxChange)
            val target2 = current - change
            
            val temp = (currentVelocity + change * omega) * deltaTime
            var newVelocity = (currentVelocity - temp * omega) * exp
            var result = target2 + (change + temp) * exp
            
            if (dot(originalTo - current, result - originalTo) > 0) {
                result = originalTo
                newVelocity = (result - originalTo) / deltaTime
            }
            
            return Pair(result, newVelocity)
        }
    }
    
    // ========== Métodos de Instancia ==========
    
    /**
     * Normaliza este vector (retorna nuevo vector)
     */
    fun normalize(): Vector3 = normalized
    
    /**
     * Establece la magnitud del vector
     */
    fun withMagnitude(magnitude: Float): Vector3 =
        normalized * magnitude
    
    /**
     * Clamp de cada componente
     */
    fun clamp(min: Float, max: Float): Vector3 =
        Vector3(
            x.coerceIn(min, max),
            y.coerceIn(min, max),
            z.coerceIn(min, max)
        )
    
    /**
     * Valor absoluto de cada componente
     */
    fun abs(): Vector3 =
        Vector3(abs(x), abs(y), abs(z))
    
    /**
     * Copia con componentes modificados
     */
    fun copy(
        x: Float = this.x,
        y: Float = this.y,
        z: Float = this.z
    ): Vector3 = Vector3(x, y, z)
    
    override fun toString(): String = "Vector3($x, $y, $z)"
    
    /**
     * Aproximadamente igual (para comparaciones con flotantes)
     */
    fun approximately(other: Vector3, epsilon: Float = MathUtils.EPSILON): Boolean =
        abs(x - other.x) < epsilon &&
        abs(y - other.y) < epsilon &&
        abs(z - other.z) < epsilon
}

/**
 * Extensions para multiplicación escalar (Float * Vector3)
 */
operator fun Float.times(vector: Vector3): Vector3 = vector * this

/**
 * Extensions para operaciones comunes
 */
infix fun Vector3.dot(other: Vector3): Float = Vector3.dot(this, other)
infix fun Vector3.cross(other: Vector3): Vector3 = Vector3.cross(this, other)
infix fun Vector3.distanceTo(other: Vector3): Float = Vector3.distance(this, other)
