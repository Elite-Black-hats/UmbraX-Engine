package com.quantum.engine.scripting

import com.quantum.engine.core.ecs.*
import org.luaj.vm2.*
import org.luaj.vm2.lib.jse.JsePlatform
import org.mozilla.javascript.*
import timber.log.Timber
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

/**
 * ScriptingSystem - Sistema de scripting multi-lenguaje
 * 
 * Soporta:
 * - Kotlin (nativo)
 * - Java (compilación dinámica)
 * - JavaScript (Rhino)
 * - Lua (LuaJ)
 * - Python (Chaquopy)
 */
class ScriptingSystem : System() {
    
    override val systemName = "ScriptingSystem"
    override val requiredComponents = listOf(ComponentType.of<ScriptComponent>())
    
    // Engines por lenguaje
    private val kotlinEngine = KotlinScriptEngine()
    private val jsEngine = JavaScriptEngine()
    private val luaEngine = LuaScriptEngine()
    private val pythonEngine = PythonScriptEngine()
    
    // Cache de scripts compilados
    private val compiledScripts = mutableMapOf<String, CompiledScript>()
    
    override fun onUpdate(entityManager: EntityManager, deltaTime: Float) {
        // Los scripts se ejecutan via eventos o calls directos
    }
    
    /**
     * Ejecuta un script según su lenguaje
     */
    fun executeScript(
        language: ScriptLanguage,
        code: String,
        context: ScriptContext
    ): ScriptResult {
        return try {
            when (language) {
                ScriptLanguage.KOTLIN -> kotlinEngine.execute(code, context)
                ScriptLanguage.JAVA -> executeJava(code, context)
                ScriptLanguage.JAVASCRIPT -> jsEngine.execute(code, context)
                ScriptLanguage.LUA -> luaEngine.execute(code, context)
                ScriptLanguage.PYTHON -> pythonEngine.execute(code, context)
            }
        } catch (e: Exception) {
            Timber.e(e, "Script execution failed")
            ScriptResult(success = false, error = e.message)
        }
    }
    
    /**
     * Compila y cachea un script
     */
    fun compileScript(
        id: String,
        language: ScriptLanguage,
        code: String
    ): CompiledScript {
        return compiledScripts.getOrPut(id) {
            when (language) {
                ScriptLanguage.KOTLIN -> kotlinEngine.compile(code)
                ScriptLanguage.JAVA -> compileJava(code)
                ScriptLanguage.JAVASCRIPT -> jsEngine.compile(code)
                ScriptLanguage.LUA -> luaEngine.compile(code)
                ScriptLanguage.PYTHON -> pythonEngine.compile(code)
            }
        }
    }
    
    private fun executeJava(code: String, context: ScriptContext): ScriptResult {
        // TODO: Compilación dinámica de Java
        return ScriptResult(success = false, error = "Java compilation not yet implemented")
    }
    
    private fun compileJava(code: String): CompiledScript {
        // TODO: Compilación dinámica
        return CompiledScript("", ScriptLanguage.JAVA, null)
    }
    
    /**
     * Registra API para que scripts puedan acceder
     */
    fun registerAPI(name: String, api: Any) {
        kotlinEngine.registerAPI(name, api)
        jsEngine.registerAPI(name, api)
        luaEngine.registerAPI(name, api)
        pythonEngine.registerAPI(name, api)
    }
}

/**
 * KotlinScriptEngine - Motor de scripts Kotlin
 */
class KotlinScriptEngine {
    
    private val host = BasicJvmScriptingHost()
    private val apis = mutableMapOf<String, Any>()
    
    fun execute(code: String, context: ScriptContext): ScriptResult {
        val scriptWithImports = buildScript(code, context)
        
        val result = host.eval(scriptWithImports.toScriptSource())
        
        return when (result) {
            is ResultWithDiagnostics.Success -> {
                ScriptResult(
                    success = true,
                    returnValue = result.value.returnValue.scriptValueToAny()
                )
            }
            is ResultWithDiagnostics.Failure -> {
                ScriptResult(
                    success = false,
                    error = result.reports.joinToString("\n") { it.message }
                )
            }
        }
    }
    
