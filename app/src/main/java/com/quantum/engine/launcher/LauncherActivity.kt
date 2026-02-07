package com.quantum.engine.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.quantum.engine.editor.MobileEditorActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * LauncherActivity - Pantalla de inicio del motor
 * 
 * Features:
 * - Gestión de proyectos
 * - Crear nuevo proyecto
 * - Abrir proyecto existente
 * - Importar proyecto
 * - Eliminar proyecto
 * - Templates de proyecto
 * - Proyectos recientes
 * - Configuración global
 */
class LauncherActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LauncherTheme {
                LauncherScreen(
                    onOpenProject = { project ->
                        openEditor(project)
                    },
                    onCreateProject = { template ->
                        createAndOpenProject(template)
                    }
                )
            }
        }
    }
    
    private fun openEditor(project: Project) {
        val intent = Intent(this, MobileEditorActivity::class.java).apply {
            putExtra("PROJECT_PATH", project.path)
            putExtra("PROJECT_NAME", project.name)
        }
        startActivity(intent)
    }
    
    private fun createAndOpenProject(template: ProjectTemplate) {
        // TODO: Crear proyecto desde template
        val project = Project(
            name = "New Project",
            path = "/sdcard/QuantumEngine/Projects/NewProject",
            template = template,
            lastModified = System.currentTimeMillis()
        )
        openEditor(project)
    }
}

/**
 * LauncherScreen - Pantalla principal del launcher
 */
@Composable
fun LauncherScreen(
    onOpenProject: (Project) -> Unit,
    onCreateProject: (ProjectTemplate) -> Unit
) {
    var selectedTab by remember { mutableStateOf(LauncherTab.PROJECTS) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // Estado
    val projects = remember { mutableStateListOf<Project>() }
    val recentProjects = remember { mutableStateListOf<Project>() }
    
    LaunchedEffect(Unit) {
        // Cargar proyectos
        loadProjects(projects, recentProjects)
    }
    
    Scaffold(
        containerColor = LauncherColors.Background,
        topBar = {
            LauncherTopBar(
                onNewProject = { showNewProjectDialog = true },
                onImport = { showImportDialog = true },
                onSettings = { showSettingsDialog = true }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Hero section
            LauncherHero()
            
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = LauncherColors.Surface
            ) {
                Tab(
                    selected = selectedTab == LauncherTab.PROJECTS,
                    onClick = { selectedTab = LauncherTab.PROJECTS },
                    text = { Text("Projects") },
                    icon = { Icon(Icons.Default.Folder, null) }
                )
                Tab(
                    selected = selectedTab == LauncherTab.TEMPLATES,
                    onClick = { selectedTab = LauncherTab.TEMPLATES },
                    text = { Text("Templates") },
                    icon = { Icon(Icons.Default.Apps, null) }
                )
                Tab(
                    selected = selectedTab == LauncherTab.LEARN,
                    onClick = { selectedTab = LauncherTab.LEARN },
                    text = { Text("Learn") },
                    icon = { Icon(Icons.Default.School, null) }
                )
            }
            
            // Contenido
            when (selectedTab) {
                LauncherTab.PROJECTS -> {
                    ProjectsTab(
                        projects = projects,
                        recentProjects = recentProjects,
                        onOpenProject = onOpenProject,
                        onDeleteProject = { project ->
                            projects.remove(project)
                            recentProjects.remove(project)
                        }
                    )
                }
                LauncherTab.TEMPLATES -> {
                    TemplatesTab(
                        onSelectTemplate = { template ->
                            onCreateProject(template)
                        }
                    )
                }
                LauncherTab.LEARN -> {
                    LearnTab()
                }
            }
        }
    }
    
    // Dialogs
    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, template ->
                showNewProjectDialog = false
                onCreateProject(template)
            }
        )
    }
    
    if (showImportDialog) {
        ImportProjectDialog(
            onDismiss = { showImportDialog = false },
            onImport = { path ->
                showImportDialog = false
                // TODO: Importar proyecto
            }
        )
    }
    
    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }
}

/**
 * Hero section con logo y versión
 */
