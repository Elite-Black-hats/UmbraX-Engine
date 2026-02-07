package com.quantum.engine.renderer.vulkan

import android.view.Surface
import com.quantum.engine.renderer.*
import com.quantum.engine.math.*
import com.quantum.engine.core.components.Color
import timber.log.Timber

/**
 * VulkanRenderer - Renderer Vulkan nativo de alto rendimiento
 * 
 * Características:
 * - Vulkan 1.3 API nativa
 * - Pipeline PBR completo
 * - Compute shaders
 * - Ray tracing (si está disponible)
 * - Multi-threading nativo
 * - Descriptor sets optimizados
 * - Dynamic rendering
 */
class VulkanRenderer : Renderer {
    
    override val stats = RenderStats()
    
    private var nativeHandle: Long = 0
    private var isInitialized = false
    
    // Vulkan state
    private var swapchainExtent = Pair(0, 0)
    private var currentFrame = 0
    
    companion object {
        init {
            System.loadLibrary("vulkan_renderer")
        }
    }
    
    override fun initialize() {
        Timber.i("Initializing Vulkan Renderer")
        
        nativeHandle = nativeCreate()
        
        if (nativeHandle == 0L) {
            throw RuntimeException("Failed to create Vulkan renderer")
        }
        
        val result = nativeInitialize(nativeHandle)
        
        if (!result) {
            throw RuntimeException("Failed to initialize Vulkan")
        }
        
        isInitialized = true
        
        Timber.i("Vulkan Renderer initialized successfully")
        printVulkanInfo()
    }
    
    /**
     * Configura la surface de Android
     */
    fun setSurface(surface: Surface, width: Int, height: Int) {
        if (!isInitialized) return
        
        nativeSetSurface(nativeHandle, surface, width, height)
        swapchainExtent = Pair(width, height)
    }
    
    override fun shutdown() {
        if (isInitialized) {
            nativeDestroy(nativeHandle)
            isInitialized = false
            nativeHandle = 0
        }
    }
    
    override fun beginFrame() {
        if (!isInitialized) return
        
        nativeBeginFrame(nativeHandle)
        stats.reset()
    }
    
    override fun endFrame() {
        if (!isInitialized) return
        
        nativeEndFrame(nativeHandle)
        currentFrame++
    }
    
    override fun submit(command: RenderCommand) {
        if (!isInitialized) return
        
        // Convertir a formato nativo
        val transform = command.transform.toArray()
        val color = floatArrayOf(
            command.material.color.r,
            command.material.color.g,
            command.material.color.b,
            command.material.color.a
        )
        
        nativeSubmitMesh(
            nativeHandle,
            getMeshHandle(command.mesh),
            transform,
            color
        )
        
        stats.drawCalls++
        stats.triangles += command.mesh.triangleCount
        stats.vertices += command.mesh.vertexCount
    }
    
    override fun setViewProjection(view: Matrix4, projection: Matrix4) {
        if (!isInitialized) return
        
        nativeSetViewProjection(
            nativeHandle,
            view.toArray(),
            projection.toArray()
        )
    }
    
    override fun clear(color: Color) {
        if (!isInitialized) return
        
        nativeSetClearColor(
            nativeHandle,
            color.r,
            color.g,
            color.b,
            color.a
        )
    }
    
    override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        if (!isInitialized) return
        