    fun compile(code: String): CompiledScript {
        return CompiledScript(code, ScriptLanguage.KOTLIN, null)
    }
    
    private fun buildScript(code: String, context: ScriptContext): String {
        return """
            import com.quantum.engine.math.*
            import com.quantum.engine.core.ecs.*
            
            // APIs registradas
            ${apis.entries.joinToString("\n") { "val ${it.key} = ${it.value}" }}
            
            // Context
            val entity = ${context.entity}
            val deltaTime = ${context.deltaTime}
            
            // Script del usuario
            $code
        """.trimIndent()
    }
    
    fun registerAPI(name: String, api: Any) {
        apis[name] = api
    }
    
    private fun ResultValue.scriptValueToAny(): Any? {
        return when (val v = this) {
            is ResultValue.Value -> v.value
            else -> null
        }
    }
}

/**
 * JavaScriptEngine - Motor JavaScript con Rhino
 */
class JavaScriptEngine {
    
    private val context: org.mozilla.javascript.Context = org.mozilla.javascript.Context.enter()
    private val scope: Scriptable = context.initStandardObjects()
    
    init {
        context.optimizationLevel = -1 // Interpretado (para Android)
    }
    
    fun execute(code: String, scriptContext: ScriptContext): ScriptResult {
        return try {
            // Inyectar contexto
            scope.put("entity", scope, scriptContext.entity)
            scope.put("deltaTime", scope, scriptContext.deltaTime)
            
            val result = context.evaluateString(scope, code, "<script>", 1, null)
            
            ScriptResult(
                success = true,
                returnValue = org.mozilla.javascript.Context.jsToJava(result, Any::class.java)
            )
        } catch (e: Exception) {
            ScriptResult(success = false, error = e.message)
        }
    }
    
    fun compile(code: String): CompiledScript {
        val script = context.compileString(code, "<compiled>", 1, null)
        return CompiledScript(code, ScriptLanguage.JAVASCRIPT, script)
    }
    
    fun registerAPI(name: String, api: Any) {
        scope.put(name, scope, api)
    }
}

/**
 * LuaScriptEngine - Motor Lua con LuaJ
 */
class LuaScriptEngine {
    
    private val globals: Globals = JsePlatform.standardGlobals()
    
    fun execute(code: String, context: ScriptContext): ScriptResult {
        return try {
            // Inyectar contexto
            globals.set("entity", CoerceJavaToLua.coerce(context.entity))
            globals.set("deltaTime", LuaValue.valueOf(context.deltaTime.toDouble()))
            
            val chunk = globals.load(code)
            val result = chunk.call()
            
            ScriptResult(
                success = true,
                returnValue = result.tojstring()
            )
        } catch (e: Exception) {
            ScriptResult(success = false, error = e.message)
        }
    }
    
    fun compile(code: String): CompiledScript {
        val chunk = globals.load(code)
        return CompiledScript(code, ScriptLanguage.LUA, chunk)
    }
    
    fun registerAPI(name: String, api: Any) {
        globals.set(name, CoerceJavaToLua.coerce(api))
    }
}

/**
 * PythonScriptEngine - Motor Python (requiere Chaquopy)
 */
class PythonScriptEngine {
    
    // TODO: Integrar Chaquopy para Python en Android
    private var pythonInstance: Any? = null
    
    fun execute(code: String, context: ScriptContext): ScriptResult {
        return ScriptResult(
            success = false,
            error = "Python support requires Chaquopy integration"
        )
    }
    
    fun compile(code: String): CompiledScript {
        return CompiledScript(code, ScriptLanguage.PYTHON, null)
    }
    
    fun registerAPI(name: String, api: Any) {
        // TODO
    }
}

/**
 * ScriptComponent - Componente para scripts
 */
data class ScriptComponent(
    var scriptId: String = "",
    var language: ScriptLanguage = ScriptLanguage.KOTLIN,
    var code: String = "",
    var enabled: Boolean = true,
    var runOnStart: Boolean = false,
    var runOnUpdate: Boolean = false
) : Component {
    override fun clone() = copy()
}

