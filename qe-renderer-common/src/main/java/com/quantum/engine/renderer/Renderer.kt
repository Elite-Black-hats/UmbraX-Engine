package com.quantum.engine.renderer

import com.quantum.engine.math.*
import com.quantum.engine.core.components.Color

/**
 * Mesh - Geometría 3D
 */
data class Mesh(
    val vertices: FloatArray,
    val indices: IntArray,
    val normals: FloatArray? = null,
    val uvs: FloatArray? = null,
    val colors: FloatArray? = null
) {
    val vertexCount: Int get() = vertices.size / 3
    val triangleCount: Int get() = indices.size / 3
    
    companion object {
        fun createCube(size: Float = 1f): Mesh {
            val s = size / 2f
            
            val vertices = floatArrayOf(
                // Front
                -s, -s,  s,  s, -s,  s,  s,  s,  s, -s,  s,  s,
                // Back
                -s, -s, -s, -s,  s, -s,  s,  s, -s,  s, -s, -s,
                // Top
                -s,  s, -s, -s,  s,  s,  s,  s,  s,  s,  s, -s,
                // Bottom
                -s, -s, -s,  s, -s, -s,  s, -s,  s, -s, -s,  s,
                // Right
                 s, -s, -s,  s,  s, -s,  s,  s,  s,  s, -s,  s,
                // Left
                -s, -s, -s, -s, -s,  s, -s,  s,  s, -s,  s, -s
            )
            
            val indices = intArrayOf(
                0, 1, 2, 2, 3, 0,       // Front
                4, 5, 6, 6, 7, 4,       // Back
                8, 9, 10, 10, 11, 8,    // Top
                12, 13, 14, 14, 15, 12, // Bottom
                16, 17, 18, 18, 19, 16, // Right
                20, 21, 22, 22, 23, 20  // Left
            )
            
            val normals = floatArrayOf(
                // Front
                0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f,
                // Back
                0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f,
                // Top
                0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f,
                // Bottom
                0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f,
                // Right
                1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f,
                // Left
                -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f
            )
            
            return Mesh(vertices, indices, normals)
        }
        
        fun createSphere(radius: Float = 0.5f, segments: Int = 16): Mesh {
            val vertices = mutableListOf<Float>()
            val indices = mutableListOf<Int>()
            val normals = mutableListOf<Float>()
            
            for (lat in 0..segments) {
                val theta = lat * Math.PI / segments
                val sinTheta = kotlin.math.sin(theta).toFloat()
                val cosTheta = kotlin.math.cos(theta).toFloat()
                
                for (lon in 0..segments) {
                    val phi = lon * 2 * Math.PI / segments
                    val sinPhi = kotlin.math.sin(phi).toFloat()
                    val cosPhi = kotlin.math.cos(phi).toFloat()
                    
                    val x = cosPhi * sinTheta
                    val y = cosTheta
                    val z = sinPhi * sinTheta
                    
                    vertices.add(x * radius)
                    vertices.add(y * radius)
                    vertices.add(z * radius)
                    
                    normals.add(x)
                    normals.add(y)
                    normals.add(z)
                }
            }
            
            for (lat in 0 until segments) {
                for (lon in 0 until segments) {
                    val first = lat * (segments + 1) + lon
                    val second = first + segments + 1
                    
                    indices.add(first)
                    indices.add(second)
                    indices.add(first + 1)
                    
                    indices.add(second)
                    indices.add(second + 1)
                    indices.add(first + 1)
                }
            }
            
            return Mesh(
                vertices.toFloatArray(),
                indices.toIntArray(),
                normals.toFloatArray()
            )
        }
        
        fun createPlane(size: Float = 1f): Mesh {
            val s = size / 2f
            
            val vertices = floatArrayOf(
                -s, 0f, -s,
                 s, 0f, -s,
                 s, 0f,  s,
                -s, 0f,  s
            )
            
            val indices = intArrayOf(0, 1, 2, 2, 3, 0)
            
            val normals = floatArrayOf(
                0f, 1f, 0f,
                0f, 1f, 0f,
                0f, 1f, 0f,
                0f, 1f, 0f
            )
            
            return Mesh(vertices, indices, normals)
        }
    }
}

/**
 * Material - Propiedades de renderizado
 */
data class Material(
    var color: Color = Color.WHITE,
    var albedoTexture: Int = 0,
    var normalTexture: Int = 0,
    var metallicTexture: Int = 0,
    var roughnessTexture: Int = 0,
    var metallic: Float = 0f,
    var roughness: Float = 0.5f,
    var emissive: Color = Color.BLACK,
    var emissiveIntensity: Float = 0f
)

/**
 * Shader - Programa de shaders
 */
interface Shader {
    val programId: Int
    fun use()
    fun setUniform(name: String, value: Int)
    fun setUniform(name: String, value: Float)
    fun setUniform(name: String, value: Vector3)
    fun setUniform(name: String, value: Color)
    fun setUniform(name: String, value: Matrix4)
}

/**
 * RenderCommand - Comando de renderizado
 */
data class RenderCommand(
    val mesh: Mesh,
    val material: Material,
    val transform: Matrix4,
    val layer: Int = 0
)

/**
 * RenderStats - Estadísticas de renderizado
 */
data class RenderStats(
    var drawCalls: Int = 0,
    var triangles: Int = 0,
    var vertices: Int = 0,
    var frameTime: Float = 0f
) {
    fun reset() {
        drawCalls = 0
        triangles = 0
        vertices = 0
    }
}

/**
 * Renderer - Interfaz base del renderer
 */
interface Renderer {
    val stats: RenderStats
    
    fun initialize()
    fun shutdown()
    fun beginFrame()
    fun endFrame()
    fun submit(command: RenderCommand)
    fun setViewProjection(view: Matrix4, projection: Matrix4)
    fun clear(color: Color)
    fun setViewport(x: Int, y: Int, width: Int, height: Int)
}
