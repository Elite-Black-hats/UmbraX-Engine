package com.quantum.engine.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.quantum.engine.core.*

/**
 * MobileEditorActivity - Editor optimizado para móvil
 * 
 * Features móviles:
 * - Bottom navigation
 * - Floating action buttons
 * - Touch gestures (pinch, rotate, pan)
 * - Quick access toolbar
 * - Swipe panels
 * - Haptic feedback
 * - Portrait/Landscape layouts
 * - One-handed mode
 */
class MobileEditorActivity : ComponentActivity() {
    
    private lateinit var editorState: MobileEditorState
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        editorState = MobileEditorState()
        
        setContent {
            MobileEditorTheme {
                MobileEditor(editorState)
            }
        }
    }
}

/**
 * MobileEditor - UI principal optimizada para móvil
 */
@Composable
fun MobileEditor(state: MobileEditorState) {
    val haptic = LocalHapticFeedback.current
    
    Scaffold(
        containerColor = MobileColors.Background,
        topBar = {
            MobileTopBar(state, haptic)
        },
        bottomBar = {
            MobileBottomNavigation(state, haptic)
        },
        floatingActionButton = {
            MobileFloatingActions(state, haptic)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Contenido principal según panel activo
            when (state.activePanel) {
                MobilePanel.SCENE -> MobileSceneView(state)
                MobilePanel.HIERARCHY -> MobileHierarchyView(state)
                MobilePanel.INSPECTOR -> MobileInspectorView(state)
                MobilePanel.ASSETS -> MobileAssetsView(state)
                MobilePanel.SETTINGS -> MobileSettingsView(state)
            }
            
            // Quick access toolbar flotante
            QuickAccessToolbar(
                state = state,
                haptic = haptic,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
            
            // Play controls flotantes
            if (state.showPlayControls) {
                PlayControlsOverlay(
                    state = state,
                    haptic = haptic,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

/**
 * Top Bar móvil compacta
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileTopBar(
    state: MobileEditorState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    state.sceneName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                if (state.unsavedChanges) {
                    Text(
                        "● Unsaved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MobileColors.Warning
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.showMenu = !state.showMenu
                }
            ) {
                Icon(Icons.Default.Menu, "Menu")
            }
        },
        actions = {
            // Save
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.save()
                }
            ) {
                Icon(Icons.Default.Save, "Save")
            }
            
            // Undo/Redo
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.undo()
                },
                enabled = state.canUndo
            ) {
                Icon(Icons.Default.Undo, "Undo")
            }
            
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.redo()
                },
                enabled = state.canRedo
            ) {
                Icon(Icons.Default.Redo, "Redo")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MobileColors.Surface
        )
    )
}

/**
 * Bottom Navigation móvil
 */
@Composable
fun MobileBottomNavigation(
    state: MobileEditorState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    NavigationBar(
        containerColor = MobileColors.Surface
    ) {
        // Scene
        NavigationBarItem(
            icon = { Icon(Icons.Default.Videocam, "Scene") },
            label = { Text("Scene") },
            selected = state.activePanel == MobilePanel.SCENE,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.activePanel = MobilePanel.SCENE
            }
        )
        
        // Hierarchy
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountTree, "Hierarchy") },
            label = { Text("Hierarchy") },
            selected = state.activePanel == MobilePanel.HIERARCHY,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.activePanel = MobilePanel.HIERARCHY
            }
        )
        
        // Inspector
        NavigationBarItem(
            icon = { Icon(Icons.Default.Tune, "Inspector") },
            label = { Text("Inspector") },
            selected = state.activePanel == MobilePanel.INSPECTOR,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.activePanel = MobilePanel.INSPECTOR
            }
        )
        
        // Assets
        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, "Assets") },
            label = { Text("Assets") },
            selected = state.activePanel == MobilePanel.ASSETS,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.activePanel = MobilePanel.ASSETS
            }
        )
        
        // Settings
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, "Settings") },
            label = { Text("Settings") },
            selected = state.activePanel == MobilePanel.SETTINGS,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.activePanel = MobilePanel.SETTINGS
            }
        )
    }
}

/**
 * Floating Action Buttons
 */
@Composable
fun MobileFloatingActions(
    state: MobileEditorState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botones expandidos
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Crear GameObject
                SmallFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.createGameObject()
                    },
                    containerColor = MobileColors.Success
                ) {
                    Icon(Icons.Default.Add, "Add GameObject")
                }
                
                // Crear luz
                SmallFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.createLight()
                    },
                    containerColor = MobileColors.Warning
                ) {
                    Icon(Icons.Default.Light, "Add Light")
                }
                
                // Crear cámara
                SmallFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.createCamera()
                    },
                    containerColor = MobileColors.Info
                ) {
                    Icon(Icons.Default.Camera, "Add Camera")
                }
            }
        }
        
        // Botón principal
        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = !expanded
            },
            containerColor = MobileColors.Primary
        ) {
            Icon(
                if (expanded) Icons.Default.Close else Icons.Default.Add,
                "Actions"
            )
        }
    }
}

/**
 * Quick Access Toolbar flotante
 */
