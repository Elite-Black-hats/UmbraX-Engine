package com.quantum.engine.renderer.gles

import android.opengl.GLES30.*
import com.quantum.engine.renderer.*
import com.quantum.engine.math.*
import com.quantum.engine.core.components.Color
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * GLESRenderer - Renderer OpenGL ES 3.0
 */
class GLESRenderer : Renderer {
    
    override val stats = RenderStats()
    
    private var defaultShader: GLESShader? = null
    private val meshCache = mutableMapOf<Mesh, GLESMesh>()
    private val commands = mutableListOf<RenderCommand>()
    
    private var viewMatrix = Matrix4.identity()
    private var projectionMatrix = Matrix4.identity()
    
    override fun initialize() {
        Timber.i("Initializing GLES Renderer")
        
        // Configuración de OpenGL
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LESS)
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CCW)
        
        // Crear shader por defecto
        defaultShader = GLESShader(
            vertexSource = DEFAULT_VERTEX_SHADER,
            fragmentSource = DEFAULT_FRAGMENT_SHADER
        )
        
        Timber.i("GLES Renderer initialized")
    }
    
    override fun shutdown() {
        meshCache.values.forEach { it.destroy() }
        meshCache.clear()
        defaultShader?.destroy()
        commands.clear()
    }
    
    override fun beginFrame() {
        stats.reset()
        commands.clear()
    }
    
    override fun endFrame() {
        // Renderizar todos los comandos
        commands.forEach { command ->
            renderCommand(command)
        }
    }
    
    override fun submit(command: RenderCommand) {
        commands.add(command)
    }
    
    override fun setViewProjection(view: Matrix4, projection: Matrix4) {
        viewMatrix = view
        projectionMatrix = projection
    }
    
    override fun clear(color: Color) {
        glClearColor(color.r, color.g, color.b, color.a)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }
    
    override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        glViewport(x, y, width, height)
    }
    
    private fun renderCommand(command: RenderCommand) {
        val shader = defaultShader ?: return
        
        // Obtener o crear mesh en GPU
        val glesMesh = meshCache.getOrPut(command.mesh) {
            GLESMesh(command.mesh)
        }
        
        // Usar shader
        shader.use()
        
        // Calcular MVP
        val mvp = projectionMatrix * viewMatrix * command.transform
        
        // Setear uniforms
        shader.setUniform("u_MVP", mvp)
        shader.setUniform("u_Model", command.transform)
        shader.setUniform("u_Color", command.material.color)
        
        // Renderizar
        glesMesh.render()
        
        // Actualizar stats
        stats.drawCalls++
        stats.triangles += command.mesh.triangleCount
        stats.vertices += command.mesh.vertexCount
    }
    
    companion object {
        private const val DEFAULT_VERTEX_SHADER = """
            #version 300 es
            precision highp float;
            
            layout(location = 0) in vec3 a_Position;
            layout(location = 1) in vec3 a_Normal;
            
            uniform mat4 u_MVP;
            uniform mat4 u_Model;
            
            out vec3 v_Normal;
            out vec3 v_FragPos;
            
            void main() {
                gl_Position = u_MVP * vec4(a_Position, 1.0);
                v_FragPos = vec3(u_Model * vec4(a_Position, 1.0));
                v_Normal = mat3(u_Model) * a_Normal;
            }
        """
        
        private const val DEFAULT_FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;
            
            in vec3 v_Normal;
            in vec3 v_FragPos;
            
            uniform vec4 u_Color;
            
            out vec4 FragColor;
            
            void main() {
                vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0));
                vec3 normal = normalize(v_Normal);
                
                float diff = max(dot(normal, lightDir), 0.0);
                vec3 diffuse = diff * vec3(1.0);
                
                vec3 ambient = vec3(0.3);
                
                vec3 result = (ambient + diffuse) * u_Color.rgb;
                FragColor = vec4(result, u_Color.a);
            }
        """
    }
}

/**
 * GLESMesh - Mesh en GPU
 */
class GLESMesh(private val mesh: Mesh) {
    
    private var vao = 0
    private var vbo = 0
    private var ebo = 0
    private var normalVbo = 0
    
    init {
        create()
    }
    
    private fun create() {
        // Generar VAO
        val vaoArray = IntArray(1)
        glGenVertexArrays(1, vaoArray, 0)
        vao = vaoArray[0]
        
        glBindVertexArray(vao)
        
        // VBO de vértices
        val vboArray = IntArray(1)
        glGenBuffers(1, vboArray, 0)
        vbo = vboArray[0]
        
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(
            GL_ARRAY_BUFFER,
            mesh.vertices.size * 4,
            createFloatBuffer(mesh.vertices),
            GL_STATIC_DRAW
        )
        
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(0)
        
        // VBO de normales
        mesh.normals?.let { normals ->
            val normalVboArray = IntArray(1)
            glGenBuffers(1, normalVboArray, 0)
            normalVbo = normalVboArray[0]
            
            glBindBuffer(GL_ARRAY_BUFFER, normalVbo)
            glBufferData(
                GL_ARRAY_BUFFER,
                normals.size * 4,
                createFloatBuffer(normals),
                GL_STATIC_DRAW
            )
            
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0)
            glEnableVertexAttribArray(1)
        }
        
        // EBO de índices
        val eboArray = IntArray(1)
        glGenBuffers(1, eboArray, 0)
        ebo = eboArray[0]
        
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        glBufferData(
            GL_ELEMENT_ARRAY_BUFFER,
            mesh.indices.size * 4,
            createIntBuffer(mesh.indices),
            GL_STATIC_DRAW
        )
        
        glBindVertexArray(0)
    }
    
    fun render() {
        glBindVertexArray(vao)
        glDrawElements(GL_TRIANGLES, mesh.indices.size, GL_UNSIGNED_INT, 0)
        glBindVertexArray(0)
    }
    
    fun destroy() {
        if (vao != 0) glDeleteVertexArrays(1, intArrayOf(vao), 0)
        if (vbo != 0) glDeleteBuffers(1, intArrayOf(vbo), 0)
        if (ebo != 0) glDeleteBuffers(1, intArrayOf(ebo), 0)
        if (normalVbo != 0) glDeleteBuffers(1, intArrayOf(normalVbo), 0)
    }
    
    private fun createFloatBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data)
            .position(0) as FloatBuffer
    }
    
    private fun createIntBuffer(data: IntArray): IntBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(data)
            .position(0) as IntBuffer
    }
}

/**
 * GLESShader - Shader de OpenGL ES
 */
class GLESShader(
    vertexSource: String,
    fragmentSource: String
) : Shader {
    
    override var programId: Int = 0
        private set
    
    init {
        programId = createProgram(vertexSource, fragmentSource)
    }
    
    override fun use() {
        glUseProgram(programId)
    }
    
    override fun setUniform(name: String, value: Int) {
        val location = glGetUniformLocation(programId, name)
        glUniform1i(location, value)
    }
    
    override fun setUniform(name: String, value: Float) {
        val location = glGetUniformLocation(programId, name)
        glUniform1f(location, value)
    }
    
    override fun setUniform(name: String, value: Vector3) {
        val location = glGetUniformLocation(programId, name)
        glUniform3f(location, value.x, value.y, value.z)
    }
    
    override fun setUniform(name: String, value: Color) {
        val location = glGetUniformLocation(programId, name)
        glUniform4f(location, value.r, value.g, value.b, value.a)
    }
    
    override fun setUniform(name: String, value: Matrix4) {
        val location = glGetUniformLocation(programId, name)
        glUniformMatrix4fv(location, 1, false, value.toArray(), 0)
    }
    
    fun destroy() {
        if (programId != 0) {
            glDeleteProgram(programId)
            programId = 0
        }
    }
    
    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource)
        
        val program = glCreateProgram()
        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)
        glLinkProgram(program)
        
        // Verificar link
        val linkStatus = IntArray(1)
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0)
        
        if (linkStatus[0] == GL_FALSE) {
            val log = glGetProgramInfoLog(program)
            glDeleteProgram(program)
            throw RuntimeException("Shader program link failed: $log")
        }
        
        // Limpiar shaders
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
        
        return program
    }
    
    private fun compileShader(type: Int, source: String): Int {
        val shader = glCreateShader(type)
        glShaderSource(shader, source)
        glCompileShader(shader)
        
        // Verificar compilación
        val compileStatus = IntArray(1)
        glGetShaderiv(shader, GL_COMPILE_STATUS, compileStatus, 0)
        
        if (compileStatus[0] == GL_FALSE) {
            val log = glGetShaderInfoLog(shader)
            glDeleteShader(shader)
            throw RuntimeException("Shader compilation failed: $log")
        }
        
        return shader
    }
}
