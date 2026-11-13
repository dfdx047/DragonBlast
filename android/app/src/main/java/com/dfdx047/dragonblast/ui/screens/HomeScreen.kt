package com.dfdx047.dragonblast.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dfdx047.dragonblast.DragonBlastApp
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.Orientation
import com.dfdx047.dragonblast.ui.LauncherTab
import com.dfdx047.dragonblast.ui.LauncherViewModel
import com.dfdx047.dragonblast.ui.components.CardsPage
import com.dfdx047.dragonblast.ui.components.Choice
import com.dfdx047.dragonblast.ui.components.ChoiceRow
import com.dfdx047.dragonblast.ui.components.ClashStage
import com.dfdx047.dragonblast.ui.components.Level
import com.dfdx047.dragonblast.ui.components.Logo
import com.dfdx047.dragonblast.ui.components.Pill
import com.dfdx047.dragonblast.ui.components.SectionCard
import com.dfdx047.dragonblast.ui.components.StatusLine
import com.dfdx047.dragonblast.ui.components.color
import com.dfdx047.dragonblast.ui.components.formatBytes
import com.dfdx047.dragonblast.ui.components.help
import com.dfdx047.dragonblast.ui.theme.DbColors

@Composable
fun HomeScreen(vm: LauncherViewModel, goTo: (LauncherTab) -> Unit) {
    val context = LocalContext.current
    val pickIso = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.importIso(uri)
    }
    fun openPicker() = pickIso.launch(arrayOf("application/x-iso9660-image", "application/octet-stream", "*/*"))
    val install = vm.install
    val ready = install?.playable == true
    var confirmDelete by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > maxHeight
        CardsPage(fullWidthFirst = true) {
            val (status, level) = when {
                install == null -> stringResource(R.string.status_checking) to Level.INFO
                vm.busy?.label == R.string.busy_import -> stringResource(R.string.status_importing) to Level.INFO
                ready -> stringResource(R.string.status_ready) to Level.OK
                install.installed -> stringResource(R.string.status_incomplete) to Level.WARN
                else -> stringResource(R.string.status_no_iso) to Level.WARN
            }
            val shape = RoundedCornerShape(22.dp)
            ClashStage(
                ready = ready,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (wide) 250.dp else 320.dp)
                    .clip(shape)
                    .border(1.dp, DbColors.Line, shape),
            ) {
                Column(Modifier.align(Alignment.TopStart).padding(18.dp)) {
                    Logo(if (wide) 30 else 32)
                    Text(stringResource(R.string.game_title), style = MaterialTheme.typography.labelLarge, color = DbColors.TextDim)
                }
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.weight(1f)) { Pill(status, level.color()) }
                    if (ready) {
                        PlayButton(enabled = vm.busy == null) { vm.play(context) }
                    } else {
                        ImportButton(enabled = vm.busy == null && install != null, onClick = ::openPicker)
                    }
                }
            }

            SectionCard(stringResource(R.string.files_title), Icons.Outlined.FolderOpen, subtitle = stringResource(R.string.files_subtitle)) {
                if (install?.installed == true) {
                    StatusLine(
                        stringResource(R.string.files_installed, install.fileCount, formatBytes(install.sizeBytes)),
                        if (ready) Level.OK else Level.WARN,
                    )
                    if (!install.xexOk) StatusLine(stringResource(R.string.files_xex_bad), Level.ERROR)
                    if (!install.packsOk) StatusLine(stringResource(R.string.files_packs_bad), Level.ERROR)
                } else {
                    Text(stringResource(R.string.files_help), style = MaterialTheme.typography.bodySmall, color = DbColors.TextDim)
                }
                vm.importError?.let { StatusLine(it, Level.ERROR) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = ::openPicker,
                        enabled = vm.busy == null,
                        border = BorderStroke(1.dp, DbColors.Line),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Outlined.Album, null, tint = DbColors.Ember)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (install?.installed == true) R.string.files_reimport else R.string.files_import), color = DbColors.Text)
                    }
                    if (install?.installed == true) {
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            enabled = vm.busy == null,
                            border = BorderStroke(1.dp, DbColors.Line),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, null, tint = DbColors.TextDim)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.files_delete), color = DbColors.TextDim)
                        }
                    }
                }
            }

            SectionCard(stringResource(R.string.screen_title), Icons.Outlined.ScreenRotation) {
                ChoiceRow(
                    title = stringResource(R.string.orientation),
                    help = help(R.string.orientation_short, R.string.orientation_long),
                    options = listOf(
                        Choice(Orientation.LANDSCAPE, stringResource(R.string.orientation_landscape)),
                        Choice(Orientation.PORTRAIT, stringResource(R.string.orientation_portrait)),
                        Choice(Orientation.SENSOR, stringResource(R.string.orientation_sensor)),
                    ),
                    selected = vm.settings.orientation,
                    onSelect = { o -> vm.update { it.copy(orientation = o) } },
                )
            }

            SectionCard(stringResource(R.string.community_title), Icons.Outlined.Groups, subtitle = stringResource(R.string.community_sub)) {
                fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LinkDot("YouTube", Icons.Outlined.SmartDisplay, Color(0xFFD86A60)) { open(DragonBlastApp.YOUTUBE_URL) }
                    LinkDot("Discord", Icons.Outlined.Forum, Color(0xFF8C94E0)) { open(DragonBlastApp.DISCORD_URL) }
                    LinkDot("Ko-fi", Icons.Outlined.LocalCafe, Color(0xFF6FB6D6)) { open(DragonBlastApp.KOFI_URL) }
                    LinkDot("GitHub", Icons.Outlined.Code, DbColors.Silver) { open(DragonBlastApp.GITHUB_URL) }
                }
                Text(stringResource(R.string.community_kofi), style = MaterialTheme.typography.bodySmall, color = DbColors.TextFaint)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = DbColors.SlateHigh,
            title = { Text(stringResource(R.string.files_delete_title)) },
            text = { Text(stringResource(R.string.files_delete_text), color = DbColors.TextDim) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteGame() }) { Text(stringResource(R.string.files_delete), color = DbColors.Error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel), color = DbColors.TextDim) } },
        )
    }
}

