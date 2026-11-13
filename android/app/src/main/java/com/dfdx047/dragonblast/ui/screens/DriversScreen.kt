package com.dfdx047.dragonblast.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.GpuInfo
import com.dfdx047.dragonblast.data.GpuVendor
import com.dfdx047.dragonblast.drivers.DriverCatalog
import com.dfdx047.dragonblast.drivers.DriverSource
import com.dfdx047.dragonblast.drivers.DriverStore
import com.dfdx047.dragonblast.drivers.InstalledDriver
import com.dfdx047.dragonblast.drivers.isPackage
import com.dfdx047.dragonblast.ui.LauncherViewModel
import com.dfdx047.dragonblast.ui.SourceState
import com.dfdx047.dragonblast.ui.components.CardsPage
import com.dfdx047.dragonblast.ui.components.Choice
import com.dfdx047.dragonblast.ui.components.ChoiceRow
import com.dfdx047.dragonblast.ui.components.InfoButton
import com.dfdx047.dragonblast.ui.components.InfoRow
import com.dfdx047.dragonblast.ui.components.Level
import com.dfdx047.dragonblast.ui.components.Pill
import com.dfdx047.dragonblast.ui.components.SectionCard
import com.dfdx047.dragonblast.ui.components.StatusLine
import com.dfdx047.dragonblast.ui.components.formatBytes
import com.dfdx047.dragonblast.ui.theme.DbColors

@Composable
fun DriversScreen(vm: LauncherViewModel) {
    val gpu = vm.gpu
    val pickZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.importDriver(uri)
    }
    val deviceVendor = gpu?.vendor ?: GpuVendor.OTHER
    var listVendor by rememberSaveable { mutableStateOf(if (deviceVendor == GpuVendor.MALI) GpuVendor.MALI else GpuVendor.ADRENO) }
    LaunchedEffect(deviceVendor) { if (deviceVendor == GpuVendor.MALI || deviceVendor == GpuVendor.ADRENO) listVendor = deviceVendor }

    CardsPage {
        SectionCard(stringResource(R.string.drv_gpu), Icons.Outlined.DeveloperBoard) {
            if (gpu == null) {
                CircularProgressIndicator(Modifier.size(20.dp), color = DbColors.Ember, strokeWidth = 2.dp)
            } else {
                InfoRow(stringResource(R.string.drv_gpu_name), gpu.renderer)
                InfoRow(stringResource(R.string.drv_soc), GpuInfo.socName())
                InfoRow("Vulkan", gpu.vulkanVersion)
                InfoRow(stringResource(R.string.drv_system_version), gpu.driverVersion)
                val last = DriverStore.lastUsed()
                InfoRow(stringResource(R.string.drv_last_used), last.ifBlank { stringResource(R.string.drv_never) })
            }
        }

        SectionCard(
            stringResource(R.string.drv_active), Icons.Outlined.Memory,
            subtitle = stringResource(R.string.drv_active_sub),
            trailing = { InfoButton(stringResource(R.string.drv_active), stringResource(R.string.drv_active_long)) },
        ) {
            DriverOption(
                stringResource(R.string.drv_auto),
                stringResource(if (deviceVendor == GpuVendor.ADRENO && DriverStore.hasBundledTurnip(LocalContext.current)) R.string.drv_auto_turnip else R.string.drv_auto_system),
                vm.driverChoice == DriverStore.CHOICE_AUTO,
            ) { vm.selectDriver(DriverStore.CHOICE_AUTO) }
            DriverOption(stringResource(R.string.drv_system), stringResource(R.string.drv_system_sub), vm.driverChoice == DriverStore.CHOICE_SYSTEM) {
                vm.selectDriver(DriverStore.CHOICE_SYSTEM)
            }
            vm.installedDrivers.forEach { d ->
                InstalledRow(d, vm.activeDriver == d.dir.name, fits = d.vendor == deviceVendor || deviceVendor == GpuVendor.OTHER, onSelect = { vm.activateDriver(d) }, onDelete = { vm.deleteDriver(d) })
            }
            OutlinedButton(
                onClick = { pickZip.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                enabled = vm.busy == null,
                border = BorderStroke(1.dp, DbColors.Line),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.FileOpen, null, tint = DbColors.Ember)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.drv_import), color = DbColors.Text)
            }
        }

        SectionCard(stringResource(R.string.drv_download), Icons.Outlined.CloudDownload, subtitle = stringResource(R.string.drv_download_sub)) {
            ChoiceRow(
                title = null,
                options = listOf(
                    Choice(GpuVendor.ADRENO, "Adreno", if (deviceVendor == GpuVendor.ADRENO) stringResource(R.string.drv_your_gpu) else "Turnip · Qualcomm"),
                    Choice(GpuVendor.MALI, "Mali", if (deviceVendor == GpuVendor.MALI) stringResource(R.string.drv_your_gpu) else "PanVK · MaliSX2"),
                ),
                selected = listVendor,
                onSelect = { listVendor = it },
            )
            if (gpu != null && listVendor != deviceVendor) {
                StatusLine(stringResource(R.string.drv_other_vendor, gpu.renderer), Level.WARN)
            }
            if (listVendor == GpuVendor.MALI) StatusLine(stringResource(R.string.drv_mali_note), Level.INFO)
            DriverCatalog.sources(listVendor).forEach { src ->
                SourceBlock(vm, src, gpu, installable = gpu != null && listVendor == deviceVendor)
            }
        }
    }
}

