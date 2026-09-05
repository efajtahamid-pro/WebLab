package com.efajtahamid.weblab.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.efajtahamid.weblab.WebLabApp
import com.efajtahamid.weblab.data.db.ProjectEntity
import com.efajtahamid.weblab.data.repository.FileRepository
import com.efajtahamid.weblab.runtime.NodeRuntimeManager
import com.efajtahamid.weblab.runtime.TerminalEngine
import com.efajtahamid.weblab.ui.editor.CodeEditorScreen
import com.efajtahamid.weblab.ui.editor.CodeEditorViewModel
import com.efajtahamid.weblab.ui.explorer.FileExplorerScreen
import com.efajtahamid.weblab.ui.explorer.FileExplorerViewModel
import com.efajtahamid.weblab.ui.preview.PreviewScreen
import com.efajtahamid.weblab.ui.preview.PreviewViewModel
import com.efajtahamid.weblab.ui.projects.ProjectsScreen
import com.efajtahamid.weblab.ui.projects.ProjectsViewModel
import com.efajtahamid.weblab.ui.settings.SettingsScreen
import com.efajtahamid.weblab.ui.settings.SettingsViewModel
import com.efajtahamid.weblab.ui.terminal.TerminalScreen
import com.efajtahamid.weblab.ui.terminal.TerminalViewModel
import java.io.File

private object Routes {
    const val PROJECTS = "projects"
    const val TERMINAL_HOME = "terminal_home"
    const val PACKAGES_HOME = "packages_home"
    const val SETTINGS = "settings"
    const val PROJECT_WORKSPACE = "project/{projectId}"

    fun projectWorkspace(projectId: String) = "project/$projectId"
}

private enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    PROJECTS(Routes.PROJECTS, "Projects", Icons.Filled.Home),
    TERMINAL(Routes.TERMINAL_HOME, "Terminal", Icons.Filled.Terminal),
    PACKAGES(Routes.PACKAGES_HOME, "Packages", Icons.Filled.Extension),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
}

