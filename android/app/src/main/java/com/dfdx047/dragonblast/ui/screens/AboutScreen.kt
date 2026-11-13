package com.dfdx047.dragonblast.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dfdx047.dragonblast.BuildConfig
import com.dfdx047.dragonblast.DragonBlastApp
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.AppLanguage
import com.dfdx047.dragonblast.ui.LauncherViewModel
import com.dfdx047.dragonblast.ui.components.Choice
import com.dfdx047.dragonblast.ui.components.ChoiceRow
import com.dfdx047.dragonblast.ui.components.Divider
import com.dfdx047.dragonblast.ui.components.InfoDialog
import com.dfdx047.dragonblast.ui.components.Logo
import com.dfdx047.dragonblast.ui.components.SectionCard
import com.dfdx047.dragonblast.ui.components.help
import com.dfdx047.dragonblast.ui.theme.DbColors

private data class Credit(val name: String, val role: Int, val url: String)

@Composable
fun AboutScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    var notices by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard(stringResource(R.string.about_title), Icons.Outlined.Info) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.app_logo), null, Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)))
                Spacer(Modifier.width(14.dp))
                Column {
                    Logo(24)
                    Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall, color = DbColors.TextDim)
                    if (BuildConfig.UI_ONLY) Text(stringResource(R.string.about_ui_only), style = MaterialTheme.typography.labelSmall, color = DbColors.Warn)
                }
            }
            Text(stringResource(R.string.about_text), style = MaterialTheme.typography.bodyMedium, color = DbColors.TextDim)
            Text(stringResource(R.string.about_by), style = MaterialTheme.typography.bodySmall, color = DbColors.TextFaint)
        }

        SectionCard(stringResource(R.string.about_credits), Icons.Outlined.VolunteerActivism) {
            listOf(
                Credit("WistfulHopes — RB2 Recompiled", R.string.credit_recomp, DragonBlastApp.RECOMP_URL),
                Credit("moddedrounds — RB2-Android", R.string.credit_android, DragonBlastApp.UPSTREAM_URL),
                Credit("Project8Recomp", R.string.credit_p8, "https://github.com/theokyr/Project8Recomp"),
                Credit("ReXGlue SDK", R.string.credit_rexglue, "https://github.com/rexglue/rexglue-sdk"),
                Credit("Xenia", R.string.credit_xenia, "https://github.com/xenia-project/xenia"),
                Credit("SDL", R.string.credit_sdl, "https://libsdl.org"),
                Credit("Mesa (Turnip, PanVK)", R.string.credit_mesa, "https://mesa3d.org"),
                Credit("libadrenotools", R.string.credit_adrenotools, "https://github.com/bylaws/libadrenotools"),
                Credit("Mr. Purple · StevenMXZ · K11MCH1 · whitebelyash · faux123", R.string.credit_turnip, "https://github.com/MrPurple666/purple-turnip"),
                Credit("zenithblue · JimVulkan · FristOneRR · MineGraph · ARMSX2", R.string.credit_panvk, "https://github.com/zenithblue-oss/panvk-kbase-android"),
                Credit("Rajdhani (Indian Type Foundry)", R.string.credit_font, "https://fonts.google.com/specimen/Rajdhani"),
            ).forEachIndexed { i, c ->
                if (i > 0) Divider()
                Row(Modifier.fillMaxWidth().clickable { open(c.url) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.bodyMedium, color = DbColors.Text)
                        Text(stringResource(c.role), style = MaterialTheme.typography.bodySmall, color = DbColors.TextFaint)
                    }
                    Icon(Icons.Outlined.OpenInNew, null, tint = DbColors.TextFaint, modifier = Modifier.size(16.dp))
                }
            }
        }

        SectionCard(stringResource(R.string.about_app), Icons.Outlined.Language) {
            ChoiceRow(
                title = stringResource(R.string.about_language),
                help = help(R.string.about_language_short),
                options = listOf(
                    Choice(AppLanguage.SYSTEM, stringResource(R.string.about_language_system)),
                    Choice(AppLanguage.PT, "Português"),
                    Choice(AppLanguage.EN, "English"),
                    Choice(AppLanguage.ES, "Español"),
                ),
                selected = vm.settings.language,
                onSelect = { l -> vm.update { it.copy(language = l) } },
            )
        }

        SectionCard(stringResource(R.string.about_legal), Icons.Outlined.Gavel) {
            Text(stringResource(R.string.about_legal_text), style = MaterialTheme.typography.bodySmall, color = DbColors.TextDim)
            Text(
                stringResource(R.string.about_notices),
                style = MaterialTheme.typography.labelLarge,
                color = DbColors.Steel,
                modifier = Modifier.clickable {
                    notices = runCatching { context.assets.open("NOTICE.txt").bufferedReader().readText() }.getOrDefault("") +
                        "\n\n" + runCatching { context.assets.open("OFL-Rajdhani.txt").bufferedReader().readText() }.getOrDefault("")
                }.padding(vertical = 4.dp),
            )
        }
    }
    notices?.let { InfoDialog(stringResource(R.string.about_notices), it) { notices = null } }
}