/** Ready: a solid ember button with a slow glow behind it. */
@Composable
private fun PlayButton(enabled: Boolean, onClick: () -> Unit) {
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        0.25f, 0.55f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "g",
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(54.dp)
            .widthIn(min = 168.dp)
            .drawBehind {
                // a soft halo just around the button, breathing slowly
                val grow = 6.dp.toPx()
                for (i in 1..3) {
                    val g = grow * i / 3f
                    drawRoundRect(
                        DbColors.Ember.copy(alpha = glow * 0.22f / i),
                        topLeft = androidx.compose.ui.geometry.Offset(-g, -g),
                        size = androidx.compose.ui.geometry.Size(size.width + g * 2, size.height + g * 2),
                        cornerRadius = CornerRadius(14.dp.toPx() + g),
                    )
                }
            },
        colors = ButtonDefaults.buttonColors(containerColor = DbColors.Ember, contentColor = Color(0xFF221508)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Filled.PlayArrow, null)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.play), style = MaterialTheme.typography.titleMedium)
    }
}

/** No game yet: an outlined, quieter button that asks for the ISO. */
@Composable
private fun ImportButton(enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(54.dp).widthIn(min = 168.dp),
        border = BorderStroke(1.dp, DbColors.Silver.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.35f), contentColor = DbColors.Silver),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Outlined.Album, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.import_iso), style = MaterialTheme.typography.titleMedium)
    }
}

/** A round link: the brand colour only in a thin ring and the icon. */
@Composable
private fun LinkDot(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(color.copy(alpha = 0.08f)).border(1.dp, color.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = color, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = DbColors.TextDim)
    }
}
