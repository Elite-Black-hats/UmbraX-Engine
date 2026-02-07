package com.quantum.engine.shadergraph

import com.quantum.engine.math.*
import com.quantum.engine.core.components.Color

/**
 * ShaderGraphSystem - Sistema de creación de shaders por nodos
 * 
 * Similar a Unity Shader Graph / Unreal Material Editor:
 * - Node-based shader creation
 * - Visual programming
 * - PBR nodes
 * - Math nodes
 * - Texture sampling
 * - Custom functions
 * - Real-time preview
 * - SPIR-V code generation
 */
class ShaderGraphSystem {
    
    private val graphs = mutableMapOf<String, ShaderGraph>()
    
    fun createGraph(name: String, type: ShaderType): ShaderGraph {
        val graph = ShaderGraph(name, type)
        graphs[name] = graph
        return graph
    }
    
    fun compile(graphName: String): CompiledShader? {
        val graph = graphs[graphName] ?: return null
        return graph.compile()
    }
}

/**
 * ShaderGraph - Gráfico de shader
 */
class ShaderGraph(
    val name: String,
    val type: ShaderType
) {
    private val nodes = mutableListOf<ShaderNode>()
    private val connections = mutableListOf<NodeConnection>()
    
    var masterNode: MasterNode? = null
    
    fun addNode(node: ShaderNode) {
        nodes.add(node)
    }
    
    fun connect(from: ShaderNode, fromPort: String, to: ShaderNode, toPort: String) {
        connections.add(NodeConnection(from, fromPort, to, toPort))
    }
    
    fun compile(): CompiledShader {
        // Generar código GLSL/SPIR-V
        val vertexCode = generateVertexCode()
        val fragmentCode = generateFragmentCode()
        
        return CompiledShader(
            name = name,
            vertexCode = vertexCode,
            fragmentCode = fragmentCode
        )
    }
    
    private fun generateVertexCode(): String {
        return """
            #version 450
            
            layout(location = 0) in vec3 inPosition;
            layout(location = 1) in vec3 inNormal;
            layout(location = 2) in vec2 inTexCoord;
            
            layout(location = 0) out vec3 fragPosition;
            layout(location = 1) out vec3 fragNormal;
            layout(location = 2) out vec2 fragTexCoord;
            
            layout(binding = 0) uniform UniformBufferObject {
                mat4 model;
                mat4 view;
                mat4 projection;
            } ubo;
            
            void main() {
                vec4 worldPos = ubo.model * vec4(inPosition, 1.0);
                gl_Position = ubo.projection * ubo.view * worldPos;
                
                fragPosition = worldPos.xyz;
                fragNormal = mat3(ubo.model) * inNormal;
                fragTexCoord = inTexCoord;
            }
        """.trimIndent()
    }
    
    private fun generateFragmentCode(): String {
        val builder = StringBuilder()
        
        builder.appendLine("#version 450")
        builder.appendLine()
        builder.appendLine("layout(location = 0) in vec3 fragPosition;")
        builder.appendLine("layout(location = 1) in vec3 fragNormal;")
        builder.appendLine("layout(location = 2) in vec2 fragTexCoord;")
        builder.appendLine()
        builder.appendLine("layout(location = 0) out vec4 outColor;")
        builder.appendLine()
        
        // Generar uniforms
        generateUniforms(builder)
        
        builder.appendLine()
        builder.appendLine("void main() {")
        
        // Generar código de nodos
        generateNodeCode(builder)
        
        builder.appendLine("}")
        
        return builder.toString()
    }
    
    private fun generateUniforms(builder: StringBuilder) {
        nodes.filterIsInstance<TextureNode>().forEachIndexed { index, node ->
            builder.appendLine("layout(binding = ${index + 1}) uniform sampler2D ${node.uniformName};")
        }
    }
    
    private fun generateNodeCode(builder: StringBuilder) {
        // Evaluar nodos en orden topológico
        masterNode?.let { master ->
            evaluateNode(master, builder)
        }
    }
    
    private fun evaluateNode(node: ShaderNode, builder: StringBuilder) {
        // TODO: Implementar evaluación recursiva
        builder.appendLine("    outColor = vec4(1.0, 0.0, 1.0, 1.0); // Placeholder")
    }
}

/**
 * ShaderNode - Nodo base
 */
abstract class ShaderNode(val name: String) {
    val inputs = mutableMapOf<String, NodePort>()
    val outputs = mutableMapOf<String, NodePort>()
    
    abstract fun generateCode(): String
}

/**
 * NodePort - Puerto de conexión
 */
data class NodePort(
    val name: String,
    val type: PortType,
    var connectedNode: ShaderNode? = null,
    var connectedPort: String? = null
)

enum class PortType {
    FLOAT,
    VECTOR2,
    VECTOR3,
    VECTOR4,
    COLOR,
    TEXTURE
}

/**
 * NodeConnection - Conexión entre nodos
 */
data class NodeConnection(
    val fromNode: ShaderNode,
    val fromPort: String,
    val toNode: ShaderNode,
    val toPort: String
)

