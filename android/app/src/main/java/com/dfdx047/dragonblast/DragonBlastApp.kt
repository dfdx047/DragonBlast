package com.dfdx047.dragonblast

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.dfdx047.dragonblast.data.AppLanguage
import com.dfdx047.dragonblast.data.AppPaths
import com.dfdx047.dragonblast.data.SettingsStore

class DragonBlastApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPaths.init(this)
        applyLanguage(SettingsStore(this).load().language)
    }

    companion object {
        const val GITHUB_URL = "https://github.com/dfdx047"
        const val REPO_URL = "https://github.com/dfdx047/DragonBlast"
        const val YOUTUBE_URL = "https://www.youtube.com/@DFDX47"
        const val DISCORD_URL = "https://discord.gg/nXSANX9V6k"
        const val KOFI_URL = "https://ko-fi.com/dfdx047"
        const val UPSTREAM_URL = "https://github.com/moddedrounds/RB2-Android"
        const val RECOMP_URL = "https://github.com/WistfulHopes/RB2"

        /**
         * The phone's language picks the strings (values-pt, values-es, English otherwise); a fixed choice from
         * About overrides it through AppCompat's per-app locale.
         */
        fun applyLanguage(lang: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(
                if (lang == AppLanguage.SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(lang.tag),
            )
        }
    }
}
