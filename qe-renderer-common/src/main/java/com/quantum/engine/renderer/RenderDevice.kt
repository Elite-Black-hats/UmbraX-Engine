package com.quantum.engine.renderer

import com.quantum.engine.math.*

/**
 * RenderDevice - Abstracción del dispositivo de renderizado
 * 
 * Interfaz común para Vulkan, OpenGL ES, etc.
 */
interface RenderDevice {
    
    /**
     * Información del dispositivo
     */
    val deviceInfo: DeviceInfo
    
    /**
     * Capacidades del dispositivo
     */
    val capabilities: DeviceCapabilities
    
    /**
     * Inicializa el dispositivo de renderizado
     */
    fun initialize(surface: Any): Boolean
    
    /**
     * Cierra el dispositivo
     */
    fun shutdown()
    
    /**
     * Comienza un frame de renderizado
     */
    fun beginFrame(): Boolean
    
    /**
     * Termina y presenta el frame
     */
    fun endFrame()
    
    /**
     * Espera a que todas las operaciones de GPU terminen
     */
    fun waitIdle()
    
    /**
     * Crea un render pass
     */
    fun createRenderPass(descriptor: RenderPassDescriptor): RenderPass
    
    /**
     * Crea un pipeline
     */
    fun createPipeline(descriptor: PipelineDescriptor): Pipeline
    
    /**
     * Crea un buffer
     */
    fun createBuffer(descriptor: BufferDescriptor): Buffer
    
    /**
     * Crea una textura
     */
    fun createTexture(descriptor: TextureDescriptor): Texture
    
    /**
     * Crea un sampler
     */
    fun createSampler(descriptor: SamplerDescriptor): Sampler
    
    /**
     * Crea un shader
     */
    fun createShader(descriptor: ShaderDescriptor): Shader
    
    /**
     * Obtiene el command buffer actual
     */
    fun getCurrentCommandBuffer(): CommandBuffer
    
    /**
     * Redimensiona el swapchain
     */
    fun resize(width: Int, height: Int)
}

/**
 * Información del dispositivo
 */
data class DeviceInfo(
    val deviceName: String,
    val vendorName: String,
    val driverVersion: String,
    val apiVersion: String,
    val deviceType: DeviceType
)

enum class DeviceType {
    INTEGRATED_GPU,
    DISCRETE_GPU,
    VIRTUAL_GPU,
    CPU,
    OTHER
}

/**
 * Capacidades del dispositivo
 */
data class DeviceCapabilities(
    val maxTextureSize: Int,
    val maxTextureLayers: Int,
    val maxColorAttachments: Int,
    val maxUniformBufferSize: Int,
    val maxStorageBufferSize: Int,
    val maxPushConstantSize: Int,
    val maxComputeWorkGroupSize: Vector3,
    val maxComputeWorkGroupCount: Vector3,
    val supportsGeometryShader: Boolean,
    val supportsTessellationShader: Boolean,
    val supportsComputeShader: Boolean,
    val supportsMultiDrawIndirect: Boolean,
    val supportsDepthClamp: Boolean,
    val supportsAnisotropicFiltering: Boolean,
    val maxAnisotropy: Float,
    val timestampPeriod: Float
)

/**
 * CommandBuffer - Buffer de comandos de renderizado
 */
interface CommandBuffer {
    
    /**
     * Comienza un render pass
     */
    fun beginRenderPass(renderPass: RenderPass, framebuffer: Framebuffer, clearValues: List<ClearValue>)
    
    /**
     * Termina el render pass actual
     */
    fun endRenderPass()
    
    /**
     * Bind de pipeline
     */
    fun bindPipeline(pipeline: Pipeline)
    
    /**
     * Bind de vertex buffers
     */
    fun bindVertexBuffers(buffers: List<Buffer>, offsets: List<Long> = emptyList())
    
    /**
     * Bind de index buffer
     */
    fun bindIndexBuffer(buffer: Buffer, offset: Long = 0, indexType: IndexType = IndexType.UINT32)
    
    /**
     * Bind de descriptor sets
     */
    fun bindDescriptorSets(sets: List<DescriptorSet>, firstSet: Int = 0)
    
    /**
     * Push constants
     */
    fun pushConstants(stage: ShaderStage, offset: Int, data: ByteArray)
    
