package com.quantum.engine.math

import kotlin.math.*

/**
 * Vector2 - Vector de 2 componentes para cálculos 2D
 */
@JvmInline
value class Vector2 private constructor(
    private val data: FloatArray
) {
    constructor(x: Float, y: Float) : this(floatArrayOf(x, y))
    
    val x: Float get() = data[0]
    val y: Float get() = data[1]
    
    operator fun get(index: Int): Float = data[index]
    
    val magnitude: Float get() = sqrt(x * x + y * y)
    val sqrMagnitude: Float get() = x * x + y * y
    val normalized: Vector2 get() {
        val mag = magnitude
        return if (mag > MathUtils.EPSILON) {
            Vector2(x / mag, y / mag)
        } else {
            ZERO
        }
    }
    
    // Operadores
    operator fun plus(other: Vector2): Vector2 = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2): Vector2 = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vector2 = Vector2(x * scalar, y * scalar)
    operator fun div(scalar: Float): Vector2 {
        val inv = 1f / scalar
        return Vector2(x * inv, y * inv)
    }
    operator fun unaryMinus(): Vector2 = Vector2(-x, -y)
    
    companion object {
        val ZERO = Vector2(0f, 0f)
        val ONE = Vector2(1f, 1f)
        val UP = Vector2(0f, 1f)
        val DOWN = Vector2(0f, -1f)
        val LEFT = Vector2(-1f, 0f)
        val RIGHT = Vector2(1f, 0f)
        
        fun dot(a: Vector2, b: Vector2): Float = a.x * b.x + a.y * b.y
        
        fun distance(a: Vector2, b: Vector2): Float = (b - a).magnitude
        
        fun sqrDistance(a: Vector2, b: Vector2): Float = (b - a).sqrMagnitude
        
        fun lerp(a: Vector2, b: Vector2, t: Float): Vector2 {
            val clamped = t.coerceIn(0f, 1f)
            return Vector2(
                MathUtils.lerp(a.x, b.x, clamped),
                MathUtils.lerp(a.y, b.y, clamped)
            )
        }
        
        fun angle(from: Vector2, to: Vector2): Float {
            val denominator = sqrt(from.sqrMagnitude * to.sqrMagnitude)
            if (denominator < MathUtils.EPSILON) return 0f
            
            val dotProduct = dot(from, to).coerceIn(-1f, 1f)
            return acos(dotProduct)
        }
        
        fun signedAngle(from: Vector2, to: Vector2): Float {
            val angle = angle(from, to)
            val sign = sign(from.x * to.y - from.y * to.x)
            return angle * sign
        }
        
        fun perpendicular(v: Vector2): Vector2 = Vector2(-v.y, v.x)
        
        fun reflect(direction: Vector2, normal: Vector2): Vector2 =
            direction - normal * (2f * dot(direction, normal))
        
        fun min(a: Vector2, b: Vector2): Vector2 =
            Vector2(min(a.x, b.x), min(a.y, b.y))
        
        fun max(a: Vector2, b: Vector2): Vector2 =
            Vector2(max(a.x, b.x), max(a.y, b.y))
    }
    
    fun normalize(): Vector2 = normalized
    
    fun copy(x: Float = this.x, y: Float = this.y): Vector2 = Vector2(x, y)
    
    override fun toString(): String = "Vector2($x, $y)"
}

operator fun Float.times(vector: Vector2): Vector2 = vector * this
infix fun Vector2.dot(other: Vector2): Float = Vector2.dot(this, other)
