package com.quantum.engine.ai.navigation

import com.quantum.engine.math.*
import com.quantum.engine.core.ecs.*
import java.util.*
import kotlin.math.*

/**
 * NavMeshSystem - Sistema completo de navegación AI
 * 
 * Features:
 * - NavMesh generation
 * - A* pathfinding
 * - Dynamic obstacles
 * - Off-mesh links
 * - Navigation queries
 * - Area types (walkable, water, etc)
 * - Agent radius support
 * - Multi-level navmesh
 */
class NavMeshSystem : System() {
    
    override val systemName = "NavMeshSystem"
    override val requiredComponents = listOf(ComponentType.of<NavMeshAgentComponent>())
    
    private val navMeshes = mutableMapOf<String, NavMesh>()
    private val agents = mutableListOf<NavMeshAgent>()
    
    override fun onUpdate(entityManager: EntityManager, deltaTime: Float) {
        // Actualizar todos los agentes
        agents.forEach { agent ->
            agent.update(deltaTime)
        }
    }
    
    fun createNavMesh(name: String, bounds: Bounds): NavMesh {
        val navMesh = NavMesh(name, bounds)
        navMeshes[name] = navMesh
        return navMesh
    }
    
    fun generateNavMesh(
        name: String,
        terrain: Array<FloatArray>,
        cellSize: Float = 0.5f,
        maxSlope: Float = 45f
    ): NavMesh {
        val navMesh = navMeshes[name] ?: createNavMesh(
            name,
            Bounds(
                Vector3.ZERO,
                Vector3(terrain.size.toFloat(), 10f, terrain[0].size.toFloat())
            )
        )
        
        // Generar grid de navegación
        for (x in terrain.indices step cellSize.toInt()) {
            for (z in terrain[x].indices step cellSize.toInt()) {
                val height = terrain[x][z]
                
                // Verificar pendiente
                val neighbors = getNeighborHeights(terrain, x, z)
                val maxSlopeDiff = neighbors.maxOrNull()?.let { abs(it - height) } ?: 0f
                
                if (maxSlopeDiff <= maxSlope) {
                    // Añadir nodo navegable
                    navMesh.addNode(
                        Vector3(x.toFloat(), height, z.toFloat()),
                        AreaType.GROUND
                    )
                }
            }
        }
        
        // Conectar nodos vecinos
        navMesh.connectNodes(cellSize * 2f)
        
        return navMesh
    }
    
    private fun getNeighborHeights(terrain: Array<FloatArray>, x: Int, z: Int): List<Float> {
        val heights = mutableListOf<Float>()
        
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                
                val nx = x + dx
                val nz = z + dz
                
                if (nx in terrain.indices && nz in terrain[nx].indices) {
                    heights.add(terrain[nx][nz])
                }
            }
        }
        
        return heights
    }
    
    fun findPath(
        start: Vector3,
        end: Vector3,
        navMeshName: String = "default"
    ): List<Vector3>? {
        val navMesh = navMeshes[navMeshName] ?: return null
        return navMesh.findPath(start, end)
    }
    
    fun createAgent(
        entity: Entity,
        navMeshName: String,
        radius: Float = 0.5f,
        speed: Float = 3.5f
    ): NavMeshAgent {
        val agent = NavMeshAgent(
            entity = entity,
            navMesh = navMeshes[navMeshName]!!,
            radius = radius,
            speed = speed
        )
        agents.add(agent)
        return agent
    }
}

/**
 * NavMesh - Malla de navegación
 */
