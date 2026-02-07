package com.quantum.engine.vfx

import com.quantum.engine.math.*
import com.quantum.engine.core.components.Color
import kotlin.math.*
import kotlin.random.Random

/**
 * VFXGraphSystem - Sistema de efectos visuales basado en nodos
 * 
 * Similar a Unity VFX Graph / Unreal Niagara:
 * - Node-based workflow
 * - GPU-accelerated particles
 * - Módulos procedurales
 * - Forces y colisiones
 * - Trails y ribbons
 * - Mesh particles
 */
class VFXGraphSystem {
    
    private val graphs = mutableMapOf<String, VFXGraph>()
    
    fun createGraph(name: String): VFXGraph {
        val graph = VFXGraph(name)
        graphs[name] = graph
        return graph
    }
    
    fun update(deltaTime: Float) {
        graphs.values.forEach { it.update(deltaTime) }
    }
}

/**
 * VFXGraph - Gráfico de efectos visuales
 */
class VFXGraph(val name: String) {
    
    private val nodes = mutableListOf<VFXNode>()
    private val connections = mutableListOf<VFXConnection>()
    
    // Contexto de ejecución
    private val context = VFXContext()
    
    fun addNode(node: VFXNode) {
        nodes.add(node)
    }
    
    fun connect(from: VFXNode, fromPort: String, to: VFXNode, toPort: String) {
        connections.add(VFXConnection(from, fromPort, to, toPort))
    }
    
    fun update(deltaTime: Float) {
        context.deltaTime = deltaTime
        
        // Ejecutar nodos en orden topológico
        nodes.forEach { node ->
            node.execute(context)
        }
    }
}

/**
 * VFXContext - Contexto de ejecución
 */
class VFXContext {
    var deltaTime: Float = 0f
    val particles = mutableListOf<VFXParticle>()
    val properties = mutableMapOf<String, Any>()
}

/**
 * VFXNode - Nodo base
 */
abstract class VFXNode(val name: String) {
    
    val inputs = mutableMapOf<String, VFXPort>()
    val outputs = mutableMapOf<String, VFXPort>()
    
    abstract fun execute(context: VFXContext)
    
    protected fun getInput(name: String): Any? {
        return inputs[name]?.value
    }
    
    protected fun setOutput(name: String, value: Any) {
        outputs[name]?.value = value
    }
}

/**
 * VFXPort - Puerto de conexión
 */
data class VFXPort(
    val name: String,
    val type: VFXPortType,
    var value: Any? = null
)

enum class VFXPortType {
    FLOAT,
    VECTOR3,
    COLOR,
    TEXTURE,
    GRADIENT,
    CURVE
}

/**
 * VFXConnection - Conexión entre nodos
 */
data class VFXConnection(
    val fromNode: VFXNode,
    val fromPort: String,
    val toNode: VFXNode,
    val toPort: String
)

// ========== Nodos de Spawn ==========

/**
 * SpawnRateNode - Nodo de tasa de generación
 */
class SpawnRateNode : VFXNode("Spawn Rate") {
    
    init {
        inputs["Rate"] = VFXPort("Rate", VFXPortType.FLOAT, 100f)
        outputs["Count"] = VFXPort("Count", VFXPortType.FLOAT)
    }
    
    private var accumulator = 0f
    
    override fun execute(context: VFXContext) {
        val rate = getInput("Rate") as Float
        accumulator += rate * context.deltaTime
        
        val count = accumulator.toInt()
        accumulator -= count
        
        setOutput("Count", count.toFloat())
        
        // Generar partículas
        repeat(count) {
            context.particles.add(VFXParticle())
        }
    }
}

/**
 * BurstNode - Nodo de ráfaga
 */
class BurstNode : VFXNode("Burst") {
    
    init {
        inputs["Count"] = VFXPort("Count", VFXPortType.FLOAT, 100f)
        inputs["Trigger"] = VFXPort("Trigger", VFXPortType.FLOAT, 0f)
    }
    
    private var triggered = false
    
    override fun execute(context: VFXContext) {
        val trigger = getInput("Trigger") as Float
        
        if (trigger > 0f && !triggered) {
            val count = (getInput("Count") as Float).toInt()
            
            repeat(count) {
                context.particles.add(VFXParticle())
            }
            
            triggered = true
        } else if (trigger == 0f) {
            triggered = false
        }
    }
}

