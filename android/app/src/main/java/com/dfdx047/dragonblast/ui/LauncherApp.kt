package com.dfdx047.dragonblast.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.ui.components.InfoDialog
import com.dfdx047.dragonblast.ui.components.Logo
import com.dfdx047.dragonblast.ui.screens.AboutScreen
import com.dfdx047.dragonblast.ui.screens.ControlsScreen
import com.dfdx047.dragonblast.ui.screens.DebugScreen
import com.dfdx047.dragonblast.ui.screens.DriversScreen
import com.dfdx047.dragonblast.ui.screens.GraphicsScreen
import com.dfdx047.dragonblast.ui.screens.HomeScreen
import com.dfdx047.dragonblast.ui.theme.DbColors

enum class LauncherTab(@StringRes val title: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Outlined.Home),
    GRAPHICS(R.string.tab_graphics, Icons.Outlined.Tune),
    CONTROLS(R.string.tab_controls, Icons.Outlined.SportsEsports),
    DRIVERS(R.string.tab_drivers, Icons.Outlined.Memory),
    DEBUG(R.string.tab_debug, Icons.Outlined.BugReport),
    ABOUT(R.string.tab_about, Icons.Outlined.Info),
}

@Composable
fun LauncherApp(vm: LauncherViewModel) {
    var tab by rememberSaveable { mutableStateOf(LauncherTab.HOME) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(vm.message) {
        val m = vm.message ?: return@LaunchedEffect
        snackbar.showSnackbar(context.getString(m.res, *m.args.toTypedArray()))
        vm.message = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val wide = maxWidth > maxHeight && maxWidth >= 600.dp
            if (wide) {
                // Landscape: a slim rail on the left, the page on the right.
                Row(Modifier.fillMaxSize()) {
                    SideRail(tab) { tab = it }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        BusyBar(vm.busy)
                        Page(tab, vm) { tab = it }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    // Home has the logo on its stage: no header there.
                    if (tab != LauncherTab.HOME) Header()
                    BusyBar(vm.busy)
                    Box(Modifier.weight(1f).fillMaxWidth()) { Page(tab, vm) { tab = it } }
                    BottomTabs(tab) { tab = it }
                }
            }
        }
    }

    if (vm.showNoEngine) {
        InfoDialog(stringResource(R.string.no_engine_title), stringResource(R.string.no_engine_text)) { vm.showNoEngine = false }
    }
}

@Composable
private fun Page(tab: LauncherTab, vm: LauncherViewModel, goTo: (LauncherTab) -> Unit) {
    AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { t ->
        Box(Modifier.fillMaxSize()) {
            when (t) {
                LauncherTab.HOME -> HomeScreen(vm, goTo)
                LauncherTab.GRAPHICS -> GraphicsScreen(vm)
                LauncherTab.CONTROLS -> ControlsScreen(vm)
                LauncherTab.DRIVERS -> DriversScreen(vm)
                LauncherTab.DEBUG -> DebugScreen(vm)
                LauncherTab.ABOUT -> AboutScreen(vm)
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(DbColors.Ink, DbColors.Graphite)))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.app_logo),
            null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Logo(22)
            Text(stringResource(R.string.header_subtitle), style = MaterialTheme.typography.labelSmall, color = DbColors.TextFaint)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, DbColors.Ember.copy(alpha = 0.35f), Color.Transparent))))
}

/** Bottom bar: six tabs share the width; the chosen one has an ember icon and a short line above it. */
@Composable
private fun BottomTabs(selected: LauncherTab, onSelect: (LauncherTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(DbColors.Ink)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(DbColors.LineSoft))
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            LauncherTab.entries.forEach { t ->
                TabItem(t, t == selected, Modifier.weight(1f)) { onSelect(t) }
            }
        }
    }
}

@Composable
private fun TabItem(tab: LauncherTab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            Modifier
                .width(18.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) DbColors.Ember else Color.Transparent),
        )
        Icon(tab.icon, null, tint = if (selected) DbColors.Ember else DbColors.TextFaint, modifier = Modifier.size(22.dp))
        Text(
            stringResource(tab.title),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = if (selected) DbColors.Text else DbColors.TextFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SideRail(selected: LauncherTab, onSelect: (LauncherTab) -> Unit) {
    Row(Modifier.fillMaxHeight()) {
        Column(
            Modifier.fillMaxHeight().width(84.dp).background(DbColors.Ink).padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(painterResource(R.drawable.app_logo), null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.height(8.dp))
            LauncherTab.entries.forEach { t ->
                TabItem(t, t == selected, Modifier.fillMaxWidth().padding(horizontal = 6.dp)) { onSelect(t) }
            }
        }
        Box(Modifier.fillMaxHeight().width(1.dp).background(DbColors.LineSoft))
    }
}

@Composable
private fun BusyBar(busy: Busy?) {
    if (busy == null) return
    Column(
        Modifier
            .fillMaxWidth()
            .background(DbColors.SlateHigh)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            stringResource(busy.label) + if (busy.detail.isNotEmpty()) " · ${busy.detail}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = DbColors.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        if (busy.progress != null) {
            LinearProgressIndicator(
                progress = { busy.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(),
                color = DbColors.Ember, trackColor = DbColors.SlateHighest, drawStopIndicator = {},
            )
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = DbColors.Ember, trackColor = DbColors.SlateHighest)
        }
    }
}