@Composable
fun WebLabNavGraph(app: WebLabApp) {
    val navController = rememberNavController()
    val runtimeManager = remember(app) { NodeRuntimeManager(app) }

    Scaffold(
        bottomBar = { WebLabBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PROJECTS,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.PROJECTS) {
                val viewModel: ProjectsViewModel = viewModel(factory = ProjectsViewModel.factory(app.projectRepository))
                ProjectsScreen(
                    viewModel = viewModel,
                    onOpenProject = { project -> navController.navigate(Routes.projectWorkspace(project.id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenTerminal = { navController.navigate(Routes.TERMINAL_HOME) },
                    onOpenPackages = { navController.navigate(Routes.PACKAGES_HOME) }
                )
            }

            composable(Routes.SETTINGS) {
                val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(runtimeManager))
                SettingsScreen(viewModel)
            }

            composable(Routes.TERMINAL_HOME) {
                MostRecentProjectGate(app, runtimeManager, title = "Terminal") { projectRoot, terminalEngine, _ ->
                    val viewModel: TerminalViewModel = viewModel(
                        factory = TerminalViewModel.factory(terminalEngine, runtimeManager)
                    )
                    TerminalScreen(viewModel)
                }
            }
            composable(Routes.PACKAGES_HOME) {
                MostRecentProjectGate(app, runtimeManager, title = "Packages") { projectRoot, _, _ ->
                    val viewModel: com.efajtahamid.weblab.ui.packages.PackagesViewModel = viewModel(
                        factory = com.efajtahamid.weblab.ui.packages.PackagesViewModel.factory(projectRoot, runtimeManager)
                    )
                    com.efajtahamid.weblab.ui.packages.PackagesScreen(viewModel)
                }
            }

            composable(
                route = Routes.PROJECT_WORKSPACE,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                ProjectWorkspace(
                    app = app,
                    runtimeManager = runtimeManager,
                    projectId = projectId
                )
            }
        }
    }
}

@Composable
private fun WebLabBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    // The bottom bar only shows for top-level destinations, not inside a project
    // workspace (which has its own internal Files / Editor / Preview / Terminal nav).
    val isTopLevel = TopLevelDestination.values().any { it.route == currentRoute?.route }
    if (!isTopLevel) return

    NavigationBar {
        TopLevelDestination.values().forEach { destination ->
            val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}

/**
 * Everything scoped to a single open project: Files, Editor, Preview, Terminal.
 * Each of these gets its own bottom navigation local to the workspace.
 */
@Composable
private fun ProjectWorkspace(
    app: WebLabApp,
    runtimeManager: NodeRuntimeManager,
    projectId: String
) {
    var project by remember { mutableStateOf<ProjectEntity?>(null) }

    LaunchedEffect(projectId) {
        project = app.projectRepository.getProject(projectId)
    }

    val loadedProject = project ?: return

    val projectRoot = remember(loadedProject.dirPath) { File(loadedProject.dirPath) }
    val fileRepository = remember(projectRoot) { FileRepository(projectRoot) }
    val terminalEngine = remember(projectRoot) { TerminalEngine(projectRoot, runtimeManager) }

    val workspaceNav = rememberNavController()
    var currentEditorPath by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = { WorkspaceBottomBar(workspaceNav) }
    ) { padding ->
        NavHost(
            navController = workspaceNav,
            startDestination = "files",
            modifier = Modifier.padding(padding)
        ) {
            composable("files") {
                val viewModel: FileExplorerViewModel = viewModel(
                    factory = FileExplorerViewModel.factory(fileRepository, projectRoot)
                )
                FileExplorerScreen(
                    projectRoot = projectRoot,
                    viewModel = viewModel,
                    onOpenFile = { relativePath ->
                        currentEditorPath = relativePath
                        workspaceNav.navigate("editor")
                    }
                )
            }
            composable("editor") {
                val path = currentEditorPath
                if (path == null) {
                    workspaceNav.popBackStack()
                } else {
                    val viewModel: CodeEditorViewModel = viewModel(factory = CodeEditorViewModel.factory(fileRepository))
                    CodeEditorScreen(viewModel = viewModel, relativePath = path, onBack = { workspaceNav.popBackStack() })
                }
            }
            composable("preview") {
                val viewModel: PreviewViewModel = viewModel(
                    factory = PreviewViewModel.factory(projectRoot, loadedProject.projectType)
                )
                PreviewScreen(viewModel)
            }
            composable("terminal") {
                val viewModel: TerminalViewModel = viewModel(
                    factory = TerminalViewModel.factory(terminalEngine, runtimeManager)
                )
                TerminalScreen(viewModel)
            }
        }
    }
}

private enum class WorkspaceDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    FILES("files", "Files", Icons.Filled.Folder),
    PREVIEW("preview", "Preview", Icons.Filled.Preview),
    TERMINAL("terminal", "Terminal", Icons.Filled.Terminal)
}

@Composable
private fun WorkspaceBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        WorkspaceDestination.values().forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo("files") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}

/**
 * Shown for the top-level Terminal/Packages tabs when no project is open yet —
 * those tools operate on a specific project's sandbox, so WebLab asks the user
 * to open one rather than showing a terminal with nothing to run against.
 */
@Composable
private fun OpenAProjectFirstScreen(title: String) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Open a project first, then use its $title tab.")
        }
    }
}

/**
 * Backs the top-level Terminal/Packages tabs: they act on whichever project the
 * user most recently touched, falling back to [OpenAProjectFirstScreen] when
 * there is no project yet.
 */
@Composable
private fun MostRecentProjectGate(
    app: WebLabApp,
    runtimeManager: NodeRuntimeManager,
    title: String,
    content: @Composable (projectRoot: File, terminalEngine: TerminalEngine, project: ProjectEntity) -> Unit
) {
    var projects by remember { mutableStateOf<List<ProjectEntity>>(emptyList()) }
    LaunchedEffect(Unit) {
        app.projectRepository.observeProjects().collect { projects = it }
    }

    val mostRecent = projects.firstOrNull()
    if (mostRecent == null) {
        OpenAProjectFirstScreen(title)
        return
    }

    val projectRoot = remember(mostRecent.dirPath) { File(mostRecent.dirPath) }
    val terminalEngine = remember(projectRoot) { TerminalEngine(projectRoot, runtimeManager) }
    content(projectRoot, terminalEngine, mostRecent)
}