    /**
     * Draw call
     */
    fun draw(vertexCount: Int, instanceCount: Int = 1, firstVertex: Int = 0, firstInstance: Int = 0)
    
    /**
     * Draw indexed
     */
    fun drawIndexed(
        indexCount: Int,
        instanceCount: Int = 1,
        firstIndex: Int = 0,
        vertexOffset: Int = 0,
        firstInstance: Int = 0
    )
    
    /**
     * Draw indirect
     */
    fun drawIndirect(buffer: Buffer, offset: Long, drawCount: Int, stride: Int)
    
    /**
     * Dispatch compute
     */
    fun dispatch(groupCountX: Int, groupCountY: Int, groupCountZ: Int)
    
    /**
     * Barrera de memoria
     */
    fun barrier(
        srcStage: PipelineStage,
        dstStage: PipelineStage,
        barriers: List<MemoryBarrier>
    )
    
    /**
     * Copia entre buffers
     */
    fun copyBuffer(src: Buffer, dst: Buffer, regions: List<BufferCopyRegion>)
    
    /**
     * Copia buffer a textura
     */
    fun copyBufferToTexture(src: Buffer, dst: Texture, regions: List<BufferTextureCopyRegion>)
    
    /**
     * Establece viewport
     */
    fun setViewport(x: Float, y: Float, width: Float, height: Float, minDepth: Float = 0f, maxDepth: Float = 1f)
    
    /**
     * Establece scissor
     */
    fun setScissor(x: Int, y: Int, width: Int, height: Int)
    
    /**
     * Establece line width
     */
    fun setLineWidth(width: Float)
    
    /**
     * Blit de textura
     */
    fun blitTexture(
        src: Texture,
        dst: Texture,
        srcRegion: TextureRegion,
        dstRegion: TextureRegion,
        filter: Filter
    )
}

/**
 * RenderPass - Define estructura de rendering
 */
interface RenderPass {
    fun destroy()
}

data class RenderPassDescriptor(
    val attachments: List<AttachmentDescriptor>,
    val subpasses: List<SubpassDescriptor>,
    val dependencies: List<SubpassDependency>
)

data class AttachmentDescriptor(
    val format: TextureFormat,
    val samples: SampleCount,
    val loadOp: AttachmentLoadOp,
    val storeOp: AttachmentStoreOp,
    val stencilLoadOp: AttachmentLoadOp = AttachmentLoadOp.DONT_CARE,
    val stencilStoreOp: AttachmentStoreOp = AttachmentStoreOp.DONT_CARE,
    val initialLayout: TextureLayout,
    val finalLayout: TextureLayout
)

enum class AttachmentLoadOp {
    LOAD, CLEAR, DONT_CARE
}

enum class AttachmentStoreOp {
    STORE, DONT_CARE
}

enum class TextureLayout {
    UNDEFINED,
    GENERAL,
    COLOR_ATTACHMENT,
    DEPTH_STENCIL_ATTACHMENT,
    DEPTH_STENCIL_READ_ONLY,
    SHADER_READ_ONLY,
    TRANSFER_SRC,
    TRANSFER_DST,
    PRESENT_SRC
}

data class SubpassDescriptor(
    val colorAttachments: List<AttachmentReference>,
    val depthStencilAttachment: AttachmentReference? = null,
    val inputAttachments: List<AttachmentReference> = emptyList(),
    val resolveAttachments: List<AttachmentReference> = emptyList()
)

data class AttachmentReference(
    val attachment: Int,
    val layout: TextureLayout
)

data class SubpassDependency(
    val srcSubpass: Int,
    val dstSubpass: Int,
    val srcStage: PipelineStage,
    val dstStage: PipelineStage,
    val srcAccess: AccessFlags,
    val dstAccess: AccessFlags
)

/**
 * Pipeline - Estado de renderizado
 */
interface Pipeline {
    fun destroy()
}

data class PipelineDescriptor(
    val shaders: List<Shader>,
    val vertexInput: VertexInputDescriptor,
    val inputAssembly: InputAssemblyDescriptor,
    val rasterization: RasterizationDescriptor,
    val multisample: MultisampleDescriptor,
    val depthStencil: DepthStencilDescriptor,
    val colorBlend: ColorBlendDescriptor,
    val dynamicStates: List<DynamicState> = emptyList(),
    val layout: PipelineLayout,
    val renderPass: RenderPass,
    val subpass: Int = 0
)