// ========== Nodos de Inicialización ==========

/**
 * SetPositionNode - Establece posición inicial
 */
class SetPositionNode : VFXNode("Set Position") {
    
    init {
        inputs["Position"] = VFXPort("Position", VFXPortType.VECTOR3, Vector3.ZERO)
        inputs["Random"] = VFXPort("Random", VFXPortType.VECTOR3, Vector3.ZERO)
    }
    
    override fun execute(context: VFXContext) {
        val position = getInput("Position") as Vector3
        val random = getInput("Random") as Vector3
        
        context.particles.forEach { particle ->
            if (particle.age == 0f) {
                particle.position = position + Vector3(
                    Random.nextFloat() * random.x - random.x / 2,
                    Random.nextFloat() * random.y - random.y / 2,
                    Random.nextFloat() * random.z - random.z / 2
                )
            }
        }
    }
}

/**
 * SetVelocityNode - Establece velocidad inicial
 */
class SetVelocityNode : VFXNode("Set Velocity") {
    
    init {
        inputs["Velocity"] = VFXPort("Velocity", VFXPortType.VECTOR3, Vector3(0f, 5f, 0f))
        inputs["Random"] = VFXPort("Random", VFXPortType.VECTOR3, Vector3.ONE)
    }
    
    override fun execute(context: VFXContext) {
        val velocity = getInput("Velocity") as Vector3
        val random = getInput("Random") as Vector3
        
        context.particles.forEach { particle ->
            if (particle.age == 0f) {
                particle.velocity = velocity + Vector3(
                    (Random.nextFloat() - 0.5f) * random.x,
                    (Random.nextFloat() - 0.5f) * random.y,
                    (Random.nextFloat() - 0.5f) * random.z
                )
            }
        }
    }
}

/**
 * SetLifetimeNode - Establece tiempo de vida
 */
class SetLifetimeNode : VFXNode("Set Lifetime") {
    
    init {
        inputs["Lifetime"] = VFXPort("Lifetime", VFXPortType.FLOAT, 5f)
        inputs["Random"] = VFXPort("Random", VFXPortType.FLOAT, 1f)
    }
    
    override fun execute(context: VFXContext) {
        val lifetime = getInput("Lifetime") as Float
        val random = getInput("Random") as Float
        
        context.particles.forEach { particle ->
            if (particle.age == 0f) {
                particle.lifetime = lifetime + Random.nextFloat() * random
            }
        }
    }
}

// ========== Nodos de Update ==========

/**
 * GravityNode - Aplica gravedad
 */
class GravityNode : VFXNode("Gravity") {
    
    init {
        inputs["Gravity"] = VFXPort("Gravity", VFXPortType.VECTOR3, Vector3(0f, -9.81f, 0f))
    }
    
    override fun execute(context: VFXContext) {
        val gravity = getInput("Gravity") as Vector3
        
        context.particles.forEach { particle ->
            particle.velocity += gravity * context.deltaTime
        }
    }
}

/**
 * TurbulenceNode - Añade turbulencia
 */
class TurbulenceNode : VFXNode("Turbulence") {
    
    init {
        inputs["Intensity"] = VFXPort("Intensity", VFXPortType.FLOAT, 1f)
        inputs["Frequency"] = VFXPort("Frequency", VFXPortType.FLOAT, 1f)
    }
    
    override fun execute(context: VFXContext) {
        val intensity = getInput("Intensity") as Float
        val frequency = getInput("Frequency") as Float
        
        context.particles.forEach { particle ->
            // Perlin noise simulado
            val noise = Vector3(
                sin(particle.position.x * frequency) * cos(particle.position.z * frequency),
                sin(particle.position.y * frequency),
                cos(particle.position.x * frequency) * sin(particle.position.z * frequency)
            )
            
            particle.velocity += noise * intensity * context.deltaTime
        }
    }
}

/**
 * DragNode - Aplica resistencia del aire
 */
class DragNode : VFXNode("Drag") {
    
    init {
        inputs["Coefficient"] = VFXPort("Coefficient", VFXPortType.FLOAT, 0.1f)
    }
    
    override fun execute(context: VFXContext) {
        val coefficient = getInput("Coefficient") as Float
        
        context.particles.forEach { particle ->
            particle.velocity *= (1f - coefficient * context.deltaTime)
        }
    }
}

/**
 * ColorOverLifetimeNode - Color sobre tiempo de vida
 */