        nativeSetViewport(nativeHandle, x, y, width, height)
    }
    
    /**
     * Carga un mesh en Vulkan
     */
    fun loadMesh(mesh: Mesh): Long {
        if (!isInitialized) return 0
        
        return nativeLoadMesh(
            nativeHandle,
            mesh.vertices,
            mesh.indices,
            mesh.normals ?: FloatArray(0),
            mesh.uvs ?: FloatArray(0)
        )
    }
    
    /**
     * Carga una textura
     */
    fun loadTexture(pixels: ByteArray, width: Int, height: Int, format: Int): Long {
        if (!isInitialized) return 0
        
        return nativeLoadTexture(nativeHandle, pixels, width, height, format)
    }
    
    /**
     * Compila un shader
     */
    fun compileShader(spirvCode: ByteArray, stage: ShaderStage): Long {
        if (!isInitialized) return 0
        
        return nativeCompileShader(nativeHandle, spirvCode, stage.ordinal)
    }
    
    /**
     * Crea un pipeline gráfico
     */
    fun createGraphicsPipeline(
        vertexShader: Long,
        fragmentShader: Long,
        config: PipelineConfig
    ): Long {
        if (!isInitialized) return 0
        
        return nativeCreateGraphicsPipeline(
            nativeHandle,
            vertexShader,
            fragmentShader,
            config.toNative()
        )
    }
    
    /**
     * Activa compute shader
     */
    fun dispatchCompute(
        computeShader: Long,
        groupsX: Int,
        groupsY: Int,
        groupsZ: Int
    ) {
        if (!isInitialized) return
        
        nativeDispatchCompute(nativeHandle, computeShader, groupsX, groupsY, groupsZ)
    }
    
    /**
     * Ray tracing (si está disponible)
     */
    fun traceRays(
        raygenShader: Long,
        missShader: Long,
        hitShader: Long,
        width: Int,
        height: Int
    ) {
        if (!isInitialized) return
        
        if (nativeSupportsRayTracing(nativeHandle)) {
            nativeTraceRays(nativeHandle, raygenShader, missShader, hitShader, width, height)
        }
    }
    
    /**
     * Obtiene info de Vulkan
     */
    fun getVulkanInfo(): VulkanInfo {
        if (!isInitialized) return VulkanInfo()
        
        val info = nativeGetVulkanInfo(nativeHandle)
        
        return VulkanInfo(
            deviceName = info[0] as String,
            apiVersion = info[1] as String,
            driverVersion = info[2] as String,
            vendorId = info[3] as Int,
            deviceType = info[4] as String,
            maxTextureSize = info[5] as Int,
            supportsRayTracing = info[6] as Boolean,
            supportsMeshShaders = info[7] as Boolean
        )
    }
    
    private fun printVulkanInfo() {
        val info = getVulkanInfo()
        Timber.i("Vulkan Device: ${info.deviceName}")
        Timber.i("Vulkan API: ${info.apiVersion}")
        Timber.i("Driver: ${info.driverVersion}")
        Timber.i("Ray Tracing: ${info.supportsRayTracing}")
        Timber.i("Mesh Shaders: ${info.supportsMeshShaders}")
    }
    
    private val meshHandles = mutableMapOf<Mesh, Long>()
    
    private fun getMeshHandle(mesh: Mesh): Long {
        return meshHandles.getOrPut(mesh) {
            loadMesh(mesh)
        }
    }
    
    // ========== JNI Native Methods ==========
    
    private external fun nativeCreate(): Long
    private external fun nativeInitialize(handle: Long): Boolean
    private external fun nativeDestroy(handle: Long)
    
    private external fun nativeSetSurface(handle: Long, surface: Surface, width: Int, height: Int)
    
    private external fun nativeBeginFrame(handle: Long)
    private external fun nativeEndFrame(handle: Long)
    
    private external fun nativeSubmitMesh(
        handle: Long,
        meshHandle: Long,
        transform: FloatArray,
        color: FloatArray
    )
    
    private external fun nativeSetViewProjection(
        handle: Long,
        view: FloatArray,
        projection: FloatArray
    )
    
    private external fun nativeSetClearColor(
        handle: Long,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    )
    
    private external fun nativeSetViewport(
        handle: Long,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    )
    
    private external fun nativeLoadMesh(
        handle: Long,
        vertices: FloatArray,
        indices: IntArray,
        normals: FloatArray,
        uvs: FloatArray
    ): Long
    
    private external fun nativeLoadTexture(
        handle: Long,
        pixels: ByteArray,
        width: Int,
        height: Int,
        format: Int
    ): Long
    
    private external fun nativeCompileShader(
        handle: Long,
        spirvCode: ByteArray,
        stage: Int
    ): Long
    
    private external fun nativeCreateGraphicsPipeline(
        handle: Long,
        vertexShader: Long,
        fragmentShader: Long,
        config: IntArray
    ): Long
    
    private external fun nativeDispatchCompute(
        handle: Long,
        computeShader: Long,
        groupsX: Int,
        groupsY: Int,
        groupsZ: Int
    )
    
    private external fun nativeSupportsRayTracing(handle: Long): Boolean
    
    private external fun nativeTraceRays(
        handle: Long,
        raygenShader: Long,
        missShader: Long,
        hitShader: Long,
        width: Int,
        height: Int
    )
    
    private external fun nativeGetVulkanInfo(handle: Long): Array<Any>
}

/**
 * Configuración de pipeline
 */
data class PipelineConfig(
    val topology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST,
    val cullMode: CullMode = CullMode.BACK,
    val frontFace: FrontFace = FrontFace.COUNTER_CLOCKWISE,
    val depthTest: Boolean = true,
    val depthWrite: Boolean = true,
    val blendEnable: Boolean = false
) {
    fun toNative(): IntArray {
        return intArrayOf(
            topology.ordinal,
            cullMode.ordinal,
            frontFace.ordinal,
            if (depthTest) 1 else 0,
            if (depthWrite) 1 else 0,
            if (blendEnable) 1 else 0
        )
    }
}

enum class PrimitiveTopology {
    POINT_LIST,
    LINE_LIST,
    LINE_STRIP,
    TRIANGLE_LIST,
    TRIANGLE_STRIP
}

enum class CullMode {
    NONE,
    FRONT,
    BACK,
    FRONT_AND_BACK
}

enum class FrontFace {
    COUNTER_CLOCKWISE,
    CLOCKWISE
}

enum class ShaderStage {
    VERTEX,
    FRAGMENT,
    COMPUTE,
    RAYGEN,
    MISS,
    CLOSEST_HIT
}

/**
 * Información de Vulkan
 */
data class VulkanInfo(
    val deviceName: String = "",
    val apiVersion: String = "",
    val driverVersion: String = "",
    val vendorId: Int = 0,
    val deviceType: String = "",
    val maxTextureSize: Int = 0,
    val supportsRayTracing: Boolean = false,
    val supportsMeshShaders: Boolean = false
)

/**
 * VulkanSurface - Wrapper para Surface de Android
 */
class VulkanSurface(
    private val renderer: VulkanRenderer,
    private var surface: Surface?,
    private var width: Int,
    private var height: Int
) {
    
    fun resize(newWidth: Int, newHeight: Int) {
        width = newWidth
        height = newHeight
        
        surface?.let {
            renderer.setSurface(it, width, height)
        }
    }
    
    fun release() {
        surface = null
    }
}