data class VertexInputDescriptor(
    val bindings: List<VertexInputBinding>,
    val attributes: List<VertexInputAttribute>
)

data class VertexInputBinding(
    val binding: Int,
    val stride: Int,
    val inputRate: VertexInputRate
)

enum class VertexInputRate {
    VERTEX, INSTANCE
}

data class VertexInputAttribute(
    val location: Int,
    val binding: Int,
    val format: VertexFormat,
    val offset: Int
)

enum class VertexFormat {
    FLOAT, FLOAT2, FLOAT3, FLOAT4,
    INT, INT2, INT3, INT4,
    UINT, UINT2, UINT3, UINT4
}

data class InputAssemblyDescriptor(
    val topology: PrimitiveTopology,
    val primitiveRestartEnable: Boolean = false
)

enum class PrimitiveTopology {
    POINT_LIST,
    LINE_LIST,
    LINE_STRIP,
    TRIANGLE_LIST,
    TRIANGLE_STRIP,
    TRIANGLE_FAN
}

data class RasterizationDescriptor(
    val depthClampEnable: Boolean = false,
    val rasterizerDiscardEnable: Boolean = false,
    val polygonMode: PolygonMode = PolygonMode.FILL,
    val cullMode: CullMode = CullMode.BACK,
    val frontFace: FrontFace = FrontFace.COUNTER_CLOCKWISE,
    val depthBiasEnable: Boolean = false,
    val depthBiasConstantFactor: Float = 0f,
    val depthBiasClamp: Float = 0f,
    val depthBiasSlopeFactor: Float = 0f,
    val lineWidth: Float = 1f
)

enum class PolygonMode {
    FILL, LINE, POINT
}

enum class CullMode {
    NONE, FRONT, BACK, FRONT_AND_BACK
}

enum class FrontFace {
    CLOCKWISE, COUNTER_CLOCKWISE
}

data class MultisampleDescriptor(
    val sampleCount: SampleCount = SampleCount.COUNT_1,
    val sampleShadingEnable: Boolean = false,
    val minSampleShading: Float = 1f
)

enum class SampleCount {
    COUNT_1, COUNT_2, COUNT_4, COUNT_8, COUNT_16, COUNT_32, COUNT_64
}

data class DepthStencilDescriptor(
    val depthTestEnable: Boolean = true,
    val depthWriteEnable: Boolean = true,
    val depthCompareOp: CompareOp = CompareOp.LESS,
    val depthBoundsTestEnable: Boolean = false,
    val stencilTestEnable: Boolean = false,
    val front: StencilOpState = StencilOpState(),
    val back: StencilOpState = StencilOpState(),
    val minDepthBounds: Float = 0f,
    val maxDepthBounds: Float = 1f
)

enum class CompareOp {
    NEVER, LESS, EQUAL, LESS_OR_EQUAL,
    GREATER, NOT_EQUAL, GREATER_OR_EQUAL, ALWAYS
}

data class StencilOpState(
    val failOp: StencilOp = StencilOp.KEEP,
    val passOp: StencilOp = StencilOp.KEEP,
    val depthFailOp: StencilOp = StencilOp.KEEP,
    val compareOp: CompareOp = CompareOp.ALWAYS,
    val compareMask: Int = 0,
    val writeMask: Int = 0,
    val reference: Int = 0
)

enum class StencilOp {
    KEEP, ZERO, REPLACE, INCREMENT_AND_CLAMP,
    DECREMENT_AND_CLAMP, INVERT, INCREMENT_AND_WRAP, DECREMENT_AND_WRAP
}

data class ColorBlendDescriptor(
    val logicOpEnable: Boolean = false,
    val logicOp: LogicOp = LogicOp.COPY,
    val attachments: List<ColorBlendAttachment>,
    val blendConstants: FloatArray = floatArrayOf(0f, 0f, 0f, 0f)
)

enum class LogicOp {
    CLEAR, AND, AND_REVERSE, COPY, AND_INVERTED,
    NO_OP, XOR, OR, NOR, EQUIVALENT, INVERT,
    OR_REVERSE, COPY_INVERTED, OR_INVERTED, NAND, SET
}

