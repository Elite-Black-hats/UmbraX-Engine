package com.quantum.engine.math

import kotlin.math.*

/**
 * Matrix4 - Matriz 4x4 para transformaciones 3D
 * 
 * Representación column-major (compatible con OpenGL/Vulkan)
 * Optimizada para operaciones comunes de transformación
 */
class Matrix4 {
    
    // Almacenamiento column-major: m[column][row]
    private val m = Array(4) { FloatArray(4) }
    
    constructor() {
        setIdentity()
    }
    
    constructor(values: FloatArray) {
        require(values.size == 16) { "Matrix4 requires 16 values" }
        for (i in 0..3) {
            for (j in 0..3) {
                m[i][j] = values[i * 4 + j]
            }
        }
    }
    
    /**
     * Acceso a elementos [column, row]
     */
    operator fun get(column: Int, row: Int): Float = m[column][row]
    operator fun set(column: Int, row: Int, value: Float) {
        m[column][row] = value
    }
    
    /**
     * Establece como matriz identidad
     */
    fun setIdentity(): Matrix4 {
        for (i in 0..3) {
            for (j in 0..3) {
                m[i][j] = if (i == j) 1f else 0f
            }
        }
        return this
    }
    
    /**
     * Establece como matriz cero
     */
    fun setZero(): Matrix4 {
        for (i in 0..3) {
            for (j in 0..3) {
                m[i][j] = 0f
            }
        }
        return this
    }
    
    // ========== Operadores ==========
    