@Composable
fun QuickAccessToolbar(
    state: MobileEditorState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(MobileColors.Surface, RoundedCornerShape(24.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tool selector
        listOf(
            Icons.Default.NearMe to EditorTool.SELECT,
            Icons.Default.OpenWith to EditorTool.MOVE,
            Icons.Default.Rotate90DegreesCcw to EditorTool.ROTATE,
            Icons.Default.ZoomOutMap to EditorTool.SCALE
        ).forEach { (icon, tool) ->
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.currentTool = tool
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.currentTool == tool) MobileColors.Primary
                        else Color.Transparent
                    )
            ) {
                Icon(
                    icon,
                    tool.name,
                    tint = if (state.currentTool == tool) Color.White
                    else MobileColors.OnSurface
                )
            }
        }
        
        Divider()
        
        // View options
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.showGrid = !state.showGrid
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        ) {
            Icon(
                Icons.Default.GridOn,
                "Grid",
                tint = if (state.showGrid) MobileColors.Primary
                else MobileColors.OnSurface
            )
        }
        
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.showGizmos = !state.showGizmos
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        ) {
            Icon(
                Icons.Default.Apps,
                "Gizmos",
                tint = if (state.showGizmos) MobileColors.Primary
                else MobileColors.OnSurface
            )
        }
    }
}

/**
 * Play Controls Overlay
 */
@Composable
fun PlayControlsOverlay(
    state: MobileEditorState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(MobileColors.Surface, RoundedCornerShape(24.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Play
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.play()
            },
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (state.isPlaying) MobileColors.Success else Color.Transparent)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                "Play",
                tint = if (state.isPlaying) Color.White else MobileColors.OnSurface,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Pause
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.pause()
            },
            enabled = state.isPlaying,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        ) {
            Icon(
                Icons.Default.Pause,
                "Pause",
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Stop
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                state.stop()
            },
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        ) {
            Icon(
                Icons.Default.Stop,
                "Stop",
                tint = MobileColors.Error,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Scene View con touch controls
 */
@Composable
fun MobileSceneView(state: MobileEditorState) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotationChange ->
                    scale *= zoom
                    offset += pan
                    rotation += rotationChange
                    
                    // Actualizar cámara
                    state.updateCamera(scale, offset, rotation)
                }
            }
    ) {
        // TODO: Integrar Vulkan Surface aquí
        
        // Stats overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text("FPS: ${state.fps}", color = Color.White, fontSize = 12.sp)
            Text("Objects: ${state.objectCount}", color = Color.White, fontSize = 12.sp)
            Text("Draw Calls: ${state.drawCalls}", color = Color.White, fontSize = 12.sp)
        }
        
        // Touch hints
        if (state.showTouchHints) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text("👆 Tap: Select", color = Color.White)
                Text("🤏 Pinch: Zoom", color = Color.White)
                Text("✌️ Two fingers: Rotate", color = Color.White)
                Text("👉 Swipe: Pan", color = Color.White)
            }
        }
    }
}

/**
 * MobileEditorState
 */
class MobileEditorState {
    var activePanel by mutableStateOf(MobilePanel.SCENE)
    var currentTool by mutableStateOf(EditorTool.SELECT)
    var isPlaying by mutableStateOf(false)
    var showMenu by mutableStateOf(false)
    var showPlayControls by mutableStateOf(true)
    var showGrid by mutableStateOf(true)
    var showGizmos by mutableStateOf(true)
    var showTouchHints by mutableStateOf(true)
    
    var sceneName by mutableStateOf("Untitled Scene")
    var unsavedChanges by mutableStateOf(false)
    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)
    
    var fps by mutableStateOf(60)
    var objectCount by mutableStateOf(0)
    var drawCalls by mutableStateOf(0)
    
    fun save() { }
    fun undo() { }
    fun redo() { }
    fun play() { isPlaying = true }
    fun pause() { }
    fun stop() { isPlaying = false }
    
    fun createGameObject() { }
    fun createLight() { }
    fun createCamera() { }
    
    fun updateCamera(scale: Float, offset: Offset, rotation: Float) { }
}

enum class MobilePanel {
    SCENE,
    HIERARCHY,
    INSPECTOR,
    ASSETS,
    SETTINGS
}

/**
 * Colores móviles optimizados
 */
object MobileColors {
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1E1E1E)
    val Primary = Color(0xFF2196F3)
    val OnSurface = Color(0xFFE0E0E0)
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF00BCD4)
}

@Composable
fun MobileEditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = MobileColors.Primary,
            surface = MobileColors.Surface,
            background = MobileColors.Background
        )
    ) {
        content()
    }
}

// Stubs para otras vistas
@Composable fun MobileHierarchyView(state: MobileEditorState) { Box(Modifier.fillMaxSize()) }
@Composable fun MobileInspectorView(state: MobileEditorState) { Box(Modifier.fillMaxSize()) }
@Composable fun MobileAssetsView(state: MobileEditorState) { Box(Modifier.fillMaxSize()) }
@Composable fun MobileSettingsView(state: MobileEditorState) { Box(Modifier.fillMaxSize()) }