data class ColorBlendAttachment(
    val blendEnable: Boolean = false,
    val srcColorBlendFactor: BlendFactor = BlendFactor.ONE,
    val dstColorBlendFactor: BlendFactor = BlendFactor.ZERO,
    val colorBlendOp: BlendOp = BlendOp.ADD,
    val srcAlphaBlendFactor: BlendFactor = BlendFactor.ONE,
    val dstAlphaBlendFactor: BlendFactor = BlendFactor.ZERO,
    val alphaBlendOp: BlendOp = BlendOp.ADD,
    val colorWriteMask: ColorComponentFlags = ColorComponentFlags.ALL
)

enum class BlendFactor {
    ZERO, ONE,
    SRC_COLOR, ONE_MINUS_SRC_COLOR,
    DST_COLOR, ONE_MINUS_DST_COLOR,
    SRC_ALPHA, ONE_MINUS_SRC_ALPHA,
    DST_ALPHA, ONE_MINUS_DST_ALPHA,
    CONSTANT_COLOR, ONE_MINUS_CONSTANT_COLOR,
    CONSTANT_ALPHA, ONE_MINUS_CONSTANT_ALPHA
}

enum class BlendOp {
    ADD, SUBTRACT, REVERSE_SUBTRACT, MIN, MAX
}

@JvmInline
value class ColorComponentFlags(val value: Int) {
    companion object {
        val R = ColorComponentFlags(1 shl 0)
        val G = ColorComponentFlags(1 shl 1)
        val B = ColorComponentFlags(1 shl 2)
        val A = ColorComponentFlags(1 shl 3)
        val ALL = ColorComponentFlags(0xF)
    }
}

enum class DynamicState {
    VIEWPORT, SCISSOR, LINE_WIDTH, DEPTH_BIAS,
    BLEND_CONSTANTS, DEPTH_BOUNDS, STENCIL_COMPARE_MASK,
    STENCIL_WRITE_MASK, STENCIL_REFERENCE
}

/**
 * PipelineLayout
 */
interface PipelineLayout {
    fun destroy()
}

/**
 * Shader
 */
interface Shader {
    val stage: ShaderStage
    fun destroy()
}

data class ShaderDescriptor(
    val stage: ShaderStage,
    val code: ByteArray,
    val entryPoint: String = "main"
)

enum class ShaderStage {
    VERTEX, FRAGMENT, COMPUTE,
    GEOMETRY, TESSELLATION_CONTROL, TESSELLATION_EVALUATION
}

/**
 * Buffer
 */
interface Buffer {
    val size: Long
    val usage: BufferUsage
    
    fun map(): ByteArray
    fun unmap()
    fun update(data: ByteArray, offset: Long = 0)
    fun destroy()
}

data class BufferDescriptor(
    val size: Long,
    val usage: BufferUsage,
    val memoryUsage: MemoryUsage
)

@JvmInline
value class BufferUsage(val value: Int) {
    companion object {
        val TRANSFER_SRC = BufferUsage(1 shl 0)
        val TRANSFER_DST = BufferUsage(1 shl 1)
        val UNIFORM_BUFFER = BufferUsage(1 shl 4)
        val STORAGE_BUFFER = BufferUsage(1 shl 5)
        val INDEX_BUFFER = BufferUsage(1 shl 6)
        val VERTEX_BUFFER = BufferUsage(1 shl 7)
        val INDIRECT_BUFFER = BufferUsage(1 shl 8)
    }
}

enum class MemoryUsage {
    GPU_ONLY,    // VRAM, mejor performance
    CPU_TO_GPU,  // Staging buffer
    GPU_TO_CPU,  // Readback
    CPU_ONLY     // System RAM
}

/**
 * Texture
 */
interface Texture {
    val width: Int
    val height: Int
    val depth: Int
    val format: TextureFormat
    val mipLevels: Int
    
    fun destroy()
}

data class TextureDescriptor(
    val type: TextureType,
    val width: Int,
    val height: Int,
    val depth: Int = 1,
    val mipLevels: Int = 1,
    val arrayLayers: Int = 1,
    val format: TextureFormat,
    val usage: TextureUsage,
    val samples: SampleCount = SampleCount.COUNT_1
)