@Composable
fun LauncherHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LauncherColors.Primary,
                        LauncherColors.PrimaryDark
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Logo (emoji temporal, reemplazar con imagen)
            Text(
                "🎮",
                fontSize = 64.sp
            )
            
            Text(
                "QUANTUM ENGINE",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                "Version 3.0.0 Final",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Top bar del launcher
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherTopBar(
    onNewProject: () -> Unit,
    onImport: () -> Unit,
    onSettings: () -> Unit
) {
    TopAppBar(
        title = { },
        actions = {
            IconButton(onClick = onNewProject) {
                Icon(Icons.Default.Add, "New Project")
            }
            IconButton(onClick = onImport) {
                Icon(Icons.Default.Upload, "Import")
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

/**
 * Tab de proyectos
 */
@Composable
fun ProjectsTab(
    projects: List<Project>,
    recentProjects: List<Project>,
    onOpenProject: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Proyectos recientes
        if (recentProjects.isNotEmpty()) {
            item {
                Text(
                    "Recent Projects",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(recentProjects) { project ->
                ProjectCard(
                    project = project,
                    onOpen = { onOpenProject(project) },
                    onDelete = { onDeleteProject(project) }
                )
            }
        }
        
        // Todos los proyectos
        item {
            Text(
                "All Projects",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (projects.isEmpty()) {
            item {
                EmptyProjectsState()
            }
        } else {
            items(projects) { project ->
                ProjectCard(
                    project = project,
                    onOpen = { onOpenProject(project) },
                    onDelete = { onDeleteProject(project) }
                )
            }
        }
    }
}

/**
 * Card de proyecto
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCard(
    project: Project,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = LauncherColors.Surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Info del proyecto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        project.template.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = LauncherColors.TextSecondary
                    )
                }
                
                Text(
                    "Modified: ${formatDate(project.lastModified)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherColors.TextSecondary
                )
            }
            
            // Icono del template
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(project.template.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    project.template.icon,
                    fontSize = 40.sp
                )
            }
            
            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Options")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        onClick = {
                            showMenu = false
                            onOpen()
                        },
                        leadingIcon = { Icon(Icons.Default.Launch, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Show in Files") },
                        onClick = { showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Folder, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }
        }
    }
}

/**
 * Estado vacío
 */
@Composable
fun EmptyProjectsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = LauncherColors.TextSecondary
        )
        
        Text(
            "No projects yet",
            style = MaterialTheme.typography.titleMedium,
            color = LauncherColors.TextSecondary
        )
        
        Text(
            "Create your first project to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = LauncherColors.TextSecondary
        )
    }
}

/**
 * Tab de templates
 */
@Composable
fun TemplatesTab(
    onSelectTemplate: (ProjectTemplate) -> Unit
) {
    val templates = remember {
        listOf(
            ProjectTemplate("3D Game", "3D", "🎮", Color(0xFF2196F3)),
            ProjectTemplate("2D Game", "2D", "🕹️", Color(0xFF4CAF50)),
            ProjectTemplate("VR Experience", "VR", "🥽", Color(0xFF9C27B0)),
            ProjectTemplate("Mobile Game", "Mobile", "📱", Color(0xFFFF9800)),
            ProjectTemplate("Multiplayer", "Network", "🌐", Color(0xFFF44336)),
            ProjectTemplate("Blank Project", "Empty", "📄", Color(0xFF607D8B))
        )
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Project Templates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(templates.chunked(2)) { rowTemplates ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowTemplates.forEach { template ->
                    TemplateCard(
                        template = template,
                        onSelect = { onSelectTemplate(template) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (rowTemplates.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateCard(
    template: ProjectTemplate,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelect,
        modifier = modifier.height(150.dp),
        colors = CardDefaults.cardColors(
            containerColor = LauncherColors.Surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(template.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    template.icon,
                    fontSize = 32.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                template.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                template.description,
                style = MaterialTheme.typography.bodySmall,
                color = LauncherColors.TextSecondary
            )
        }
    }
}

/**
 * Tab de aprendizaje
 */
@Composable
fun LearnTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Learn Quantum Engine",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Tutoriales
        items(
            listOf(
                LearnItem("Getting Started", "🚀", "Learn the basics"),
                LearnItem("3D Basics", "🎨", "Create your first 3D scene"),
                LearnItem("Physics", "⚽", "Add realistic physics"),
                LearnItem("Scripting", "💻", "Write game logic"),
                LearnItem("UI Design", "🖼️", "Create game interfaces"),
                LearnItem("Audio", "🔊", "Add sound effects")
            )
        ) { item ->
            LearnCard(item)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnCard(item: LearnItem) {
    Card(
        onClick = { },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LauncherColors.Surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(item.icon, fontSize = 32.sp)
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherColors.TextSecondary
                )
            }
            
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

// Dialogs y helpers en el siguiente archivo...

/**
 * Modelos de datos
 */
data class Project(
    val name: String,
    val path: String,
    val template: ProjectTemplate,
    val lastModified: Long
)

data class ProjectTemplate(
    val name: String,
    val description: String,
    val icon: String,
    val color: Color
)

data class LearnItem(
    val title: String,
    val icon: String,
    val description: String
)

enum class LauncherTab {
    PROJECTS,
    TEMPLATES,
    LEARN
}

object LauncherColors {
    val Background = Color(0xFF0D1117)
    val Surface = Color(0xFF161B22)
    val Primary = Color(0xFF2196F3)
    val PrimaryDark = Color(0xFF1976D2)
    val TextSecondary = Color(0xFF8B949E)
}

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LauncherColors.Primary,
            surface = LauncherColors.Surface,
            background = LauncherColors.Background
        ),
        content = content
    )
}

fun loadProjects(
    projects: MutableList<Project>,
    recentProjects: MutableList<Project>
) {
    // TODO: Cargar desde almacenamiento
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Stubs para dialogs
@Composable fun NewProjectDialog(onDismiss: () -> Unit, onCreate: (String, ProjectTemplate) -> Unit) {}
@Composable fun ImportProjectDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {}
@Composable fun SettingsDialog(onDismiss: () -> Unit) {}
