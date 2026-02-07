package com.quantum.engine.math

import kotlin.math.*

/**
 * MathUtils - Utilidades matemáticas generales
 */
object MathUtils {
    
    const val PI = 3.14159265359f
    const val TWO_PI = PI * 2f
    const val HALF_PI = PI * 0.5f
    const val DEG_TO_RAD = PI / 180f
    const val RAD_TO_DEG = 180f / PI
    const val EPSILON = 0.00001f
    const val FLOAT_TOLERANCE = 1e-6f
    
    /**
     * Interpolación lineal
     */
    inline fun lerp(a: Float, b: Float, t: Float): Float =
        a + (b - a) * t
    
    /**
     * Inverse lerp
     */
    inline fun inverseLerp(a: Float, b: Float, value: Float): Float =
        if (abs(b - a) < EPSILON) 0f
        else (value - a) / (b - a)
    
    /**
     * Remap de un rango a otro
     */
    inline fun remap(
        value: Float,
        from1: Float, to1: Float,
        from2: Float, to2: Float
    ): Float {
        val t = inverseLerp(from1, to1, value)
        return lerp(from2, to2, t)
    }
    
    /**
     * Smoothstep interpolation
     */
    inline fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
    
    /**
     * Smootherstep (Ken Perlin)
     */
    inline fun smootherstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * t * (t * (t * 6f - 15f) + 10f)
    }
    
    /**
     * Clamp entre min y max
     */
    inline fun clamp(value: Float, min: Float, max: Float): Float =
        value.coerceIn(min, max)
    
    /**
     * Clamp01
     */
    inline fun clamp01(value: Float): Float = value.coerceIn(0f, 1f)
    
    /**
     * Aproximadamente igual
     */
    inline fun approximately(a: Float, b: Float, epsilon: Float = EPSILON): Boolean =
        abs(a - b) < epsilon
    
    /**
     * Signo de un número
     */
    inline fun sign(value: Float): Float =
        if (value >= 0f) 1f else -1f
    
    /**
     * Ping-pong entre 0 y length
     */
    fun pingPong(t: Float, length: Float): Float {
        val t2 = t % (length * 2f)
        return if (t2 < length) t2 else (length * 2f - t2)
    }
    
    /**
     * Repeat entre 0 y length
     */
    fun repeat(t: Float, length: Float): Float {
        return t - floor(t / length) * length
    }
    
    /**
     * Delta angle entre dos ángulos
     */
    fun deltaAngle(current: Float, target: Float): Float {
        var delta = repeat(target - current, 360f)
        if (delta > 180f) delta -= 360f
        return delta
    }
    
    /**
     * Mueve current hacia target
     */
    fun moveTowards(current: Float, target: Float, maxDelta: Float): Float {
        return if (abs(target - current) <= maxDelta) {
            target
        } else {
            current + sign(target - current) * maxDelta
        }
    }
    
    /**
     * Mueve current angle hacia target angle
     */
    fun moveTowardsAngle(
        current: Float,
        target: Float,
        maxDelta: Float
    ): Float {
        val delta = deltaAngle(current, target)
        return if (-maxDelta < delta && delta < maxDelta) {
            target
        } else {
            current + delta.coerceIn(-maxDelta, maxDelta)
        }
    }
    
    /**
     * Smooth damp para float
     */
    fun smoothDamp(
        current: Float,
        target: Float,
        currentVelocity: Float,
        smoothTime: Float,
        maxSpeed: Float = Float.POSITIVE_INFINITY,
        deltaTime: Float
    ): Pair<Float, Float> {
        val smoothTime2 = max(0.0001f, smoothTime)
        val omega = 2f / smoothTime2
        val x = omega * deltaTime
        val exp = 1f / (1f + x + 0.48f * x * x + 0.235f * x * x * x)
        
        var change = current - target
        val originalTo = target
        
        val maxChange = maxSpeed * smoothTime2
        change = change.coerceIn(-maxChange, maxChange)
        val target2 = current - change
        
        val temp = (currentVelocity + omega * change) * deltaTime
        var newVelocity = (currentVelocity - omega * temp) * exp
        var result = target2 + (change + temp) * exp
        
        if ((originalTo - current > 0f) == (result > originalTo)) {
            result = originalTo
            newVelocity = (result - originalTo) / deltaTime
        }
        
        return Pair(result, newVelocity)
    }
    
    /**
     * Snap to grid
     */
    fun snap(value: Float, snapValue: Float): Float =
        round(value / snapValue) * snapValue
    
    /**
     * Normaliza un ángulo a [0, 360)
     */
    fun normalizeAngle(angle: Float): Float {
        var result = angle % 360f
        if (result < 0f) result += 360f
        return result
    }
    
    /**
     * Shortest angle difference
     */
    fun shortestAngleDifference(from: Float, to: Float): Float {
        val diff = (to - from + 180f) % 360f - 180f
        return if (diff < -180f) diff + 360f else diff
    }
    
    /**
     * Is power of two
     */
    fun isPowerOfTwo(value: Int): Boolean =
        value > 0 && (value and (value - 1)) == 0
    
    /**
     * Next power of two
     */
    fun nextPowerOfTwo(value: Int): Int {
        var v = value - 1
        v = v or (v shr 1)
        v = v or (v shr 2)
        v = v or (v shr 4)
        v = v or (v shr 8)
        v = v or (v shr 16)
        return v + 1
    }
    
    /**
     * Barycentric coordinates
     */
    fun barycentric(
        p: Vector3,
        a: Vector3,
        b: Vector3,
        c: Vector3
    ): Vector3 {
        val v0 = b - a
        val v1 = c - a
        val v2 = p - a
        
        val d00 = v0 dot v0
        val d01 = v0 dot v1
        val d11 = v1 dot v1
        val d20 = v2 dot v0
        val d21 = v2 dot v1
        
        val denom = d00 * d11 - d01 * d01
        val v = (d11 * d20 - d01 * d21) / denom
        val w = (d00 * d21 - d01 * d20) / denom
        val u = 1f - v - w
        
        return Vector3(u, v, w)
    }
}

/**
 * Extensions para Float
 */
fun Float.toDegrees(): Float = this * MathUtils.RAD_TO_DEG
fun Float.toRadians(): Float = this * MathUtils.DEG_TO_RAD
fun Float.approximately(other: Float, epsilon: Float = MathUtils.EPSILON): Boolean =
    MathUtils.approximately(this, other, epsilon)
