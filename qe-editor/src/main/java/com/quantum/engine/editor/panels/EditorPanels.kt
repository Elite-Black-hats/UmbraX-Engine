package com.quantum.engine.editor.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.quantum.engine.editor.EditorState
import com.quantum.engine.editor.ui.EditorColors
import com.quantum.engine.core.ecs.Entity

/**
 * InspectorPanel - Panel de inspector tipo Unity
 */
@Composable
fun InspectorPanel(
    state: EditorState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = EditorColors.Panel
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Inspector",
                    style = MaterialTheme.typography.titleSmall,
                    color = EditorColors.Text
                )
            }
            
            Divider(color = EditorColors.Border)
            
            // Contenido
            state.selectedEntity?.let { entity ->
                InspectorContent(entity, state)
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No object selected",
                        color = EditorColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun InspectorContent(entity: Entity, state: EditorState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Nombre y tag
        item {
            ComponentSection(title = "GameObject") {
                val metadata = state.entityManager.getMetadata(entity)
                
                OutlinedTextField(
                    value = metadata?.name ?: "",
                    onValueChange = { metadata?.name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = metadata?.tag ?: "",
                    onValueChange = { metadata?.tag = it },
                    label = { Text("Tag") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        
        // Transform
        item {
            TransformComponent(entity, state)
        }
        
        // Otros componentes
        item {
            ComponentSection(title = "Add Component") {
                Button(
                    onClick = { /* TODO: Menú de componentes */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Component")
                }
            }
        }
    }
}

@Composable
fun ComponentSection(
    title: String,
    collapsible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EditorColors.Surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = EditorColors.Text
                )
                
                if (collapsible) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
fun TransformComponent(entity: Entity, state: EditorState) {
    ComponentSection(title = "Transform") {
        // Position
        Vector3Field(
            label = "Position",
            value = FloatArray(3) { 0f },
            onValueChange = { }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Rotation
        Vector3Field(
            label = "Rotation",
            value = FloatArray(3) { 0f },
            onValueChange = { }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Scale
        Vector3Field(
            label = "Scale",
            value = FloatArray(3) { 1f },
            onValueChange = { }
        )
    }
}

@Composable
fun Vector3Field(
    label: String,
    value: FloatArray,
    onValueChange: (FloatArray) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = EditorColors.TextSecondary)
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("X", "Y", "Z").forEachIndexed { index, axis ->
                OutlinedTextField(
                    value = value[index].toString(),
                    onValueChange = { /* TODO */ },
                    label = { Text(axis) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

/**
 * SceneViewPanel - Vista 3D de la escena
 */
@Composable
fun SceneViewPanel(
    state: EditorState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = EditorColors.Background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // TODO: Integrar GLSurfaceView
            
            // Overlay con stats
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                Text("FPS: 60", color = Color.White, fontFamily = FontFamily.Monospace)
                Text("Objects: ${state.entityManager.entityCount}", color = Color.White, fontFamily = FontFamily.Monospace)
            }
            
            // Gizmos en overlay
            if (state.showGizmos) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(80.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    // TODO: Gizmo de ejes
                }
            }
        }
    }
}

/**
 * ProjectPanel - Navegador de assets
 */
@Composable
fun ProjectPanel(
    state: EditorState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = EditorColors.Panel
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Project",
                    style = MaterialTheme.typography.titleSmall,
                    color = EditorColors.Text
                )
                
                Row {
                    IconButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                    }
                    
                    IconButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = "Import")
                    }
                }
            }
            
            Divider(color = EditorColors.Border)
            
            // Navegación de carpetas
            Row(modifier = Modifier.fillMaxSize()) {
                // Árbol de carpetas (izquierda)
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .fillMaxHeight()
                        .background(EditorColors.Surface)
                ) {
                    FolderTreeItem("Assets", 0, true)
                    FolderTreeItem("Scenes", 1, false)
                    FolderTreeItem("Scripts", 1, false)
                    FolderTreeItem("Materials", 1, false)
                    FolderTreeItem("Textures", 1, false)
                    FolderTreeItem("Models", 1, false)
                    FolderTreeItem("Audio", 1, false)
                }
                
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = EditorColors.Border
                )
                
                // Grid de assets (derecha)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(80.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // TODO: Assets
                }
            }
        }
    }
}

@Composable
fun FolderTreeItem(name: String, level: Int, selected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(if (selected) EditorColors.AccentBlue.copy(0.3f) else Color.Transparent)
            .padding(start = (level * 16).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = EditorColors.AccentOrange
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(name, style = MaterialTheme.typography.bodySmall, color = EditorColors.Text)
    }
}

/**
 * ConsolePanel - Consola de logs
 */
@Composable
fun ConsolePanel(
    state: EditorState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = EditorColors.Panel
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { /* TODO: Clear */ },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                    
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("Info", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("Warning", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("Error", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            
            Divider(color = EditorColors.Border)
            
            // Lista de logs
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Logs de ejemplo
                item {
                    ConsoleLogItem(
                        type = LogType.INFO,
                        message = "Quantum Engine initialized",
                        timestamp = "00:00:01"
                    )
                }
                
                item {
                    ConsoleLogItem(
                        type = LogType.INFO,
                        message = "Scene loaded: Main Scene",
                        timestamp = "00:00:02"
                    )
                }
            }
        }
    }
}

@Composable
fun ConsoleLogItem(type: LogType, message: String, timestamp: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (type) {
                LogType.INFO -> Icons.Default.Info
                LogType.WARNING -> Icons.Default.Warning
                LogType.ERROR -> Icons.Default.Error
            },
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = when (type) {
                LogType.INFO -> EditorColors.AccentBlue
                LogType.WARNING -> EditorColors.Warning
                LogType.ERROR -> EditorColors.Error
            }
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            timestamp,
            style = MaterialTheme.typography.bodySmall,
            color = EditorColors.TextSecondary,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = EditorColors.Text,
            fontFamily = FontFamily.Monospace
        )
    }
}

enum class LogType {
    INFO,
    WARNING,
    ERROR
}
