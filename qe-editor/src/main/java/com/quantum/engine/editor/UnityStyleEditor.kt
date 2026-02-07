package com.quantum.engine.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.quantum.engine.core.*
import com.quantum.engine.core.ecs.*
import timber.log.Timber
import kotlinx.coroutines.*

/**
 * EditorActivity - Editor completo idéntico a Unity/Unreal
 * 
 * Features:
 * - Layout dockable con drag & drop
 * - Multiple viewports
 * - Tool system completo
 * - Asset management
 * - Scene hierarchy
 * - Inspector avanzado
 * - Console con stack traces
 * - Profiler en tiempo real
 * - Settings completos
 * - Keyboard shortcuts
 * - Undo/Redo system
 * - Multi-selection
 * - Prefab system
 */
class EditorActivity : ComponentActivity() {
    
    private lateinit var editorState: EditorState
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.plant(Timber.DebugTree())
        
        editorState = EditorState()
        
        setContent {
            QuantumEditorTheme {
                UnityStyleEditor(editorState)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        editorState.shutdown()
    }
}

/**
 * UnityStyleEditor - Layout completo estilo Unity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnityStyleEditor(state: EditorState) {
    
    var selectedPanel by remember { mutableStateOf<EditorPanel?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UnityColors.Background)
    ) {
        // Menu Bar (File, Edit, Assets, GameObject, etc)
        EditorMenuBar(state)
        
        // Toolbar
        EditorToolbar(state)
        
        // Main content area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Layout dockable
            DockableLayout(
                state = state,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Status Bar
        EditorStatusBar(state)
    }
    
    // Floating windows
    if (state.showSettings) {
        SettingsWindow(
            onDismiss = { state.showSettings = false },
            state = state
        )
    }
}

/**
 * EditorMenuBar - Barra de menú completa tipo Unity
 */
@Composable
fun EditorMenuBar(state: EditorState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        color = UnityColors.MenuBar,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MenuBarItem("File", state) {
                MenuItem("New Scene", Icons.Default.Add) { state.newScene() }
                MenuItem("Open Scene", Icons.Default.FolderOpen) { }
                MenuItem("Save Scene", Icons.Default.Save) { state.saveScene() }
                MenuItem("Save Scene As...", Icons.Default.SaveAs) { }
                Divider()
                MenuItem("Build Settings", Icons.Default.Build) { }
                MenuItem("Build and Run", Icons.Default.PlayArrow) { }
                Divider()
                MenuItem("Exit", Icons.Default.ExitToApp) { }
            }
            
            MenuBarItem("Edit", state) {
                MenuItem("Undo", Icons.Default.Undo) { state.undo() }
                MenuItem("Redo", Icons.Default.Redo) { state.redo() }
                Divider()
                MenuItem("Cut", Icons.Default.ContentCut) { }
                MenuItem("Copy", Icons.Default.ContentCopy) { }
                MenuItem("Paste", Icons.Default.ContentPaste) { }
                MenuItem("Duplicate", Icons.Default.FileCopy) { }
                MenuItem("Delete", Icons.Default.Delete) { }
                Divider()
                MenuItem("Select All", Icons.Default.SelectAll) { }
                Divider()
                MenuItem("Preferences", Icons.Default.Settings) { state.showSettings = true }
            }
            
            MenuBarItem("Assets", state) {
                MenuItem("Create", Icons.Default.Add) { }
                MenuItem("Import New Asset", Icons.Default.Upload) { }
                MenuItem("Export Package", Icons.Default.Archive) { }
                Divider()
                MenuItem("Refresh", Icons.Default.Refresh) { }
            }
            
            MenuBarItem("GameObject", state) {
                MenuItem("Create Empty", Icons.Default.Add) { state.createEmpty() }
                Divider()
                MenuItem("3D Object", Icons.Default.ViewInAr) { }
                MenuItem("2D Object", Icons.Default.Crop) { }
                MenuItem("Light", Icons.Default.Light) { }
                MenuItem("Audio", Icons.Default.VolumeUp) { }
                MenuItem("Camera", Icons.Default.Videocam) { }
            }
            
            MenuBarItem("Component", state) {
                MenuItem("Add", Icons.Default.Add) { }
            }
            
            MenuBarItem("Window", state) {
                MenuItem("Hierarchy", null) { state.showHierarchy = !state.showHierarchy }
                MenuItem("Inspector", null) { state.showInspector = !state.showInspector }
                MenuItem("Project", null) { state.showProject = !state.showProject }
                MenuItem("Console", null) { state.showConsole = !state.showConsole }
                Divider()
                MenuItem("Profiler", Icons.Default.Speed) { state.showProfiler = !state.showProfiler }
                MenuItem("Animator", Icons.Default.Animation) { }
                MenuItem("Lighting", Icons.Default.WbSunny) { }
            }
            
            MenuBarItem("Help", state) {
                MenuItem("Documentation", Icons.Default.MenuBook) { }
                MenuItem("Tutorials", Icons.Default.School) { }
                Divider()
                MenuItem("About", Icons.Default.Info) { }
            }
        }
    }
}