enum class ShaderType {
    SURFACE,
    UNLIT,
    POST_PROCESS,
    COMPUTE
}

// ========== Master Nodes ==========

/**
 * MasterNode - Nodo de salida final
 */
class MasterNode : ShaderNode("Master") {
    init {
        inputs["Albedo"] = NodePort("Albedo", PortType.COLOR)
        inputs["Metallic"] = NodePort("Metallic", PortType.FLOAT)
        inputs["Smoothness"] = NodePort("Smoothness", PortType.FLOAT)
        inputs["Normal"] = NodePort("Normal", PortType.VECTOR3)
        inputs["Emission"] = NodePort("Emission", PortType.COLOR)
        inputs["Alpha"] = NodePort("Alpha", PortType.FLOAT)
    }
    
    override fun generateCode(): String {
        return """
            // PBR Lighting calculation
            vec3 albedo = ${getInputCode("Albedo")};
            float metallic = ${getInputCode("Metallic")};
            float smoothness = ${getInputCode("Smoothness")};
            vec3 normal = ${getInputCode("Normal")};
            vec3 emission = ${getInputCode("Emission")};
            float alpha = ${getInputCode("Alpha")};
            
            // Simplified PBR
            vec3 lighting = albedo * 0.8;
            outColor = vec4(lighting + emission, alpha);
        """.trimIndent()
    }
    
    private fun getInputCode(portName: String): String {
        val port = inputs[portName]
        return if (port?.connectedNode != null) {
            port.connectedNode!!.generateCode()
        } else {
            when (portName) {
                "Albedo" -> "vec3(1.0)"
                "Metallic" -> "0.0"
                "Smoothness" -> "0.5"
                "Normal" -> "normalize(fragNormal)"
                "Emission" -> "vec3(0.0)"
                "Alpha" -> "1.0"
                else -> "0.0"
            }
        }
    }
}

// ========== Input Nodes ==========

/**
 * TextureNode - Sample texture
 */
class TextureNode(val uniformName: String = "mainTexture") : ShaderNode("Texture") {
    init {
        inputs["UV"] = NodePort("UV", PortType.VECTOR2)
        outputs["RGB"] = NodePort("RGB", PortType.VECTOR3)
        outputs["RGBA"] = NodePort("RGBA", PortType.VECTOR4)
        outputs["R"] = NodePort("R", PortType.FLOAT)
        outputs["G"] = NodePort("G", PortType.FLOAT)
        outputs["B"] = NodePort("B", PortType.FLOAT)
        outputs["A"] = NodePort("A", PortType.FLOAT)
    }
    
    override fun generateCode(): String {
        val uvCode = inputs["UV"]?.connectedNode?.generateCode() ?: "fragTexCoord"
        return "texture($uniformName, $uvCode)"
    }
}

/**
 * ColorNode - Color constant
 */
class ColorNode(var color: Color = Color.WHITE) : ShaderNode("Color") {
    init {
        outputs["Color"] = NodePort("Color", PortType.COLOR)
    }
    
    override fun generateCode(): String {
        return "vec4(${color.r}, ${color.g}, ${color.b}, ${color.a})"
    }
}

/**
 * FloatNode - Float constant
 */
class FloatNode(var value: Float = 0f) : ShaderNode("Float") {
    init {
        outputs["Value"] = NodePort("Value", PortType.FLOAT)
    }
    
    override fun generateCode(): String {
        return value.toString()
    }
}

/**
 * Vector3Node - Vector3 constant
 */
class Vector3Node(var value: Vector3 = Vector3.ZERO) : ShaderNode("Vector3") {
    init {
        outputs["Vector"] = NodePort("Vector", PortType.VECTOR3)
    }
    
    override fun generateCode(): String {
        return "vec3(${value.x}, ${value.y}, ${value.z})"
    }
}

/**
 * UVNode - Texture coordinates
 */
class UVNode : ShaderNode("UV") {
    init {
        outputs["UV"] = NodePort("UV", PortType.VECTOR2)
    }
    
    override fun generateCode(): String {
        return "fragTexCoord"
    }
}

/**
 * NormalNode - Surface normal
 */
class NormalNode : ShaderNode("Normal") {
    init {
        outputs["Normal"] = NodePort("Normal", PortType.VECTOR3)
    }
    
    override fun generateCode(): String {
        return "normalize(fragNormal)"
    }
}

/**
 * PositionNode - World position
 */
class PositionNode : ShaderNode("Position") {
    init {
        outputs["Position"] = NodePort("Position", PortType.VECTOR3)
    }
    
    override fun generateCode(): String {
        return "fragPosition"
    }
}

// ========== Math Nodes ==========

/**
 * AddNode - Addition
 */
class AddNode : ShaderNode("Add") {
    init {
        inputs["A"] = NodePort("A", PortType.FLOAT)
        inputs["B"] = NodePort("B", PortType.FLOAT)
        outputs["Result"] = NodePort("Result", PortType.FLOAT)
    }
    
    override fun generateCode(): String {
        val a = inputs["A"]?.connectedNode?.generateCode() ?: "0.0"
        val b = inputs["B"]?.connectedNode?.generateCode() ?: "0.0"
        return "($a + $b)"
    }
}