    /**
     * Multiplicación de matrices
     */
    operator fun times(other: Matrix4): Matrix4 {
        val result = Matrix4()
        for (i in 0..3) {
            for (j in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += this[k, j] * other[i, k]
                }
                result[i, j] = sum
            }
        }
        return result
    }
    
    /**
     * Multiplicación por vector (transformación)
     */
    operator fun times(vec: Vector3): Vector3 {
        val x = m[0][0] * vec.x + m[1][0] * vec.y + m[2][0] * vec.z + m[3][0]
        val y = m[0][1] * vec.x + m[1][1] * vec.y + m[2][1] * vec.z + m[3][1]
        val z = m[0][2] * vec.x + m[1][2] * vec.y + m[2][2] * vec.z + m[3][2]
        val w = m[0][3] * vec.x + m[1][3] * vec.y + m[2][3] * vec.z + m[3][3]
        
        return if (abs(w - 1f) < MathUtils.EPSILON) {
            Vector3(x, y, z)
        } else {
            Vector3(x / w, y / w, z / w)
        }
    }
    
    /**
     * Transformación de dirección (ignora translación)
     */
    fun transformDirection(vec: Vector3): Vector3 {
        val x = m[0][0] * vec.x + m[1][0] * vec.y + m[2][0] * vec.z
        val y = m[0][1] * vec.x + m[1][1] * vec.y + m[2][1] * vec.z
        val z = m[0][2] * vec.x + m[1][2] * vec.y + m[2][2] * vec.z
        return Vector3(x, y, z)
    }
    
    // ========== Transformaciones ==========
    
    /**
     * Traslación
     */
    fun translate(x: Float, y: Float, z: Float): Matrix4 {
        m[3][0] += x
        m[3][1] += y
        m[3][2] += z
        return this
    }
    
    fun translate(vec: Vector3): Matrix4 = translate(vec.x, vec.y, vec.z)
    
    /**
     * Rotación alrededor del eje X
     */
    fun rotateX(angle: Float): Matrix4 {
        val rad = angle.toRadians()
        val c = cos(rad)
        val s = sin(rad)
        
        val rot = Matrix4()
        rot[0, 0] = 1f
        rot[1, 1] = c
        rot[1, 2] = s
        rot[2, 1] = -s
        rot[2, 2] = c
        rot[3, 3] = 1f
        
        val copy = this.copy()
        setIdentity()
        return copy * rot
    }
    
    /**
     * Rotación alrededor del eje Y
     */
    fun rotateY(angle: Float): Matrix4 {
        val rad = angle.toRadians()
        val c = cos(rad)
        val s = sin(rad)
        
        val rot = Matrix4()
        rot[0, 0] = c
        rot[0, 2] = -s
        rot[1, 1] = 1f
        rot[2, 0] = s
        rot[2, 2] = c
        rot[3, 3] = 1f
        
        val copy = this.copy()
        setIdentity()
        return copy * rot
    }
    
    /**
     * Rotación alrededor del eje Z
     */
    fun rotateZ(angle: Float): Matrix4 {
        val rad = angle.toRadians()
        val c = cos(rad)
        val s = sin(rad)
        
        val rot = Matrix4()
        rot[0, 0] = c
        rot[0, 1] = s
        rot[1, 0] = -s
        rot[1, 1] = c
        rot[2, 2] = 1f
        rot[3, 3] = 1f
        
        val copy = this.copy()
        setIdentity()
        return copy * rot
    }
    
    /**
     * Rotación alrededor de un eje arbitrario
     */
    fun rotate(angle: Float, axis: Vector3): Matrix4 {
        val rad = angle.toRadians()
        val c = cos(rad)
        val s = sin(rad)
        val t = 1f - c
        
        val normalized = axis.normalized
        val x = normalized.x
        val y = normalized.y
        val z = normalized.z
        
        val rot = Matrix4()
        rot[0, 0] = t * x * x + c
        rot[0, 1] = t * x * y + s * z
        rot[0, 2] = t * x * z - s * y
        
        rot[1, 0] = t * x * y - s * z
        rot[1, 1] = t * y * y + c
        rot[1, 2] = t * y * z + s * x
        
        rot[2, 0] = t * x * z + s * y
        rot[2, 1] = t * y * z - s * x
        rot[2, 2] = t * z * z + c
        
        rot[3, 3] = 1f
        
        val copy = this.copy()
        setIdentity()
        return copy * rot
    }
    
    /**
     * Escalado
     */
    fun scale(x: Float, y: Float, z: Float): Matrix4 {
        m[0][0] *= x
        m[1][1] *= y
        m[2][2] *= z
        return this
    }
    
    fun scale(vec: Vector3): Matrix4 = scale(vec.x, vec.y, vec.z)
    fun scale(uniform: Float): Matrix4 = scale(uniform, uniform, uniform)
    
    // ========== Operaciones de Matriz ==========
    
    /**
     * Determinante
     */
    fun determinant(): Float {
        val a00 = m[0][0]; val a01 = m[0][1]; val a02 = m[0][2]; val a03 = m[0][3]
        val a10 = m[1][0]; val a11 = m[1][1]; val a12 = m[1][2]; val a13 = m[1][3]
        val a20 = m[2][0]; val a21 = m[2][1]; val a22 = m[2][2]; val a23 = m[2][3]
        val a30 = m[3][0]; val a31 = m[3][1]; val a32 = m[3][2]; val a33 = m[3][3]
        
        return (
            a00 * (a11 * (a22 * a33 - a23 * a32) - a12 * (a21 * a33 - a23 * a31) + a13 * (a21 * a32 - a22 * a31)) -
            a01 * (a10 * (a22 * a33 - a23 * a32) - a12 * (a20 * a33 - a23 * a30) + a13 * (a20 * a32 - a22 * a30)) +
            a02 * (a10 * (a21 * a33 - a23 * a31) - a11 * (a20 * a33 - a23 * a30) + a13 * (a20 * a31 - a21 * a30)) -
            a03 * (a10 * (a21 * a32 - a22 * a31) - a11 * (a20 * a32 - a22 * a30) + a12 * (a20 * a31 - a21 * a30))
        )
    }
    
    /**
     * Transpuesta
     */
    fun transpose(): Matrix4 {
        val result = Matrix4()
        for (i in 0..3) {
            for (j in 0..3) {
                result[i, j] = this[j, i]
            }
        }
        return result
    }
    
    /**
     * Extrae la posición de la matriz
     */
    fun getTranslation(): Vector3 = Vector3(m[3][0], m[3][1], m[3][2])
    
    /**
     * Extrae la escala de la matriz
     */
    fun getScale(): Vector3 {
        val scaleX = sqrt(m[0][0] * m[0][0] + m[0][1] * m[0][1] + m[0][2] * m[0][2])
        val scaleY = sqrt(m[1][0] * m[1][0] + m[1][1] * m[1][1] + m[1][2] * m[1][2])
        val scaleZ = sqrt(m[2][0] * m[2][0] + m[2][1] * m[2][1] + m[2][2] * m[2][2])
        return Vector3(scaleX, scaleY, scaleZ)
    }
    
    /**
     * Copia la matriz
     */
    fun copy(): Matrix4 {
        val result = Matrix4()
        for (i in 0..3) {
            for (j in 0..3) {
                result[i, j] = this[i, j]
            }
        }
        return result
    }
    
    /**
     * Convierte a array (column-major)
     */
    fun toArray(): FloatArray {
        val array = FloatArray(16)
        var index = 0
        for (i in 0..3) {
            for (j in 0..3) {
                array[index++] = m[i][j]
            }
        }
        return array
    }
    
    override fun toString(): String {
        return """
            Matrix4(
              [${m[0][0]}, ${m[1][0]}, ${m[2][0]}, ${m[3][0]}]
              [${m[0][1]}, ${m[1][1]}, ${m[2][1]}, ${m[3][1]}]
              [${m[0][2]}, ${m[1][2]}, ${m[2][2]}, ${m[3][2]}]
              [${m[0][3]}, ${m[1][3]}, ${m[2][3]}, ${m[3][3]}]
            )
        """.trimIndent()
    }
    
    // ========== Métodos Estáticos ==========
    
    companion object {
        
        /**
         * Matriz identidad
         */
        fun identity(): Matrix4 = Matrix4()
        
        /**
         * Matriz de translación
         */
        fun translation(x: Float, y: Float, z: Float): Matrix4 {
            val mat = identity()
            mat[3, 0] = x
            mat[3, 1] = y
            mat[3, 2] = z
            return mat
        }
        
        fun translation(vec: Vector3): Matrix4 = translation(vec.x, vec.y, vec.z)
        
        /**
         * Matriz de rotación desde quaternion
         */
        fun rotation(quat: Quaternion): Matrix4 {
            val mat = identity()
            
            val xx = quat.x * quat.x
            val yy = quat.y * quat.y
            val zz = quat.z * quat.z
            val xy = quat.x * quat.y
            val xz = quat.x * quat.z
            val yz = quat.y * quat.z
            val wx = quat.w * quat.x
            val wy = quat.w * quat.y
            val wz = quat.w * quat.z
            
            mat[0, 0] = 1f - 2f * (yy + zz)
            mat[0, 1] = 2f * (xy + wz)
            mat[0, 2] = 2f * (xz - wy)
            
            mat[1, 0] = 2f * (xy - wz)
            mat[1, 1] = 1f - 2f * (xx + zz)
            mat[1, 2] = 2f * (yz + wx)
            
            mat[2, 0] = 2f * (xz + wy)
            mat[2, 1] = 2f * (yz - wx)
            mat[2, 2] = 1f - 2f * (xx + yy)
            
            return mat
        }
        
        /**
         * Matriz de escalado
         */
        fun scaling(x: Float, y: Float, z: Float): Matrix4 {
            val mat = identity()
            mat[0, 0] = x
            mat[1, 1] = y
            mat[2, 2] = z
            return mat
        }
        
        fun scaling(vec: Vector3): Matrix4 = scaling(vec.x, vec.y, vec.z)
        fun scaling(uniform: Float): Matrix4 = scaling(uniform, uniform, uniform)
        
        /**
         * Matriz TRS (Translation, Rotation, Scale)
         */
        fun trs(
            translation: Vector3,
            rotation: Quaternion,
            scale: Vector3
        ): Matrix4 {
            return translation(translation) * rotation(rotation) * scaling(scale)
        }
        
        /**
         * Matriz de perspectiva (frustum)
         */
        fun perspective(
            fovY: Float,
            aspect: Float,
            near: Float,
            far: Float
        ): Matrix4 {
            val mat = Matrix4().setZero()
            
            val tanHalfFovy = tan(fovY.toRadians() / 2f)
            
            mat[0, 0] = 1f / (aspect * tanHalfFovy)
            mat[1, 1] = 1f / tanHalfFovy
            mat[2, 2] = -(far + near) / (far - near)
            mat[2, 3] = -1f
            mat[3, 2] = -(2f * far * near) / (far - near)
            
            return mat
        }
        
        /**
         * Matriz de perspectiva inversa (Vulkan compatible)
         */
        fun perspectiveVulkan(
            fovY: Float,
            aspect: Float,
            near: Float,
            far: Float
        ): Matrix4 {
            val mat = perspective(fovY, aspect, near, far)
            mat[1, 1] *= -1f // Flip Y para Vulkan
            return mat
        }
        
        /**
         * Matriz ortográfica
         */
        fun orthographic(
            left: Float,
            right: Float,
            bottom: Float,
            top: Float,
            near: Float,
            far: Float
        ): Matrix4 {
            val mat = identity()
            
            mat[0, 0] = 2f / (right - left)
            mat[1, 1] = 2f / (top - bottom)
            mat[2, 2] = -2f / (far - near)
            mat[3, 0] = -(right + left) / (right - left)
            mat[3, 1] = -(top + bottom) / (top - bottom)
            mat[3, 2] = -(far + near) / (far - near)
            
            return mat
        }
        
        /**
         * Matriz LookAt (View matrix)
         */
        fun lookAt(eye: Vector3, target: Vector3, up: Vector3): Matrix4 {
            val f = (target - eye).normalized
            val s = (f cross up).normalized
            val u = s cross f
            
            val mat = identity()
            
            mat[0, 0] = s.x
            mat[1, 0] = s.y
            mat[2, 0] = s.z
            
            mat[0, 1] = u.x
            mat[1, 1] = u.y
            mat[2, 1] = u.z
            
            mat[0, 2] = -f.x
            mat[1, 2] = -f.y
            mat[2, 2] = -f.z
            
            mat[3, 0] = -(s dot eye)
            mat[3, 1] = -(u dot eye)
            mat[3, 2] = f dot eye
            
            return mat
        }
    }
}
