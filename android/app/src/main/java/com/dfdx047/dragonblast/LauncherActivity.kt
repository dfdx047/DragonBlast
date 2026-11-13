package com.dfdx047.dragonblast

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.dfdx047.dragonblast.ui.LauncherApp
import com.dfdx047.dragonblast.ui.LauncherViewModel
import com.dfdx047.dragonblast.ui.theme.DragonBlastTheme

class LauncherActivity : AppCompatActivity() {
    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DragonBlastTheme {
                LauncherApp(vm)
            }
        }
        // The in-game menu's "restart" comes back here with play=true.
        if (savedInstanceState == null && intent.getBooleanExtra("play", false)) vm.play(this)
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }
}