/**
 * MultiplyNode - Multiplication
 */
class MultiplyNode : ShaderNode("Multiply") {
    init {
        inputs["A"] = NodePort("A", PortType.FLOAT)
        inputs["B"] = NodePort("B", PortType.FLOAT)
        outputs["Result"] = NodePort("Result", PortType.FLOAT)
    }
    
    override fun generateCode(): String {
        val a = inputs["A"]?.connectedNode?.generateCode() ?: "1.0"
        val b = inputs["B"]?.connectedNode?.generateCode() ?: "1.0"
        return "($a * $b)"
    }
}

/**
 * LerpNode - Linear interpolation
 */
class LerpNode : ShaderNode("Lerp") {
    init {
        inputs["A"] = NodePort("A", PortType.VECTOR3)
        inputs["B"] = NodePort("B", PortType.VECTOR3)
        inputs["T"] = NodePort("T", PortType.FLOAT)
        outputs["Result"] = NodePort("Result", PortType.VECTOR3)
    }
    
    override fun generateCode(): String {
        val a = inputs["A"]?.connectedNode?.generateCode() ?: "vec3(0.0)"
        val b = inputs["B"]?.connectedNode?.generateCode() ?: "vec3(1.0)"
        val t = inputs["T"]?.connectedNode?.generateCode() ?: "0.5"
        return "mix($a, $b, $t)"
    }
}

/**
 * DotProductNode - Dot product
 */
class DotProductNode : ShaderNode("Dot Product") {
    init {
        inputs["A"] = NodePort("A", PortType.VECTOR3)
        inputs["B"] = NodePort("B", PortType.VECTOR3)
        outputs["Result"] = NodePort("Result", PortType.FLOAT)
    }
    
    override fun generateCode(): String {
        val a = inputs["A"]?.connectedNode?.generateCode() ?: "vec3(0.0)"
        val b = inputs["B"]?.connectedNode?.generateCode() ?: "vec3(1.0)"
        return "dot($a, $b)"
    }
}

/**
 * FresnelNode - Fresnel effect
 */
class FresnelNode : ShaderNode("Fresnel") {
    init {
        inputs["Normal"] = NodePort("Normal", PortType.VECTOR3)
        inputs["ViewDir"] = NodePort("ViewDir", PortType.VECTOR3)
        inputs["Power"] = NodePort("Power", PortType.FLOAT)
        outputs["Result"] = NodePort("Result", PortType.FLOAT)
    }
    
    override fun generateCode(): String {
        val normal = inputs["Normal"]?.connectedNode?.generateCode() ?: "fragNormal"
        val viewDir = inputs["ViewDir"]?.connectedNode?.generateCode() ?: "normalize(cameraPos - fragPosition)"
        val power = inputs["Power"]?.connectedNode?.generateCode() ?: "5.0"
        
        return "pow(1.0 - max(dot($normal, $viewDir), 0.0), $power)"
    }
}

// ========== Utility Nodes ==========

/**
 * NoiseNode - Procedural noise
 */
class NoiseNode : ShaderNode("Noise") {
    init {
        inputs["UV"] = NodePort("UV", PortType.VECTOR2)
        inputs["Scale"] = NodePort("Scale", PortType.FLOAT)
        outputs["Noise"] = NodePort("Noise", PortType.FLOAT)
    }
    
    override fun generateCode(): String {
        return """
            fract(sin(dot(fragTexCoord, vec2(12.9898, 78.233))) * 43758.5453)
        """.trimIndent()
    }
}

/**
 * CompiledShader - Shader compilado
 */
data class CompiledShader(
    val name: String,
    val vertexCode: String,
    val fragmentCode: String
)

/**
 * Ejemplo de uso
 */
object ShaderGraphExamples {
    
    fun createPBRShader(): ShaderGraph {
        val graph = ShaderGraph("PBR Material", ShaderType.SURFACE)
        
        // Master node
        val master = MasterNode()
        graph.masterNode = master
        
        // Albedo texture
        val albedoTex = TextureNode("albedoTexture")
        graph.addNode(albedoTex)
        graph.connect(albedoTex, "RGB", master, "Albedo")
        
        // Metallic value
        val metallic = FloatNode(0.5f)
        graph.addNode(metallic)
        graph.connect(metallic, "Value", master, "Metallic")
        
        // Smoothness value
        val smoothness = FloatNode(0.7f)
        graph.addNode(smoothness)
        graph.connect(smoothness, "Value", master, "Smoothness")
        
        return graph
    }
    
    fun createHologramShader(): ShaderGraph {
        val graph = ShaderGraph("Hologram", ShaderType.SURFACE)
        
        val master = MasterNode()
        graph.masterNode = master
        
        // Fresnel for edges
        val fresnel = FresnelNode()
        val color = ColorNode(Color(0f, 1f, 1f, 1f))
        
        graph.addNode(fresnel)
        graph.addNode(color)
        
        graph.connect(fresnel, "Result", master, "Emission")
        
        return graph
    }
}
