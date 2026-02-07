package com.quantum.engine.editor.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quantum.engine.core.ecs.Entity
import com.quantum.engine.editor.EditorState
import com.quantum.engine.editor.ui.EditorColors

/**
 * HierarchyPanel - Panel de jerarquía tipo Unity
 * 
 * Muestra todos los objetos de la escena en forma de árbol
 */
@Composable
fun HierarchyPanel(
    state: EditorState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = EditorColors.Panel,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            HierarchyHeader(state)
            
            Divider(color = EditorColors.Border)
            
            // Lista de entidades
            HierarchyList(state)
        }
    }
}

@Composable
fun HierarchyHeader(state: EditorState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Hierarchy",
            style = MaterialTheme.typography.titleSmall,
            color = EditorColors.Text
        )
        
        Row {
            IconButton(
                onClick = { /* TODO: Crear objeto */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create",
                    tint = EditorColors.Text,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            IconButton(
                onClick = { /* TODO: Buscar */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = EditorColors.Text,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HierarchyList(state: EditorState) {
    val entities = remember(state.entityManager.entityCount) {
        getAllEntities(state)
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(entities) { entity ->
            HierarchyItem(
                entity = entity,
                state = state,
                level = 0
            )
        }
        
        // Mensaje si no hay entidades
        if (entities.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Empty scene\nClick + to create objects",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EditorColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun HierarchyItem(
    entity: Entity,
    state: EditorState,
    level: Int
) {
    val metadata = state.entityManager.getMetadata(entity)
    val isSelected = state.selectedEntity == entity
    var isExpanded by remember { mutableStateOf(true) }
    
    Column {
        // Item principal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    if (isSelected) EditorColors.AccentBlue.copy(alpha = 0.3f)
                    else Color.Transparent
                )
                .clickable { state.selectedEntity = entity }
                .padding(start = (level * 16).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de expansión si tiene hijos
            if (metadata?.children?.isNotEmpty() == true) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = "Expand",
                        tint = EditorColors.Text,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(20.dp))
            }
            
            // Icono de tipo de objeto
            Icon(
                getEntityIcon(entity, state),
                contentDescription = null,
                tint = EditorColors.Text,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Nombre
            Text(
                metadata?.name ?: "Entity ${entity.id}",
                style = MaterialTheme.typography.bodySmall,
                color = if (metadata?.active == true) EditorColors.Text else EditorColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Botón de activar/desactivar
            Checkbox(
                checked = metadata?.active ?: true,
                onCheckedChange = { metadata?.active = it },
                modifier = Modifier.size(16.dp)
            )
        }
        
        // Hijos (si está expandido)
        if (isExpanded) {
            metadata?.children?.forEach { child ->
                HierarchyItem(
                    entity = child,
                    state = state,
                    level = level + 1
                )
            }
        }
    }
}

fun getEntityIcon(entity: Entity, state: EditorState): androidx.compose.ui.graphics.vector.ImageVector {
    // Determinar icono basado en componentes
    // TODO: Chequear componentes reales
    return Icons.Default.Widgets // Por defecto
}

fun getAllEntities(state: EditorState): List<Entity> {
    // TODO: Obtener todas las entidades sin parent
    return emptyList()
}
