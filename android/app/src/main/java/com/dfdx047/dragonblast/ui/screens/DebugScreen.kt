package com.dfdx047.dragonblast.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.AppPaths
import com.dfdx047.dragonblast.data.Diagnostics
import com.dfdx047.dragonblast.ui.LauncherViewModel
import com.dfdx047.dragonblast.ui.components.CardsPage
import com.dfdx047.dragonblast.ui.components.Divider
import com.dfdx047.dragonblast.ui.components.InfoButton
import com.dfdx047.dragonblast.ui.components.SectionCard
import com.dfdx047.dragonblast.ui.components.SettingSwitch
import com.dfdx047.dragonblast.ui.components.formatBytes
import com.dfdx047.dragonblast.ui.components.help
import com.dfdx047.dragonblast.ui.theme.DbColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DebugScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val s = vm.settings
    var confirmReset by remember { mutableStateOf(false) }
    var tail by remember { mutableStateOf("") }
    var cacheSize by remember { mutableStateOf(0L) }
    LaunchedEffect(vm.message) {
        withContext(Dispatchers.IO) {
            tail = Diagnostics.logTail()
            cacheSize = Diagnostics.dirSize(AppPaths.cache)
        }
    }

    CardsPage {
        SectionCard(stringResource(R.string.dbg_logs), Icons.Outlined.Description, subtitle = stringResource(R.string.dbg_logs_sub)) {
            ActionButton(Icons.Outlined.IosShare, stringResource(R.string.dbg_export), enabled = vm.busy == null) { vm.exportLogs(context) }
            SettingSwitch(stringResource(R.string.dbg_verbose), help(R.string.dbg_verbose_short, R.string.dbg_verbose_long), s.verboseLog) { v ->
                vm.update { it.copy(verboseLog = v) }
            }
            Divider()
            Text(stringResource(R.string.dbg_last_log), style = MaterialTheme.typography.labelLarge, color = DbColors.TextDim)
            Text(
                tail.ifBlank { stringResource(R.string.dbg_no_log) },
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                color = DbColors.TextDim,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .background(DbColors.Ink, RoundedCornerShape(10.dp))
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
            )
        }

        SectionCard(stringResource(R.string.dbg_overlay), Icons.Outlined.Terminal) {
            SettingSwitch(stringResource(R.string.dbg_gpu_stats), help(R.string.dbg_gpu_stats_short, R.string.dbg_gpu_stats_long), s.gpuStats) { v ->
                vm.update { it.copy(gpuStats = v) }
            }
        }

        SectionCard(
            stringResource(R.string.dbg_args), Icons.Outlined.Build, subtitle = stringResource(R.string.dbg_args_sub),
            trailing = { InfoButton(stringResource(R.string.dbg_args), stringResource(R.string.dbg_args_long)) },
        ) {
            var text by remember(s.extraArgs) { mutableStateOf(s.extraArgs) }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                placeholder = { Text("--vsync=false", fontFamily = FontFamily.Monospace, color = DbColors.TextFaint) },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DbColors.Ember, unfocusedBorderColor = DbColors.Line, cursorColor = DbColors.Ember),
            )
            if (text != s.extraArgs) {
                TextButton(onClick = { vm.update { it.copy(extraArgs = text.trim()) } }) { Text(stringResource(R.string.save), color = DbColors.Ember) }
            }
        }

        SectionCard(stringResource(R.string.dbg_maint), Icons.Outlined.RestartAlt) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(Icons.Outlined.RestartAlt, stringResource(R.string.dbg_clear_cache, formatBytes(cacheSize))) { vm.clearShaderCache() }
                ActionButton(Icons.Outlined.RestartAlt, stringResource(R.string.dbg_reset)) { confirmReset = true }
            }
            Text(stringResource(R.string.dbg_clear_cache_help), style = MaterialTheme.typography.bodySmall, color = DbColors.TextFaint)
        }

        SectionCard(stringResource(R.string.dbg_device), Icons.Outlined.PhoneAndroid) {
            val report = remember(vm.gpu, vm.install, vm.settings, vm.driverChoice) { Diagnostics.deviceReport(context, s) }
            Text(report, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, lineHeight = 14.sp, color = DbColors.TextDim)
            ActionButton(Icons.Outlined.ContentCopy, stringResource(R.string.dbg_copy)) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Dragon Blast", report))
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = DbColors.SlateHigh,
            title = { Text(stringResource(R.string.dbg_reset)) },
            text = { Text(stringResource(R.string.dbg_reset_text), color = DbColors.TextDim) },
            confirmButton = { TextButton(onClick = { confirmReset = false; vm.resetSettings() }) { Text(stringResource(R.string.dbg_reset), color = DbColors.Error) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel), color = DbColors.TextDim) } },
        )
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, border = BorderStroke(1.dp, DbColors.Line), shape = RoundedCornerShape(12.dp)) {
        Icon(icon, null, tint = DbColors.Ember)
        Spacer(Modifier.width(8.dp))
        Text(label, color = DbColors.Text)
    }
}