enum class TextureType {
    TEXTURE_1D,
    TEXTURE_2D,
    TEXTURE_3D,
    TEXTURE_CUBE,
    TEXTURE_1D_ARRAY,
    TEXTURE_2D_ARRAY,
    TEXTURE_CUBE_ARRAY
}

enum class TextureFormat {
    // Color formats
    R8_UNORM, R8_SNORM, R8_UINT, R8_SINT,
    R16_UNORM, R16_SNORM, R16_UINT, R16_SINT, R16_SFLOAT,
    R32_UINT, R32_SINT, R32_SFLOAT,
    
    RG8_UNORM, RG8_SNORM, RG8_UINT, RG8_SINT,
    RG16_UNORM, RG16_SNORM, RG16_UINT, RG16_SINT, RG16_SFLOAT,
    RG32_UINT, RG32_SINT, RG32_SFLOAT,
    
    RGB8_UNORM, RGB8_SNORM, RGB8_UINT, RGB8_SINT,
    
    RGBA8_UNORM, RGBA8_SNORM, RGBA8_UINT, RGBA8_SINT, RGBA8_SRGB,
    RGBA16_UNORM, RGBA16_SNORM, RGBA16_UINT, RGBA16_SINT, RGBA16_SFLOAT,
    RGBA32_UINT, RGBA32_SINT, RGBA32_SFLOAT,
    
    BGRA8_UNORM, BGRA8_SRGB,
    
    // Depth/Stencil formats
    D16_UNORM,
    D32_SFLOAT,
    S8_UINT,
    D16_UNORM_S8_UINT,
    D24_UNORM_S8_UINT,
    D32_SFLOAT_S8_UINT,
    
    // Compressed formats
    BC1_RGB_UNORM, BC1_RGB_SRGB,
    BC1_RGBA_UNORM, BC1_RGBA_SRGB,
    BC2_UNORM, BC2_SRGB,
    BC3_UNORM, BC3_SRGB,
    BC4_UNORM, BC4_SNORM,
    BC5_UNORM, BC5_SNORM,
    BC6H_UFLOAT, BC6H_SFLOAT,
    BC7_UNORM, BC7_SRGB,
    
    ETC2_R8G8B8_UNORM, ETC2_R8G8B8_SRGB,
    ETC2_R8G8B8A1_UNORM, ETC2_R8G8B8A1_SRGB,
    ETC2_R8G8B8A8_UNORM, ETC2_R8G8B8A8_SRGB
}

@JvmInline
value class TextureUsage(val value: Int) {
    companion object {
        val TRANSFER_SRC = TextureUsage(1 shl 0)
        val TRANSFER_DST = TextureUsage(1 shl 1)
        val SAMPLED = TextureUsage(1 shl 2)
        val STORAGE = TextureUsage(1 shl 3)
        val COLOR_ATTACHMENT = TextureUsage(1 shl 4)
        val DEPTH_STENCIL_ATTACHMENT = TextureUsage(1 shl 5)
        val INPUT_ATTACHMENT = TextureUsage(1 shl 7)
    }
}

/**
 * Sampler
 */
interface Sampler {
    fun destroy()
}

data class SamplerDescriptor(
    val magFilter: Filter,
    val minFilter: Filter,
    val mipmapMode: MipmapMode,
    val addressModeU: AddressMode,
    val addressModeV: AddressMode,
    val addressModeW: AddressMode,
    val mipLodBias: Float = 0f,
    val anisotropyEnable: Boolean = false,
    val maxAnisotropy: Float = 1f,
    val compareEnable: Boolean = false,
    val compareOp: CompareOp = CompareOp.ALWAYS,
    val minLod: Float = 0f,
    val maxLod: Float = Float.MAX_VALUE,
    val borderColor: BorderColor = BorderColor.FLOAT_TRANSPARENT_BLACK
)

enum class Filter {
    NEAREST, LINEAR
}

enum class MipmapMode {
    NEAREST, LINEAR
}

enum class AddressMode {
    REPEAT, MIRRORED_REPEAT, CLAMP_TO_EDGE, CLAMP_TO_BORDER
}