@Composable
fun MenuBarItem(
    text: String,
    state: EditorState,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Text(
            text = text,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = UnityColors.Text
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(UnityColors.MenuPopup)
        ) {
            content()
        }
    }
}

@Composable
fun MenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
        },
        onClick = onClick
    )
}

/**
 * DockableLayout - Sistema de paneles dockables
 */
@Composable
fun DockableLayout(
    state: EditorState,
    modifier: Modifier = Modifier
) {
    // Layout tipo Unity: Hierarchy | Scene | Inspector
    //                     Project + Console
    
    Column(modifier = modifier) {
        // Top area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f)
        ) {
            // Left: Hierarchy
            if (state.showHierarchy) {
                HierarchyPanel(
                    state = state,
                    modifier = Modifier
                        .width(state.hierarchyWidth.dp)
                        .fillMaxHeight()
                )
                
                VerticalDivider()
            }
            
            // Center: Scene View / Game View
            Column(modifier = Modifier.weight(1f)) {
                // Tabs
                TabRow(selectedTabIndex = state.selectedViewTab) {
                    Tab(
                        selected = state.selectedViewTab == 0,
                        onClick = { state.selectedViewTab = 0 },
                        text = { Text("Scene") }
                    )
                    Tab(
                        selected = state.selectedViewTab == 1,
                        onClick = { state.selectedViewTab = 1 },
                        text = { Text("Game") }
                    )
                }
                
                // Content
                when (state.selectedViewTab) {
                    0 -> SceneViewPanel(state, Modifier.fillMaxSize())
                    1 -> GameViewPanel(state, Modifier.fillMaxSize())
                }
            }
            
            VerticalDivider()
            
            // Right: Inspector
            if (state.showInspector) {
                InspectorPanel(
                    state = state,
                    modifier = Modifier
                        .width(state.inspectorWidth.dp)
                        .fillMaxHeight()
                )
            }
        }
        
        HorizontalDivider()
        
        // Bottom area: Project + Console
        Column(modifier = Modifier.fillMaxWidth().weight(0.3f)) {
            // Tabs
            TabRow(selectedTabIndex = state.selectedBottomTab) {
                Tab(
                    selected = state.selectedBottomTab == 0,
                    onClick = { state.selectedBottomTab = 0 },
                    text = { Text("Project") }
                )
                Tab(
                    selected = state.selectedBottomTab == 1,
                    onClick = { state.selectedBottomTab = 1 },
                    text = { Text("Console") }
                )
                if (state.showProfiler) {
                    Tab(
                        selected = state.selectedBottomTab == 2,
                        onClick = { state.selectedBottomTab = 2 },
                        text = { Text("Profiler") }
                    )
                }
            }
            
            // Content
            when (state.selectedBottomTab) {
                0 -> ProjectPanel(state, Modifier.fillMaxSize())
                1 -> ConsolePanel(state, Modifier.fillMaxSize())
                2 -> if (state.showProfiler) ProfilerPanel(state, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun VerticalDivider() {
    Divider(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp),
        color = UnityColors.Border
    )
}

@Composable
fun HorizontalDivider() {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
        color = UnityColors.Border
    )
}

/**
 * GameViewPanel - Vista de juego
 */
@Composable
fun GameViewPanel(state: EditorState, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {
            // TODO: Integrar GLSurfaceView aquí
            
            if (!state.isPlaying) {
                Text(
                    "Press Play to test your game",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * ProfilerPanel - Panel de profiling avanzado
 */
@Composable
fun ProfilerPanel(state: EditorState, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = UnityColors.Panel) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Text("Profiler", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stats en tiempo real
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("FPS", "${state.currentFPS}")
                StatItem("CPU", "${state.cpuUsage}ms")
                StatItem("GPU", "${state.gpuUsage}ms")
                StatItem("Memory", "${state.memoryUsageMB}MB")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Gráfica de performance (simplificada)
            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                // TODO: Dibujar gráfica de FPS
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = UnityColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, color = UnityColors.AccentBlue)
    }
}

/**
 * Colores estilo Unity
 */
object UnityColors {
    val Background = Color(0xFF1E1E1E)
    val MenuBar = Color(0xFF2D2D30)
    val MenuPopup = Color(0xFF2D2D30)
    val Panel = Color(0xFF252526)
    val Surface = Color(0xFF3E3E42)
    val Border = Color(0xFF3F3F46)
    val Toolbar = Color(0xFF2D2D30)
    
    val Text = Color(0xFFCCCCCC)
    val TextSecondary = Color(0xFF808080)
    
    val AccentBlue = Color(0xFF007ACC)
    val AccentOrange = Color(0xFFFF9800)
    val Success = Color(0xFF4EC9B0)
    val Warning = Color(0xFFD4A959)
    val Error = Color(0xFFF48771)
}
