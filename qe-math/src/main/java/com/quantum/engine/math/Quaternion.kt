package com.quantum.engine.math

import kotlin.math.*

/**
 * Quaternion - Representación de rotación 3D
 * 
 * Formato: q = w + xi + yj + zk
 * Más eficiente y estable que matrices de rotación o ángulos de Euler
 */
data class Quaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float
) {
    
    /**
     * Magnitud del quaternion
     */
    val magnitude: Float
        get() = sqrt(x * x + y * y + z * z + w * w)
    
    /**
     * Magnitud al cuadrado
     */
    val sqrMagnitude: Float
        get() = x * x + y * y + z * z + w * w
    
    /**
     * Quaternion normalizado (magnitud = 1)
     */
    val normalized: Quaternion
        get() {
            val mag = magnitude
            return if (mag > MathUtils.EPSILON) {
                Quaternion(x / mag, y / mag, z / mag, w / mag)
            } else {
                IDENTITY
            }
        }
    
    /**
     * Quaternion conjugado (inversa para quaternions unitarios)
     */
    val conjugate: Quaternion
        get() = Quaternion(-x, -y, -z, w)
    
    /**
     * Inversa del quaternion
     */
    val inverse: Quaternion
        get() {
            val sqrMag = sqrMagnitude
            if (sqrMag < MathUtils.EPSILON) return IDENTITY
            
            val invMag = 1f / sqrMag
            return Quaternion(-x * invMag, -y * invMag, -z * invMag, w * invMag)
        }
    
    // ========== Operadores ==========
    
    /**
     * Multiplicación de quaternions (composición de rotaciones)
     */
    operator fun times(other: Quaternion): Quaternion {
        return Quaternion(
            w * other.x + x * other.w + y * other.z - z * other.y,
            w * other.y - x * other.z + y * other.w + z * other.x,
            w * other.z + x * other.y - y * other.x + z * other.w,
            w * other.w - x * other.x - y * other.y - z * other.z
        )
    }
    
    /**
     * Multiplicación por escalar
     */
    operator fun times(scalar: Float): Quaternion =
        Quaternion(x * scalar, y * scalar, z * scalar, w * scalar)
    
    /**
     * División por escalar
     */
    operator fun div(scalar: Float): Quaternion {
        val inv = 1f / scalar
        return Quaternion(x * inv, y * inv, z * inv, w * inv)
    }
    
    /**
     * Suma de quaternions
     */
    operator fun plus(other: Quaternion): Quaternion =
        Quaternion(x + other.x, y + other.y, z + other.z, w + other.w)
    
    /**
     * Resta de quaternions
     */
    operator fun minus(other: Quaternion): Quaternion =
        Quaternion(x - other.x, y - other.y, z - other.z, w - other.w)
    
    /**
     * Negación
     */
    operator fun unaryMinus(): Quaternion =
        Quaternion(-x, -y, -z, -w)
    
    // ========== Rotación de Vectores ==========
    
    /**
     * Rota un vector usando este quaternion
     */
    fun rotate(vec: Vector3): Vector3 {
        // v' = q * v * q^-1
        // Optimizado: v' = v + 2 * cross(q.xyz, cross(q.xyz, v) + q.w * v)
        val qvec = Vector3(x, y, z)
        val uv = qvec cross vec
        val uuv = qvec cross uv
        
        return vec + (uv * w + uuv) * 2f
    }
    
    /**
     * Convierte a ángulos de Euler (en grados)
     * Retorna (pitch, yaw, roll)
     */
    fun toEulerAngles(): Vector3 {
        // Pitch (X-axis rotation)
        val sinp = 2f * (w * x + y * z)
        val cosp = 1f - 2f * (x * x + y * y)
        val pitch = atan2(sinp, cosp).toDegrees()
        
        // Yaw (Y-axis rotation)
        val siny = 2f * (w * y - z * x)
        val yaw = if (abs(siny) >= 1f) {
            // Gimbal lock
            (MathUtils.HALF_PI * MathUtils.sign(siny)).toDegrees()
        } else {
            asin(siny).toDegrees()
        }
        
        // Roll (Z-axis rotation)
        val sinr = 2f * (w * z + x * y)
        val cosr = 1f - 2f * (y * y + z * z)
        val roll = atan2(sinr, cosr).toDegrees()
        
        return Vector3(pitch, yaw, roll)
    }
    
    /**
     * Convierte a matriz de rotación
     */
    fun toMatrix(): Matrix4 = Matrix4.rotation(this)
    
    /**
     * Extrae el eje y ángulo de rotación
     */
    fun toAxisAngle(): Pair<Vector3, Float> {
        val angle = 2f * acos(w.coerceIn(-1f, 1f))
        val s = sqrt(1f - w * w)
        
        val axis = if (s < MathUtils.EPSILON) {
            Vector3.UP
        } else {
            Vector3(x / s, y / s, z / s)
        }
        
        return Pair(axis, angle.toDegrees())
    }
    
    /**
     * Normaliza el quaternion
     */
    fun normalize(): Quaternion = normalized
    
    /**
     * Aproximadamente igual
     */
    fun approximately(other: Quaternion, epsilon: Float = MathUtils.EPSILON): Boolean {
        // Quaternions q y -q representan la misma rotación
        val dot = abs(this dot other)
        return dot > 1f - epsilon
    }
    
    override fun toString(): String = "Quaternion($x, $y, $z, $w)"
    
    // ========== Métodos Estáticos ==========
    
    companion object {
        
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)
        
        /**
         * Producto punto entre quaternions
         */
        infix fun Quaternion.dot(other: Quaternion): Float =
            x * other.x + y * other.y + z * other.z + w * other.w
        
        /**
         * Quaternion desde ángulos de Euler (en grados)
         */
        fun fromEulerAngles(pitch: Float, yaw: Float, roll: Float): Quaternion {
            val p = pitch.toRadians() * 0.5f
            val y = yaw.toRadians() * 0.5f
            val r = roll.toRadians() * 0.5f
            
            val cp = cos(p)
            val sp = sin(p)
            val cy = cos(y)
            val sy = sin(y)
            val cr = cos(r)
            val sr = sin(r)
            
            return Quaternion(
                sr * cp * cy - cr * sp * sy,
                cr * sp * cy + sr * cp * sy,
                cr * cp * sy - sr * sp * cy,
                cr * cp * cy + sr * sp * sy
            )
        }
        
        /**
         * Quaternion desde eje y ángulo
         */
        fun fromAxisAngle(axis: Vector3, angleDegrees: Float): Quaternion {
            val rad = angleDegrees.toRadians() * 0.5f
            val s = sin(rad)
            val normalized = axis.normalized
            
            return Quaternion(
                normalized.x * s,
                normalized.y * s,
                normalized.z * s,
                cos(rad)
            )
        }
        
        /**
         * Quaternion desde matriz de rotación
         */
        fun fromRotationMatrix(
            m00: Float, m01: Float, m02: Float,
            m10: Float, m11: Float, m12: Float,
            m20: Float, m21: Float, m22: Float
        ): Quaternion {
            val trace = m00 + m11 + m22
            
            return if (trace > 0f) {
                val s = sqrt(trace + 1f) * 2f
                Quaternion(
                    (m21 - m12) / s,
                    (m02 - m20) / s,
                    (m10 - m01) / s,
                    0.25f * s
                )
            } else if (m00 > m11 && m00 > m22) {
                val s = sqrt(1f + m00 - m11 - m22) * 2f
                Quaternion(
                    0.25f * s,
                    (m01 + m10) / s,
                    (m02 + m20) / s,
                    (m21 - m12) / s
                )
            } else if (m11 > m22) {
                val s = sqrt(1f + m11 - m00 - m22) * 2f
                Quaternion(
                    (m01 + m10) / s,
                    0.25f * s,
                    (m12 + m21) / s,
                    (m02 - m20) / s
                )
            } else {
                val s = sqrt(1f + m22 - m00 - m11) * 2f
                Quaternion(
                    (m02 + m20) / s,
                    (m12 + m21) / s,
                    0.25f * s,
                    (m10 - m01) / s
                )
            }
        }
        
        /**
         * Quaternion que rota desde un vector hacia otro
         */
        fun fromToRotation(from: Vector3, to: Vector3): Quaternion {
            val fromNorm = from.normalized
            val toNorm = to.normalized
            
            val dot = fromNorm dot toNorm
            
            // Vectores paralelos
            if (dot >= 1f - MathUtils.EPSILON) {
                return IDENTITY
            }
            
            // Vectores opuestos
            if (dot <= -1f + MathUtils.EPSILON) {
                // Encontrar eje perpendicular
                val axis = if (abs(fromNorm.x) < 0.9f) {
                    Vector3.RIGHT cross fromNorm
                } else {
                    Vector3.UP cross fromNorm
                }
                return fromAxisAngle(axis.normalized, 180f)
            }
            
            // Caso general
            val axis = fromNorm cross toNorm
            val s = sqrt((1f + dot) * 2f)
            val invS = 1f / s
            
            return Quaternion(
                axis.x * invS,
                axis.y * invS,
                axis.z * invS,
                s * 0.5f
            ).normalized
        }
        
        /**
         * LookRotation - Quaternion que mira en una dirección
         */
        fun lookRotation(forward: Vector3, up: Vector3 = Vector3.UP): Quaternion {
            val forwardNorm = forward.normalized
            
            val right = (up cross forwardNorm).normalized
            val upNew = forwardNorm cross right
            
            // Construir matriz de rotación y convertir a quaternion
            val m00 = right.x
            val m01 = right.y
            val m02 = right.z
            val m10 = upNew.x
            val m11 = upNew.y
            val m12 = upNew.z
            val m20 = forwardNorm.x
            val m21 = forwardNorm.y
            val m22 = forwardNorm.z
            
            return fromRotationMatrix(
                m00, m01, m02,
                m10, m11, m12,
                m20, m21, m22
            )
        }
        
        /**
         * Interpolación lineal (no normalizada)
         */
        fun lerp(a: Quaternion, b: Quaternion, t: Float): Quaternion {
            val clamped = t.coerceIn(0f, 1f)
            
            // Asegurar camino más corto
            var bCorrected = b
            if ((a dot b) < 0f) {
                bCorrected = -b
            }
            
            return Quaternion(
                MathUtils.lerp(a.x, bCorrected.x, clamped),
                MathUtils.lerp(a.y, bCorrected.y, clamped),
                MathUtils.lerp(a.z, bCorrected.z, clamped),
                MathUtils.lerp(a.w, bCorrected.w, clamped)
            )
        }
        
        /**
         * Interpolación esférica (slerp)
         */
        fun slerp(a: Quaternion, b: Quaternion, t: Float): Quaternion {
            val clamped = t.coerceIn(0f, 1f)
            
            var cosHalfTheta = a dot b
            
            // Asegurar camino más corto
            var bCorrected = b
            if (cosHalfTheta < 0f) {
                bCorrected = -b
                cosHalfTheta = -cosHalfTheta
            }
            
            // Si son casi iguales, usar lerp
            if (cosHalfTheta >= 1f - MathUtils.EPSILON) {
                return lerp(a, bCorrected, clamped).normalized
            }
            
            val halfTheta = acos(cosHalfTheta)
            val sinHalfTheta = sqrt(1f - cosHalfTheta * cosHalfTheta)
            
            // Si son casi opuestos
            if (abs(sinHalfTheta) < MathUtils.EPSILON) {
                return Quaternion(
                    a.x * 0.5f + bCorrected.x * 0.5f,
                    a.y * 0.5f + bCorrected.y * 0.5f,
                    a.z * 0.5f + bCorrected.z * 0.5f,
                    a.w * 0.5f + bCorrected.w * 0.5f
                )
            }
            
            val ratioA = sin((1f - clamped) * halfTheta) / sinHalfTheta
            val ratioB = sin(clamped * halfTheta) / sinHalfTheta
            
            return Quaternion(
                a.x * ratioA + bCorrected.x * ratioB,
                a.y * ratioA + bCorrected.y * ratioB,
                a.z * ratioA + bCorrected.z * ratioB,
                a.w * ratioA + bCorrected.w * ratioB
            )
        }
        
        /**
         * Ángulo entre dos quaternions (en grados)
         */
        fun angle(a: Quaternion, b: Quaternion): Float {
            val dot = abs(a dot b).coerceIn(-1f, 1f)
            return (2f * acos(dot)).toDegrees()
        }
        
        /**
         * Rota un quaternion hacia otro con velocidad máxima
         */
        fun rotateTowards(
            from: Quaternion,
            to: Quaternion,
            maxDegreesDelta: Float
        ): Quaternion {
            val angle = angle(from, to)
            if (angle < MathUtils.EPSILON) return to
            
            val t = (maxDegreesDelta / angle).coerceIn(0f, 1f)
            return slerp(from, to, t)
        }
    }
}

/**
 * Extension para multiplicación por escalar
 */
operator fun Float.times(quat: Quaternion): Quaternion = quat * this