enum class BorderColor {
    FLOAT_TRANSPARENT_BLACK,
    INT_TRANSPARENT_BLACK,
    FLOAT_OPAQUE_BLACK,
    INT_OPAQUE_BLACK,
    FLOAT_OPAQUE_WHITE,
    INT_OPAQUE_WHITE
}

/**
 * DescriptorSet
 */
interface DescriptorSet {
    fun updateBuffer(binding: Int, buffer: Buffer, offset: Long = 0, range: Long = -1)
    fun updateTexture(binding: Int, texture: Texture, sampler: Sampler? = null)
    fun destroy()
}

/**
 * Framebuffer
 */
interface Framebuffer {
    val width: Int
    val height: Int
    fun destroy()
}

/**
 * Valores auxiliares
 */
sealed class ClearValue {
    data class Color(val r: Float, val g: Float, val b: Float, val a: Float) : ClearValue()
    data class DepthStencil(val depth: Float, val stencil: Int) : ClearValue()
}

enum class IndexType {
    UINT16, UINT32
}

@JvmInline
value class PipelineStage(val value: Int) {
    companion object {
        val TOP_OF_PIPE = PipelineStage(1 shl 0)
        val DRAW_INDIRECT = PipelineStage(1 shl 1)
        val VERTEX_INPUT = PipelineStage(1 shl 2)
        val VERTEX_SHADER = PipelineStage(1 shl 3)
        val FRAGMENT_SHADER = PipelineStage(1 shl 7)
        val EARLY_FRAGMENT_TESTS = PipelineStage(1 shl 8)
        val LATE_FRAGMENT_TESTS = PipelineStage(1 shl 9)
        val COLOR_ATTACHMENT_OUTPUT = PipelineStage(1 shl 10)
        val COMPUTE_SHADER = PipelineStage(1 shl 11)
        val TRANSFER = PipelineStage(1 shl 12)
        val BOTTOM_OF_PIPE = PipelineStage(1 shl 13)
    }
}

@JvmInline
value class AccessFlags(val value: Int) {
    companion object {
        val NONE = AccessFlags(0)
        val INDIRECT_COMMAND_READ = AccessFlags(1 shl 0)
        val INDEX_READ = AccessFlags(1 shl 1)
        val VERTEX_ATTRIBUTE_READ = AccessFlags(1 shl 2)
        val UNIFORM_READ = AccessFlags(1 shl 3)
        val INPUT_ATTACHMENT_READ = AccessFlags(1 shl 4)
        val SHADER_READ = AccessFlags(1 shl 5)
        val SHADER_WRITE = AccessFlags(1 shl 6)
        val COLOR_ATTACHMENT_READ = AccessFlags(1 shl 7)
        val COLOR_ATTACHMENT_WRITE = AccessFlags(1 shl 8)
        val DEPTH_STENCIL_ATTACHMENT_READ = AccessFlags(1 shl 9)
        val DEPTH_STENCIL_ATTACHMENT_WRITE = AccessFlags(1 shl 10)
        val TRANSFER_READ = AccessFlags(1 shl 11)
        val TRANSFER_WRITE = AccessFlags(1 shl 12)
    }
}

data class MemoryBarrier(
    val srcAccess: AccessFlags,
    val dstAccess: AccessFlags
)

data class BufferCopyRegion(
    val srcOffset: Long,
    val dstOffset: Long,
    val size: Long
)

data class BufferTextureCopyRegion(
    val bufferOffset: Long,
    val bufferRowLength: Int,
    val bufferImageHeight: Int,
    val textureSubresource: TextureSubresource,
    val textureOffset: Vector3,
    val textureExtent: Vector3
)

data class TextureSubresource(
    val aspectMask: TextureAspect,
    val mipLevel: Int,
    val baseArrayLayer: Int,
    val layerCount: Int
)

@JvmInline
value class TextureAspect(val value: Int) {
    companion object {
        val COLOR = TextureAspect(1 shl 0)
        val DEPTH = TextureAspect(1 shl 1)
        val STENCIL = TextureAspect(1 shl 2)
    }
}

data class TextureRegion(
    val offset: Vector3,
    val extent: Vector3,
    val mipLevel: Int = 0,
    val baseArrayLayer: Int = 0,
    val layerCount: Int = 1
)