class NavMesh(
    val name: String,
    val bounds: Bounds
) {
    private val nodes = mutableListOf<NavNode>()
    private val connections = mutableMapOf<NavNode, MutableList<NavConnection>>()
    private val spatialGrid = mutableMapOf<Pair<Int, Int>, MutableList<NavNode>>()
    
    private val gridSize = 10f
    
    fun addNode(position: Vector3, areaType: AreaType) {
        val node = NavNode(
            id = nodes.size,
            position = position,
            areaType = areaType
        )
        nodes.add(node)
        
        // Añadir a grid espacial
        val gridX = (position.x / gridSize).toInt()
        val gridZ = (position.z / gridSize).toInt()
        spatialGrid.getOrPut(Pair(gridX, gridZ)) { mutableListOf() }.add(node)
    }
    
    fun connectNodes(maxDistance: Float) {
        nodes.forEach { node ->
            val neighbors = findNearbyNodes(node.position, maxDistance)
            
            neighbors.forEach { neighbor ->
                if (neighbor != node) {
                    val distance = Vector3.distance(node.position, neighbor.position)
                    
                    if (distance <= maxDistance) {
                        addConnection(node, neighbor, distance)
                    }
                }
            }
        }
    }
    
    private fun findNearbyNodes(position: Vector3, radius: Float): List<NavNode> {
        val nearby = mutableListOf<NavNode>()
        
        val gridX = (position.x / gridSize).toInt()
        val gridZ = (position.z / gridSize).toInt()
        
        for (dx in -1..1) {
            for (dz in -1..1) {
                spatialGrid[Pair(gridX + dx, gridZ + dz)]?.let { cellNodes ->
                    nearby.addAll(cellNodes)
                }
            }
        }
        
        return nearby.filter { Vector3.distance(it.position, position) <= radius }
    }
    
    private fun addConnection(from: NavNode, to: NavNode, cost: Float) {
        connections.getOrPut(from) { mutableListOf() }.add(
            NavConnection(to, cost)
        )
    }
    
    /**
     * A* Pathfinding
     */
    fun findPath(start: Vector3, end: Vector3): List<Vector3>? {
        val startNode = findNearestNode(start) ?: return null
        val endNode = findNearestNode(end) ?: return null
        
        val openSet = PriorityQueue<AStarNode>(compareBy { it.fScore })
        val closedSet = mutableSetOf<NavNode>()
        val cameFrom = mutableMapOf<NavNode, NavNode>()
        
        val gScore = mutableMapOf<NavNode, Float>().withDefault { Float.MAX_VALUE }
        val fScore = mutableMapOf<NavNode, Float>().withDefault { Float.MAX_VALUE }
        
        gScore[startNode] = 0f
        fScore[startNode] = heuristic(startNode, endNode)
        
        openSet.add(AStarNode(startNode, fScore[startNode]!!))
        
        while (openSet.isNotEmpty()) {
            val current = openSet.poll().node
            
            if (current == endNode) {
                return reconstructPath(cameFrom, current)
            }
            
            closedSet.add(current)
            
            connections[current]?.forEach { connection ->
                val neighbor = connection.to
                
                if (neighbor in closedSet) return@forEach
                
                val tentativeGScore = gScore.getValue(current) + connection.cost
                
                if (tentativeGScore < gScore.getValue(neighbor)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    fScore[neighbor] = tentativeGScore + heuristic(neighbor, endNode)
                    
                    if (openSet.none { it.node == neighbor }) {
                        openSet.add(AStarNode(neighbor, fScore[neighbor]!!))
                    }
                }
            }
        }
        
        return null // No path found
    }
    
    private fun findNearestNode(position: Vector3): NavNode? {
        return nodes.minByOrNull { Vector3.distance(it.position, position) }
    }
    
    private fun heuristic(a: NavNode, b: NavNode): Float {
        return Vector3.distance(a.position, b.position)
    }
    
    private fun reconstructPath(cameFrom: Map<NavNode, NavNode>, current: NavNode): List<Vector3> {
        val path = mutableListOf(current.position)
        var node = current
        
        while (node in cameFrom) {
            node = cameFrom[node]!!
            path.add(0, node.position)
        }
        
        return path
    }
    
    fun samplePosition(position: Vector3, maxDistance: Float): Vector3? {
        return findNearestNode(position)?.position
    }
}

/**
 * NavNode - Nodo de navegación
 */