@Composable
private fun DriverOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) DbColors.Ember.copy(alpha = 0.07f) else DbColors.SlateHigh)
            .border(1.dp, if (selected) DbColors.Ember.copy(alpha = 0.8f) else DbColors.Line, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Radio(selected)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = if (selected) DbColors.EmberGlow else DbColors.Text, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = DbColors.TextFaint)
        }
    }
}

@Composable
private fun Radio(selected: Boolean) {
    Box(
        Modifier.size(18.dp).border(1.5.dp, if (selected) DbColors.Ember else DbColors.TextFaint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(8.dp).background(DbColors.Ember, CircleShape))
    }
}

@Composable
private fun InstalledRow(d: InstalledDriver, selected: Boolean, fits: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) DbColors.Ember.copy(alpha = 0.07f) else DbColors.SlateHigh)
            .border(1.dp, if (selected) DbColors.Ember.copy(alpha = 0.8f) else DbColors.Line, shape)
            .selectable(selected, enabled = fits, role = Role.RadioButton, onClick = onSelect)
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Radio(selected)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(d.name, style = MaterialTheme.typography.bodyMedium, color = if (selected) DbColors.EmberGlow else DbColors.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOf(d.version, d.author, d.library.name).filter { it.isNotBlank() }.joinToString(" · ")
            Text(sub, style = MaterialTheme.typography.bodySmall, color = DbColors.TextFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!fits) Text(stringResource(R.string.drv_wrong_gpu), style = MaterialTheme.typography.labelSmall, color = DbColors.Warn)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, null, tint = DbColors.TextFaint) }
    }
}