/**
 * Lenguajes soportados
 */
enum class ScriptLanguage {
    KOTLIN,
    JAVA,
    JAVASCRIPT,
    LUA,
    PYTHON
}

/**
 * Contexto de ejecución del script
 */
data class ScriptContext(
    val entity: Long = 0,
    val deltaTime: Float = 0f,
    val customData: Map<String, Any> = emptyMap()
)

/**
 * Resultado de ejecución
 */
data class ScriptResult(
    val success: Boolean,
    val returnValue: Any? = null,
    val error: String? = null
)

/**
 * Script compilado
 */
data class CompiledScript(
    val code: String,
    val language: ScriptLanguage,
    val compiledData: Any?
)

/**
 * ScriptAPI - API expuesta a scripts
 */
class ScriptAPI(
    private val entityManager: EntityManager
) {
    
    // Transform
    fun getPosition(entity: Long): com.quantum.engine.math.Vector3? {
        val e = Entity(entity)
        return entityManager.getComponent<com.quantum.engine.core.components.TransformComponent>(e)?.localPosition
    }
    
    fun setPosition(entity: Long, x: Float, y: Float, z: Float) {
        val e = Entity(entity)
        entityManager.getComponent<com.quantum.engine.core.components.TransformComponent>(e)?.let {
            it.localPosition = com.quantum.engine.math.Vector3(x, y, z)
            it.isDirty = true
        }
    }
    
    fun rotate(entity: Long, x: Float, y: Float, z: Float) {
        val e = Entity(entity)
        entityManager.getComponent<com.quantum.engine.core.components.TransformComponent>(e)?.rotate(
            com.quantum.engine.math.Vector3(x, y, z)
        )
    }
    
    // Physics
    fun addForce(entity: Long, x: Float, y: Float, z: Float) {
        val e = Entity(entity)
        entityManager.getComponent<com.quantum.engine.physics.RigidbodyComponent>(e)?.addForce(
            com.quantum.engine.math.Vector3(x, y, z)
        )
    }
    
    // Instantiate
    fun instantiate(prefabName: String): Long {
        val entity = entityManager.createEntity(prefabName)
        return entity.id
    }
    
    // Destroy
    fun destroy(entity: Long) {
        entityManager.destroyEntity(Entity(entity))
    }
    
    // Input (simplificado)
    fun getKey(keyCode: Int): Boolean {
        // TODO: Integrar con InputSystem
        return false
    }
    
    // Time
    fun getTime(): Float {
        return System.currentTimeMillis() / 1000f
    }
    
    // Debug
    fun log(message: String) {
        Timber.d("Script: $message")
    }
}

/**
 * Ejemplos de scripts en diferentes lenguajes
 */
object ScriptExamples {
    
    // Kotlin
    val kotlinExample = """
        // Rotar objeto
        val transform = getPosition(entity)
        rotate(entity, 0f, 90f * deltaTime, 0f)
        
        // Aplicar fuerza
        if (getKey(32)) { // Espacio
            addForce(entity, 0f, 10f, 0f)
        }
        
        log("Position: ${'$'}transform")
    """.trimIndent()
    
    // JavaScript
    val jsExample = """
        // Rotar objeto
        var pos = getPosition(entity);
        rotate(entity, 0, 90 * deltaTime, 0);
        
        // Aplicar fuerza
        if (getKey(32)) {
            addForce(entity, 0, 10, 0);
        }
        
        log("Position: " + pos);
    """.trimIndent()
    
    // Lua
    val luaExample = """
        -- Rotar objeto
        local pos = getPosition(entity)
        rotate(entity, 0, 90 * deltaTime, 0)
        
        -- Aplicar fuerza
        if getKey(32) then
            addForce(entity, 0, 10, 0)
        end
        
        log("Position: " .. pos)
    """.trimIndent()
    
    // Python
    val pythonExample = """
# Rotar objeto
pos = getPosition(entity)
rotate(entity, 0, 90 * deltaTime, 0)

# Aplicar fuerza
if getKey(32):
    addForce(entity, 0, 10, 0)

log(f"Position: {pos}")
    """.trimIndent()
}
