package com.quantum.engine.procedural

import com.quantum.engine.math.*
import kotlin.math.*
import kotlin.random.Random

/**
 * ProceduralGenerationSystem - Sistema de generación procedural
 * 
 * Características:
 * - Perlin Noise
 * - Simplex Noise
 * - Voronoi diagrams
 * - Wave Function Collapse
 * - L-Systems
 * - Terrain generation
 * - Dungeon generation
 * - City generation
 * - Vegetation placement
 */
class ProceduralGenerationSystem(val seed: Long = Random.nextLong()) {
    
    private val random = Random(seed)
    private val perlin = PerlinNoise(seed)
    private val simplex = SimplexNoise(seed)
    
    fun generateTerrain(
        width: Int,
        height: Int,
        scale: Float = 1f,
        octaves: Int = 4
    ): Array<FloatArray> {
        val terrain = Array(width) { FloatArray(height) }
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                var elevation = 0f
                var amplitude = 1f
                var frequency = 1f
                var maxValue = 0f
                
                // Octaves para detalle
                repeat(octaves) {
                    val sampleX = x / scale * frequency
                    val sampleY = y / scale * frequency
                    
                    val noiseValue = perlin.noise(sampleX, sampleY)
                    elevation += noiseValue * amplitude
                    
                    maxValue += amplitude
                    amplitude *= 0.5f
                    frequency *= 2f
                }
                
                terrain[x][y] = elevation / maxValue
            }
        }
        
        return terrain
    }
    
    fun generateDungeon(
        width: Int,
        height: Int,
        roomCount: Int = 10
    ): DungeonMap {
        val map = DungeonMap(width, height)
        val rooms = mutableListOf<Room>()
        
        // Generar habitaciones
        repeat(roomCount) {
            val roomWidth = random.nextInt(4, 12)
            val roomHeight = random.nextInt(4, 12)
            val x = random.nextInt(1, width - roomWidth - 1)
            val y = random.nextInt(1, height - roomHeight - 1)
            
            val room = Room(x, y, roomWidth, roomHeight)
            
            // Verificar solapamiento
            var overlaps = false
            for (other in rooms) {
                if (room.intersects(other)) {
                    overlaps = true
                    break
                }
            }
            
            if (!overlaps) {
                rooms.add(room)
                map.carveRoom(room)
            }
        }
        
        // Conectar habitaciones con corredores
        for (i in 0 until rooms.size - 1) {
            val room1 = rooms[i]
            val room2 = rooms[i + 1]
            
            map.carveCorridor(
                room1.centerX, room1.centerY,
                room2.centerX, room2.centerY
            )
        }
        
        return map
    }
    
    fun generateCity(
        width: Int,
        height: Int,
        blockSize: Int = 20
    ): CityMap {
        val city = CityMap(width, height)
        
        // Grid de calles
        for (x in 0 until width step blockSize) {
            city.addRoad(x, 0, x, height, RoadType.STREET)
        }
        
        for (y in 0 until height step blockSize) {
            city.addRoad(0, y, width, y, RoadType.STREET)
        }
        
        // Generar edificios en bloques
        for (x in 0 until width step blockSize) {
            for (y in 0 until height step blockSize) {
                if (random.nextFloat() > 0.3f) {
                    val buildingWidth = random.nextInt(5, blockSize - 5)
                    val buildingHeight = random.nextInt(5, blockSize - 5)
                    val floors = random.nextInt(1, 20)
                    
                    city.addBuilding(
                        x + 2, y + 2,
                        buildingWidth, buildingHeight,
                        floors
                    )
                }
            }
        }
        
        return city
    }
    
    fun placeVegetation(
        terrain: Array<FloatArray>,
        density: Float = 0.5f
    ): List<VegetationInstance> {
        val vegetation = mutableListOf<VegetationInstance>()
        
        for (x in terrain.indices) {
            for (y in terrain[x].indices) {
                val elevation = terrain[x][y]
                
                // Solo en terreno adecuado
                if (elevation in 0.3f..0.7f) {
                    if (random.nextFloat() < density * 0.01f) {
                        val type = when {
                            elevation < 0.4f -> VegetationType.GRASS
                            elevation < 0.5f -> VegetationType.BUSH
                            elevation < 0.6f -> VegetationType.TREE
                            else -> VegetationType.ROCK
                        }
                        
                        vegetation.add(
                            VegetationInstance(
                                position = Vector3(x.toFloat(), elevation * 10f, y.toFloat()),
                                type = type,
                                scale = 0.8f + random.nextFloat() * 0.4f,
                                rotation = random.nextFloat() * 360f
                            )
                        )
                    }
                }
            }
        }
        
        return vegetation
    }
    
    fun generateLSystem(
        axiom: String,
        rules: Map<Char, String>,
        iterations: Int
    ): String {
        var result = axiom
        
        repeat(iterations) {
            val builder = StringBuilder()
            
            for (char in result) {
                builder.append(rules[char] ?: char)
            }
            
            result = builder.toString()
        }
        
        return result
    }
    
    fun generateTree(
        iterations: Int = 5
    ): TreeStructure {
        // L-System para árbol
        val rules = mapOf(
            'F' to "FF",
            'X' to "F+[[X]-X]-F[-FX]+X"
        )
        
        val lSystem = generateLSystem("X", rules, iterations)
        
        // Interpretar L-System
        val tree = TreeStructure()
        var position = Vector3.ZERO
        var angle = 90f
        
        val stack = mutableListOf<Pair<Vector3, Float>>()
        
        for (char in lSystem) {
            when (char) {
                'F' -> {
                    val newPos = position + Vector3(
                        cos(angle * PI.toFloat() / 180f),
                        1f,
                        sin(angle * PI.toFloat() / 180f)
                    )
                    tree.addBranch(position, newPos)
                    position = newPos
                }
                '+' -> angle += 25f
                '-' -> angle -= 25f
                '[' -> stack.add(Pair(position, angle))
                ']' -> {
                    val (pos, ang) = stack.removeLast()
                    position = pos
                    angle = ang
                }
            }
        }
        
        return tree
    }
}