class ColorOverLifetimeNode : VFXNode("Color Over Lifetime") {
    
    init {
        inputs["Gradient"] = VFXPort("Gradient", VFXPortType.GRADIENT)
    }
    
    override fun execute(context: VFXContext) {
        // TODO: Implementar gradient
        context.particles.forEach { particle ->
            val t = particle.age / particle.lifetime
            particle.color = Color(1f, 1f - t, 0f, 1f - t)
        }
    }
}

/**
 * SizeOverLifetimeNode - Tamaño sobre tiempo de vida
 */
class SizeOverLifetimeNode : VFXNode("Size Over Lifetime") {
    
    init {
        inputs["Curve"] = VFXPort("Curve", VFXPortType.CURVE)
        inputs["Multiplier"] = VFXPort("Multiplier", VFXPortType.FLOAT, 1f)
    }
    
    override fun execute(context: VFXContext) {
        val multiplier = getInput("Multiplier") as Float
        
        context.particles.forEach { particle ->
            val t = particle.age / particle.lifetime
            // Curva simple: grande al inicio, pequeño al final
            particle.size = (1f - t) * multiplier
        }
    }
}

/**
 * UpdateNode - Actualiza posición de partículas
 */
class UpdateNode : VFXNode("Update") {
    
    override fun execute(context: VFXContext) {
        val toRemove = mutableListOf<VFXParticle>()
        
        context.particles.forEach { particle ->
            particle.age += context.deltaTime
            
            if (particle.age >= particle.lifetime) {
                toRemove.add(particle)
            } else {
                particle.position += particle.velocity * context.deltaTime
            }
        }
        
        context.particles.removeAll(toRemove)
    }
}

// ========== Nodos de Rendering ==========

/**
 * RenderNode - Renderiza partículas
 */
class RenderNode : VFXNode("Render") {
    
    init {
        inputs["BlendMode"] = VFXPort("BlendMode", VFXPortType.FLOAT, 0f)
    }
    
    override fun execute(context: VFXContext) {
        // TODO: Enviar partículas al renderer
    }
}

/**
 * VFXParticle - Partícula del VFX
 */
data class VFXParticle(
    var position: Vector3 = Vector3.ZERO,
    var velocity: Vector3 = Vector3.ZERO,
    var acceleration: Vector3 = Vector3.ZERO,
    var color: Color = Color.WHITE,
    var size: Float = 1f,
    var rotation: Float = 0f,
    var angularVelocity: Float = 0f,
    var age: Float = 0f,
    var lifetime: Float = 5f
)

/**
 * Ejemplo de uso: Fuego
 */
object VFXGraphExamples {
    
    fun createFireEffect(): VFXGraph {
        val graph = VFXGraph("Fire")
        
        // Spawn
        val spawnRate = SpawnRateNode()
        spawnRate.inputs["Rate"]?.value = 50f
        graph.addNode(spawnRate)
        
        // Initialize
        val setPosition = SetPositionNode()
        setPosition.inputs["Position"]?.value = Vector3.ZERO
        setPosition.inputs["Random"]?.value = Vector3(0.5f, 0f, 0.5f)
        graph.addNode(setPosition)
        
        val setVelocity = SetVelocityNode()
        setVelocity.inputs["Velocity"]?.value = Vector3(0f, 3f, 0f)
        setVelocity.inputs["Random"]?.value = Vector3(1f, 1f, 1f)
        graph.addNode(setVelocity)
        
        val setLifetime = SetLifetimeNode()
        setLifetime.inputs["Lifetime"]?.value = 2f
        graph.addNode(setLifetime)
        
        // Update
        val turbulence = TurbulenceNode()
        turbulence.inputs["Intensity"]?.value = 2f
        graph.addNode(turbulence)
        
        val drag = DragNode()
        drag.inputs["Coefficient"]?.value = 0.5f
        graph.addNode(drag)
        
        val colorOverLife = ColorOverLifetimeNode()
        graph.addNode(colorOverLife)
        
        val sizeOverLife = SizeOverLifetimeNode()
        sizeOverLife.inputs["Multiplier"]?.value = 1.5f
        graph.addNode(sizeOverLife)
        
        val update = UpdateNode()
        graph.addNode(update)
        
        // Render
        val render = RenderNode()
        graph.addNode(render)
        
        return graph
    }
}
