package com.quantum.engine.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quantum.engine.core.*
import com.quantum.engine.core.ecs.*
import com.quantum.engine.editor.ui.*
import timber.log.Timber

/**
 * EditorActivity - Editor principal tipo Unity/Unreal
 */
class EditorActivity : ComponentActivity() {
    
    private lateinit var editorState: EditorState
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.plant(Timber.DebugTree())
        
        // Crear estado del editor
        editorState = EditorState()
        
        setContent {
            QuantumEditorTheme {
                QuantumEditor(editorState)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        editorState.shutdown()
    }
}

/**
 * EditorState - Estado global del editor
 */
class EditorState {
    
    val engine: QuantumEngine = QuantumEngine.builder()
        .config {
            fixedTimeStep = 1f / 60f
            targetFPS = 60
        }
        .build()
    
    val entityManager: EntityManager get() = engine.entityManager
    
    var selectedEntity by mutableStateOf<Entity?>(null)
    var currentTool by mutableStateOf(EditorTool.SELECT)
    var isPlaying by mutableStateOf(false)
    var showGrid by mutableStateOf(true)
    var showGizmos by mutableStateOf(true)
    
    // Paneles
    var showHierarchy by mutableStateOf(true)
    var showInspector by mutableStateOf(true)
    var showProject by mutableStateOf(true)
    var showConsole by mutableStateOf(true)
    var showProfiler by mutableStateOf(false)
    
    // Proyecto
    val projectName = mutableStateOf("Untitled Project")
    val sceneName = mutableStateOf("Main Scene")
    
    init {
        engine.initialize()
        createDefaultScene()
    }
    
    private fun createDefaultScene() {
        // Crear cámara principal
        val camera = entityManager.createEntity("Main Camera")
        // TODO: Añadir componentes
        
        // Luz direccional
        val light = entityManager.createEntity("Directional Light")
        // TODO: Añadir componentes
    }
    
    fun play() {
        isPlaying = true
        engine.start()
    }
    
    fun pause() {
        isPlaying = false
        engine.pause()
    }
    
    fun stop() {
        isPlaying = false
        engine.stop()
        createDefaultScene() // Resetear escena
    }
    
    fun shutdown() {
        engine.shutdown()
    }
}

enum class EditorTool {
    SELECT,
    MOVE,
    ROTATE,
    SCALE,
    TERRAIN,
    PAINT
}
