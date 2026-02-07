package com.quantum.engine.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quantum.engine.editor.*
import com.quantum.engine.editor.panels.*

/**
 * QuantumEditor - Layout principal del editor
 * 
 * Layout tipo Unity:
 * ┌─────────────────────────────────────┐
 * │           Toolbar                    │
 * ├──────────┬──────────────┬───────────┤
 * │          │              │           │
 * │Hierarchy │  Scene View  │ Inspector │
 * │          │              │           │
 * │          │              │           │
 * ├──────────┴──────────────┴───────────┤
 * │         Project Browser              │
 * ├──────────────────────────────────────┤
 * │            Console                   │
 * └──────────────────────────────────────┘
 */
@Composable
fun QuantumEditor(state: EditorState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorColors.Background)
    ) {
        // Toolbar superior
        EditorToolbar(state)
        
        // Área principal (Hierarchy + Scene + Inspector)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f)
        ) {
            // Panel izquierdo: Hierarchy
            if (state.showHierarchy) {
                HierarchyPanel(
                    state = state,
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight()
                )
            }
            
            // Panel central: Scene View
            SceneViewPanel(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            
            // Panel derecho: Inspector
            if (state.showInspector) {
                InspectorPanel(
                    state = state,
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                )
            }
        }
        
        // Panel inferior: Project + Console
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f)
        ) {
            // Project Browser
            if (state.showProject) {
                ProjectPanel(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
            
            // Console
            if (state.showConsole) {
                ConsolePanel(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
        }
    }
}

/**
 * Toolbar superior con controles de play/pause
 */
@Composable
fun EditorToolbar(state: EditorState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        color = EditorColors.Toolbar,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lado izquierdo: Herramientas
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Selector de herramientas
                EditorToolButton(
                    icon = Icons.Default.NearMe,
                    selected = state.currentTool == EditorTool.SELECT,
                    tooltip = "Select (Q)"
                ) {
                    state.currentTool = EditorTool.SELECT
                }
                
                EditorToolButton(
                    icon = Icons.Default.OpenWith,
                    selected = state.currentTool == EditorTool.MOVE,
                    tooltip = "Move (W)"
                ) {
                    state.currentTool = EditorTool.MOVE
                }
                
                EditorToolButton(
                    icon = Icons.Default.Rotate90DegreesCcw,
                    selected = state.currentTool == EditorTool.ROTATE,
                    tooltip = "Rotate (E)"
                ) {
                    state.currentTool = EditorTool.ROTATE
                }
                
                EditorToolButton(
                    icon = Icons.Default.ZoomOutMap,
                    selected = state.currentTool == EditorTool.SCALE,
                    tooltip = "Scale (R)"
                ) {
                    state.currentTool = EditorTool.SCALE
                }
            }
            
            // Centro: Play/Pause/Stop
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { state.play() },
                    enabled = !state.isPlaying
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (state.isPlaying) Color.Green else Color.White
                    )
                }
                
                IconButton(
                    onClick = { state.pause() },
                    enabled = state.isPlaying
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                }
                
                IconButton(
                    onClick = { state.stop() }
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                }
            }
            
            // Lado derecho: Configuración
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { /* TODO: Settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                
                IconButton(onClick = { /* TODO: Layouts */ }) {
                    Icon(Icons.Default.ViewQuilt, contentDescription = "Layouts")
                }
            }
        }
    }
}

@Composable
fun EditorToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    tooltip: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            icon,
            contentDescription = tooltip,
            tint = if (selected) EditorColors.AccentBlue else Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Tema del editor
 */
@Composable
fun QuantumEditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = EditorColors.AccentBlue,
            secondary = EditorColors.AccentOrange,
            background = EditorColors.Background,
            surface = EditorColors.Surface
        )
    ) {
        content()
    }
}

/**
 * Colores del editor tipo Unity/Unreal
 */
object EditorColors {
    val Background = Color(0xFF1E1E1E)
    val Surface = Color(0xFF2D2D2D)
    val Toolbar = Color(0xFF3C3C3C)
    val Panel = Color(0xFF252526)
    val Border = Color(0xFF3F3F46)
    val AccentBlue = Color(0xFF2196F3)
    val AccentOrange = Color(0xFFFF9800)
    val Text = Color(0xFFCCCCCC)
    val TextSecondary = Color(0xFF808080)
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFC107)
    val Error = Color(0xFFF44336)
}