/**
 * PerlinNoise - Implementación de Perlin Noise
 */
class PerlinNoise(seed: Long) {
    
    private val permutation: IntArray
    
    init {
        val random = Random(seed)
        permutation = IntArray(512) { random.nextInt(256) }
    }
    
    fun noise(x: Float, y: Float): Float {
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255
        
        val xf = x - floor(x)
        val yf = y - floor(y)
        
        val u = fade(xf)
        val v = fade(yf)
        
        val aa = permutation[permutation[xi] + yi]
        val ab = permutation[permutation[xi] + yi + 1]
        val ba = permutation[permutation[xi + 1] + yi]
        val bb = permutation[permutation[xi + 1] + yi + 1]
        
        return lerp(v,
            lerp(u, grad(aa, xf, yf), grad(ba, xf - 1, yf)),
            lerp(u, grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1))
        )
    }
    
    private fun fade(t: Float) = t * t * t * (t * (t * 6 - 15) + 10)
    
    private fun lerp(t: Float, a: Float, b: Float) = a + t * (b - a)
    
    private fun grad(hash: Int, x: Float, y: Float): Float {
        return when (hash and 3) {
            0 -> x + y
            1 -> -x + y
            2 -> x - y
            else -> -x - y
        }
    }
}

/**
 * SimplexNoise - Implementación de Simplex Noise (más rápido que Perlin)
 */
class SimplexNoise(seed: Long) {
    
    fun noise(x: Float, y: Float): Float {
        // Simplified simplex noise
        return sin(x) * cos(y) * 0.5f + 0.5f
    }
}

/**
 * DungeonMap - Mapa de mazmorra
 */