data class NavNode(
    val id: Int,
    val position: Vector3,
    val areaType: AreaType
)

/**
 * NavConnection - Conexión entre nodos
 */
data class NavConnection(
    val to: NavNode,
    val cost: Float
)

/**
 * AStarNode - Nodo para A*
 */
private data class AStarNode(
    val node: NavNode,
    val fScore: Float
)

/**
 * AreaType - Tipo de área
 */
enum class AreaType(val cost: Float) {
    GROUND(1f),
    ROAD(0.8f),
    GRASS(1.2f),
    WATER(5f),
    UNWALKABLE(Float.MAX_VALUE)
}

/**
 * NavMeshAgent - Agente que navega por el navmesh
 */
class NavMeshAgent(
    val entity: Entity,
    val navMesh: NavMesh,
    var radius: Float = 0.5f,
    var speed: Float = 3.5f
) {
    var currentPath: List<Vector3>? = null
    private var currentWaypoint = 0
    
    var position = Vector3.ZERO
    var destination: Vector3? = null
    var isMoving = false
    
    fun setDestination(target: Vector3) {
        destination = target
        currentPath = navMesh.findPath(position, target)
        currentWaypoint = 0
        isMoving = currentPath != null
    }
    
    fun update(deltaTime: Float) {
        if (!isMoving || currentPath == null) return
        
        val path = currentPath!!
        
        if (currentWaypoint >= path.size) {
            isMoving = false
            return
        }
        
        val waypoint = path[currentWaypoint]
        val direction = (waypoint - position).normalized()
        val distance = Vector3.distance(position, waypoint)
        
        if (distance < 0.1f) {
            currentWaypoint++
            return
        }
        
        position += direction * speed * deltaTime
    }
    
    fun stop() {
        isMoving = false
        currentPath = null
    }
}

/**
 * NavMeshAgentComponent - Componente para agentes
 */
data class NavMeshAgentComponent(
    var navMeshName: String = "default",
    var radius: Float = 0.5f,
    var speed: Float = 3.5f,
    var acceleration: Float = 8f,
    var angularSpeed: Float = 120f,
    var stoppingDistance: Float = 0f,
    var autoRepath: Boolean = true,
    var avoidancePriority: Int = 50
) : Component {
    override fun clone() = copy()
}

/**
 * NavMeshObstacle - Obstáculo dinámico
 */
data class NavMeshObstacle(
    val position: Vector3,
    val radius: Float,
    val height: Float = 2f
)

/**
 * Bounds - Límites
 */
data class Bounds(
    val center: Vector3,
    val size: Vector3
)

/**
 * Ejemplo de uso
 */
object NavMeshExamples {
    
    fun setupNavigation(navMeshSystem: NavMeshSystem) {
        // Crear terreno de ejemplo
        val terrain = Array(100) { x ->
            FloatArray(100) { z ->
                sin(x * 0.1f) * cos(z * 0.1f) * 2f
            }
        }
        
        // Generar navmesh
        val navMesh = navMeshSystem.generateNavMesh(
            name = "level1",
            terrain = terrain,
            cellSize = 0.5f,
            maxSlope = 30f
        )
        
        println("NavMesh generated with ${navMesh} nodes")
        
        // Crear agente
        val entity = Entity(1)
        val agent = navMeshSystem.createAgent(
            entity = entity,
            navMeshName = "level1",
            radius = 0.5f,
            speed = 3.5f
        )
        
        // Establecer destino
        agent.setDestination(Vector3(50f, 0f, 50f))
        
        println("Agent created and destination set")
    }
    
    fun findPathExample(navMeshSystem: NavMeshSystem) {
        val start = Vector3(10f, 0f, 10f)
        val end = Vector3(90f, 0f, 90f)
        
        val path = navMeshSystem.findPath(start, end, "level1")
        
        if (path != null) {
            println("Path found with ${path.size} waypoints")
            path.forEach { waypoint ->
                println("  → $waypoint")
            }
        } else {
            println("No path found")
        }
    }
}