/** One repository: header with credit and count; opens to its releases and their packages. */
@Composable
private fun SourceBlock(vm: LauncherViewModel, src: DriverSource, gpu: GpuInfo?, installable: Boolean) {
    var open by rememberSaveable(src.key) { mutableStateOf(false) }
    val context = LocalContext.current
    val state = vm.sources[src.key]
    val shape = RoundedCornerShape(12.dp)
    val supported = gpu == null || !installable || src.supports(gpu)
    Column(Modifier.fillMaxWidth().clip(shape).background(DbColors.SlateHigh).border(1.dp, DbColors.Line, shape)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    open = !open
                    if (open) vm.loadSource(src)
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(src.label, style = MaterialTheme.typography.titleMedium, color = DbColors.Text)
                    if (src.experimental) {
                        Spacer(Modifier.width(8.dp))
                        Pill(stringResource(R.string.experimental), DbColors.Warn)
                    }
                }
                Text(stringResource(src.about), style = MaterialTheme.typography.bodySmall, color = DbColors.TextFaint)
                Text("github.com/${src.owner}/${src.repo}", style = MaterialTheme.typography.labelSmall, color = DbColors.SteelDeep)
                if (!supported) Text(stringResource(R.string.drv_source_not_for_gpu), style = MaterialTheme.typography.labelSmall, color = DbColors.Warn)
            }
            (state as? SourceState.Ready)?.let { st ->
                val n = st.listing.releases.count { r -> r.assets.any { a -> isPackage(a.name) && (gpu == null || !installable || src.accepts(a.name, gpu)) } }
                Text(stringResource(R.string.drv_versions, n), style = MaterialTheme.typography.labelMedium, color = DbColors.TextDim)
                Spacer(Modifier.width(4.dp))
            }
            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = DbColors.TextDim)
        }
        AnimatedVisibility(open) {
            Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                when (state) {
                    null, SourceState.Loading -> Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = DbColors.Ember, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodySmall, color = DbColors.TextDim)
                    }
                    is SourceState.Failed -> Column {
                        StatusLine(stringResource(R.string.drv_list_failed, state.error), Level.ERROR)
                        Row {
                            OutlinedButton(onClick = { vm.loadSource(src, force = true) }, border = BorderStroke(1.dp, DbColors.Line)) {
                                Icon(Icons.Outlined.Refresh, null, tint = DbColors.TextDim)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.retry), color = DbColors.Text)
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(src.page))) } }, border = BorderStroke(1.dp, DbColors.Line)) {
                                Icon(Icons.Outlined.OpenInNew, null, tint = DbColors.TextDim)
                                Spacer(Modifier.width(6.dp))
                                Text("GitHub", color = DbColors.Text)
                            }
                        }
                    }
                    is SourceState.Ready -> {
                        val installedNames = vm.installedDrivers.map { it.dir.name }.toSet()
                        var shown = 0
                        state.listing.releases.forEach { r ->
                            val assets = r.assets.filter { a ->
                                isPackage(a.name) && (gpu == null || !installable || src.accepts(a.name, gpu))
                            }
                            if (assets.isEmpty()) return@forEach
                            shown++
                            ReleaseRow(vm, r, assets, installedNames, installable && supported)
                        }
                        if (shown == 0) Text(stringResource(R.string.drv_none_for_gpu), style = MaterialTheme.typography.bodySmall, color = DbColors.TextFaint, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseRow(vm: LauncherViewModel, r: DriverCatalog.Release, assets: List<DriverCatalog.Asset>, installed: Set<String>, installable: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.title, style = MaterialTheme.typography.bodyMedium, color = DbColors.Silver, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(8.dp))
            Text(r.date, style = MaterialTheme.typography.labelSmall, color = DbColors.TextFaint)
        }
        assets.forEach { a ->
            val base = a.name.removeSuffix(".zip").removeSuffix(".adpkg")
            val have = base in installed
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(a.name, style = MaterialTheme.typography.bodySmall, color = DbColors.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatBytes(a.size), style = MaterialTheme.typography.labelSmall, color = DbColors.TextFaint)
                }
                if (have) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = DbColors.Ok, modifier = Modifier.padding(12.dp).size(20.dp))
                } else if (vm.downloading == a.url) {
                    CircularProgressIndicator(Modifier.padding(14.dp).size(18.dp), color = DbColors.Ember, strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { vm.downloadDriver(a) }, enabled = installable && vm.busy == null) {
                        Icon(Icons.Outlined.CloudDownload, null, tint = if (installable) DbColors.Ember else DbColors.TextFaint)
                    }
                }
            }
        }
    }
}