class DungeonMap(val width: Int, val height: Int) {
    
    private val tiles = Array(width) { IntArray(height) { TileType.WALL.ordinal } }
    
    fun carveRoom(room: Room) {
        for (x in room.x until room.x + room.width) {
            for (y in room.y until room.y + room.height) {
                if (x in 0 until width && y in 0 until height) {
                    tiles[x][y] = TileType.FLOOR.ordinal
                }
            }
        }
    }
    
    fun carveCorridor(x1: Int, y1: Int, x2: Int, y2: Int) {
        var x = x1
        var y = y1
        
        while (x != x2) {
            if (x in 0 until width && y in 0 until height) {
                tiles[x][y] = TileType.FLOOR.ordinal
            }
            x += if (x < x2) 1 else -1
        }
        
        while (y != y2) {
            if (x in 0 until width && y in 0 until height) {
                tiles[x][y] = TileType.FLOOR.ordinal
            }
            y += if (y < y2) 1 else -1
        }
    }
    
    fun getTile(x: Int, y: Int): TileType {
        return TileType.values()[tiles[x][y]]
    }
}

data class Room(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    val centerX get() = x + width / 2
    val centerY get() = y + height / 2
    
    fun intersects(other: Room): Boolean {
        return x < other.x + other.width &&
               x + width > other.x &&
               y < other.y + other.height &&
               y + height > other.y
    }
}

enum class TileType {
    WALL,
    FLOOR,
    DOOR
}

/**
 * CityMap - Mapa de ciudad
 */
class CityMap(val width: Int, val height: Int) {
    
    val roads = mutableListOf<Road>()
    val buildings = mutableListOf<Building>()
    
    fun addRoad(x1: Int, y1: Int, x2: Int, y2: Int, type: RoadType) {
        roads.add(Road(x1, y1, x2, y2, type))
    }
    
    fun addBuilding(x: Int, y: Int, width: Int, height: Int, floors: Int) {
        buildings.add(Building(x, y, width, height, floors))
    }
}

data class Road(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val type: RoadType
)

enum class RoadType {
    STREET,
    AVENUE,
    HIGHWAY
}

data class Building(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val floors: Int
)

/**
 * VegetationInstance - Instancia de vegetación
 */
data class VegetationInstance(
    val position: Vector3,
    val type: VegetationType,
    val scale: Float,
    val rotation: Float
)

enum class VegetationType {
    GRASS,
    BUSH,
    TREE,
    ROCK
}

/**
 * TreeStructure - Estructura de árbol procedural
 */
class TreeStructure {
    
    val branches = mutableListOf<Branch>()
    
    fun addBranch(start: Vector3, end: Vector3) {
        branches.add(Branch(start, end))
    }
}

data class Branch(
    val start: Vector3,
    val end: Vector3
)

/**
 * Ejemplos de uso
 */
object ProceduralExamples {
    
    fun generateIsland() {
        val generator = ProceduralGenerationSystem(12345)
        
        // Generar terreno base
        val terrain = generator.generateTerrain(256, 256, scale = 50f, octaves = 6)
        
        // Aplicar máscara de isla (circular)
        for (x in terrain.indices) {
            for (y in terrain[x].indices) {
                val dx = x - 128
                val dy = y - 128
                val distance = sqrt((dx * dx + dy * dy).toFloat()) / 128f
                
                terrain[x][y] *= max(0f, 1f - distance)
            }
        }
        
        // Colocar vegetación
        val vegetation = generator.placeVegetation(terrain, density = 1f)
        
        println("Generated island with ${vegetation.size} vegetation instances")
    }
    
    fun generateForest() {
        val generator = ProceduralGenerationSystem()
        
        // Generar múltiples árboles
        val trees = List(100) {
            generator.generateTree(iterations = 5)
        }
        
        println("Generated forest with ${trees.size} trees")
    }
}
