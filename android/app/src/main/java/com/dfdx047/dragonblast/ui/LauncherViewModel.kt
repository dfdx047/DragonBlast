package com.dfdx047.dragonblast.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dfdx047.dragonblast.DragonBlastApp
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.GameLib
import com.dfdx047.dragonblast.data.Updater
import com.dfdx047.dragonblast.data.GameLibException
import com.dfdx047.dragonblast.data.AppPaths
import com.dfdx047.dragonblast.data.Diagnostics
import com.dfdx047.dragonblast.data.Engine
import com.dfdx047.dragonblast.data.GpuInfo
import com.dfdx047.dragonblast.data.InstallState
import com.dfdx047.dragonblast.data.IsoFailure
import com.dfdx047.dragonblast.data.IsoInstaller
import com.dfdx047.dragonblast.data.Settings
import com.dfdx047.dragonblast.data.SettingsStore
import com.dfdx047.dragonblast.drivers.DriverCatalog
import com.dfdx047.dragonblast.drivers.DriverException
import com.dfdx047.dragonblast.drivers.DriverSource
import com.dfdx047.dragonblast.drivers.DriverStore
import com.dfdx047.dragonblast.drivers.InstalledDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList())

/** A long job: the bar under the header shows it. */
data class Busy(@StringRes val label: Int, val detail: String = "", val progress: Float? = null)

/** A driver source's list on the Drivers tab. */
sealed interface SourceState {
    data object Loading : SourceState
    data class Ready(val listing: DriverCatalog.Listing) : SourceState
    data class Failed(val error: String) : SourceState
}

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)

    var settings by mutableStateOf(store.load())
        private set
    var install by mutableStateOf<InstallState?>(null)
        private set
    var gpu by mutableStateOf<GpuInfo?>(null)
        private set
    var busy by mutableStateOf<Busy?>(null)
        private set
    var message by mutableStateOf<UiMessage?>(null)
    /** Long error of the last import, shown in the game files card. */
    var importError by mutableStateOf<String?>(null)
        private set
    var showNoEngine by mutableStateOf(false)
    /** A newer release found by the updater (dialog on screen while set). */
    var update by mutableStateOf<Updater.Release?>(null)
    /** Last check's result for the About card: null (none yet), "" up to date, or an error. */
    var updateStatus by mutableStateOf<String?>(null)
    var checkingUpdate by mutableStateOf(false)
    /** Bumps when the game gets installed, so Home can play the "ready" transition once. */
    var installedPulse by mutableStateOf(0)
        private set

    var installedDrivers by mutableStateOf<List<InstalledDriver>>(emptyList())
        private set
    var driverChoice by mutableStateOf(DriverStore.CHOICE_AUTO)
        private set
    var activeDriver by mutableStateOf<String?>(null)
        private set
    val sources = mutableStateMapOf<String, SourceState>()
    var downloading by mutableStateOf<String?>(null)
        private set

    val engineAvailable: Boolean = Engine.available(app)

    init {
        refresh()
        if (settings.autoUpdates && Updater.autoCheckDue(app)) checkUpdate(manual = false)
        viewModelScope.launch(Dispatchers.IO) {
            val g = GpuInfo.get(app)
            withContext(Dispatchers.Main) { gpu = g }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val st = InstallState.scan()
            val list = DriverStore.installed()
            val ctx = getApplication<Application>()
            withContext(Dispatchers.Main) {
                install = st
                // A play request that came while the files were still being checked (game menu "restart").
                pendingPlay?.let { ctx2 ->
                    pendingPlay = null
                    if (st.playable) play(ctx2)
                }
                installedDrivers = list
                driverChoice = DriverStore.choice(ctx)
                activeDriver = DriverStore.activeName(ctx)
            }
        }
    }

    fun update(transform: (Settings) -> Settings) {
        val old = settings
        val s = transform(old)
        settings = s
        store.save(s)
        if (s.language != old.language) DragonBlastApp.applyLanguage(s.language)
    }

    fun resetSettings() {
        store.reset()
        settings = store.load()
        message = UiMessage(R.string.msg_settings_reset)
    }

    // ---- game files ----

    fun importIso(uri: Uri) {
        if (busy != null) return
        importError = null
        val ctx = getApplication<Application>()
        busy = Busy(R.string.busy_import)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var last = 0L
                val (files, bytes) = IsoInstaller.install(ctx, uri) { done, total, file ->
                    val now = System.currentTimeMillis()
                    if (now - last > 120) {
                        last = now
                        busy = Busy(R.string.busy_import, file.substringAfterLast('/'), if (total > 0) done.toFloat() / total else null)
                    }
                }
                val st = InstallState.scan()
                withContext(Dispatchers.Main) {
                    install = st
                    busy = null
                    installedPulse++
                    message = UiMessage(R.string.msg_import_ok, listOf(files, String.format(java.util.Locale.ROOT, "%.2f", bytes / 1e9)))
                }
            } catch (f: IsoFailure) {
                withContext(Dispatchers.Main) {
                    busy = null
                    importError = isoError(ctx, f)
                    message = UiMessage(R.string.msg_import_failed)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    busy = null
                    importError = ctx.getString(R.string.iso_err_io, e.message ?: "")
                    message = UiMessage(R.string.msg_import_failed)
                }
            }
        }
    }

    private fun isoError(ctx: Context, f: IsoFailure): String = when (f.reason) {
        IsoFailure.Reason.OPEN -> ctx.getString(R.string.iso_err_open)
        IsoFailure.Reason.NOT_XBOX -> ctx.getString(R.string.iso_err_not_xbox)
        IsoFailure.Reason.INCOMPLETE -> ctx.getString(R.string.iso_err_incomplete, f.detail)
        IsoFailure.Reason.NO_XEX -> ctx.getString(R.string.iso_err_no_xex)
        IsoFailure.Reason.NO_SPACE -> f.detail.split('|').let { ctx.getString(R.string.iso_err_space, it.getOrElse(0) { "?" }, it.getOrElse(1) { "?" }) }
        IsoFailure.Reason.ZEROED -> ctx.getString(R.string.iso_err_zeroed, f.detail)
        IsoFailure.Reason.WRONG_VERSION -> ctx.getString(R.string.iso_err_version)
        IsoFailure.Reason.ORIGINAL_RELEASE -> ctx.getString(R.string.iso_err_original)
        IsoFailure.Reason.DAMAGED_TABLE -> ctx.getString(R.string.iso_err_table)
        IsoFailure.Reason.MOVE -> ctx.getString(R.string.iso_err_move)
        IsoFailure.Reason.IO -> ctx.getString(R.string.iso_err_io, f.detail)
    }

    fun deleteGame() {
        if (busy != null) return
        busy = Busy(R.string.busy_delete)
        viewModelScope.launch(Dispatchers.IO) {
            AppPaths.game.deleteRecursively()
            val st = InstallState.scan()
            withContext(Dispatchers.Main) {
                install = st
                busy = null
                message = UiMessage(R.string.msg_game_deleted)
            }
        }
    }

    private var pendingPlay: Context? = null

    fun play(context: Context) {
        if (!engineAvailable) {
            showNoEngine = true
            return
        }
        val st = install
        if (st == null) {
            // Still scanning the game files: play once the scan ends.
            pendingPlay = context
            return
        }
        if (!st.playable) return
        val ctx = getApplication<Application>()
        if (GameLib.ready(ctx)) {
            startGame(context)
            return
        }
        // First start, or an update brought another game build: decrypt it with the player's default.xex (GameLib).
        if (busy != null) return
        importError = null
        busy = Busy(R.string.busy_unpack)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var last = 0L
                GameLib.install(ctx) { done, total ->
                    val now = System.currentTimeMillis()
                    if (now - last > 120) {
                        last = now
                        busy = Busy(R.string.busy_unpack, "", if (total > 0) done.toFloat() / total else null)
                    }
                }
                withContext(Dispatchers.Main) {
                    busy = null
                    startGame(context)
                }
            } catch (e: GameLibException) {
                withContext(Dispatchers.Main) {
                    busy = null
                    importError = when (e.reason) {
                        GameLibException.Reason.UNSUPPORTED_VERSION -> ctx.getString(R.string.unpack_err_version, e.detail)
                        GameLibException.Reason.NO_XEX -> ctx.getString(R.string.iso_err_no_xex)
                        GameLibException.Reason.NO_ENGINE -> ctx.getString(R.string.unpack_err_engine)
                        GameLibException.Reason.IO -> ctx.getString(R.string.iso_err_io, e.detail)
                    }
                    message = UiMessage(R.string.msg_unpack_failed)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    busy = null
                    importError = ctx.getString(R.string.iso_err_io, e.message ?: e.javaClass.simpleName)
                    message = UiMessage(R.string.msg_unpack_failed)
                }
            }
        }
    }

    // ---- updates ----

    fun checkUpdate(manual: Boolean = true) {
        if (checkingUpdate) return
        checkingUpdate = true
        viewModelScope.launch(Dispatchers.IO) {
            val r = runCatching { Updater.check() }
            withContext(Dispatchers.Main) {
                checkingUpdate = false
                r.onSuccess { rel ->
                    updateStatus = ""
                    if (rel != null) update = rel else if (manual) message = UiMessage(R.string.msg_update_none)
                }.onFailure { e ->
                    updateStatus = e.message ?: e.javaClass.simpleName
                    if (manual) message = UiMessage(R.string.msg_update_failed)
                }
            }
        }
    }

    fun installUpdate(context: Context, r: Updater.Release) {
        if (busy != null) return
        if (!Updater.canInstall(context)) {
            message = UiMessage(R.string.msg_update_permission)
            Updater.openInstallPermission(context)
            return
        }
        update = null
        busy = Busy(R.string.busy_update, r.version)
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var last = 0L
                val apk = Updater.download(ctx, r) { done, total ->
                    val now = System.currentTimeMillis()
                    if (now - last > 120) {
                        last = now
                        busy = Busy(R.string.busy_update, r.version, if (total > 0) done.toFloat() / total else null)
                    }
                }
                withContext(Dispatchers.Main) {
                    busy = null
                    Updater.install(context, apk)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    busy = null
                    updateStatus = e.message ?: e.javaClass.simpleName
                    message = UiMessage(R.string.msg_update_failed)
                }
            }
        }
    }

    private fun startGame(context: Context) {
        val intent = Intent().setClassName(context, "org.rb2recomp.android.GameActivity")
        context.startActivity(intent)
    }

    // ---- drivers ----

    fun loadSource(source: DriverSource, force: Boolean = false) {
        val cur = sources[source.key]
        if (!force && (cur is SourceState.Ready || cur is SourceState.Loading)) return
        sources[source.key] = SourceState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val st = try {
                SourceState.Ready(DriverCatalog.releases(source, force))
            } catch (e: Exception) {
                SourceState.Failed(e.message ?: e.javaClass.simpleName)
            }
            withContext(Dispatchers.Main) { sources[source.key] = st }
        }
    }

    fun downloadDriver(asset: DriverCatalog.Asset) {
        if (busy != null) return
        downloading = asset.url
        busy = Busy(R.string.busy_driver, asset.name)
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var last = 0L
                val d = DriverStore.download(asset) { done, total ->
                    val now = System.currentTimeMillis()
                    if (now - last > 120) {
                        last = now
                        busy = Busy(R.string.busy_driver, asset.name, if (total > 0) done.toFloat() / total else null)
                    }
                }
                DriverStore.activate(ctx, d)
                withContext(Dispatchers.Main) {
                    message = UiMessage(R.string.msg_driver_ready, listOf(d.name))
                }
            } catch (e: DriverException) {
                withContext(Dispatchers.Main) { message = driverError(e) }
            } finally {
                withContext(Dispatchers.Main) {
                    busy = null
                    downloading = null
                    refresh()
                }
            }
        }
    }

    fun importDriver(uri: Uri) {
        if (busy != null) return
        val ctx = getApplication<Application>()
        val name = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "driver.zip"
        busy = Busy(R.string.busy_driver, name)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val d = DriverStore.import(ctx, uri, name)
                DriverStore.activate(ctx, d)
                withContext(Dispatchers.Main) { message = UiMessage(R.string.msg_driver_ready, listOf(d.name)) }
            } catch (e: DriverException) {
                withContext(Dispatchers.Main) { message = driverError(e) }
            } finally {
                withContext(Dispatchers.Main) {
                    busy = null
                    refresh()
                }
            }
        }
    }

    private fun driverError(e: DriverException) = when (e.reason) {
        DriverException.Reason.NETWORK -> UiMessage(R.string.drv_err_network)
        DriverException.Reason.NOT_ZIP -> UiMessage(R.string.drv_err_zip)
        DriverException.Reason.NO_LIBRARY -> UiMessage(R.string.drv_err_lib)
        DriverException.Reason.MIN_API -> UiMessage(R.string.drv_err_api, listOf(e.message ?: ""))
        DriverException.Reason.IO -> UiMessage(R.string.drv_err_io)
    }

    fun activateDriver(d: InstalledDriver) {
        DriverStore.activate(getApplication(), d)
        refresh()
    }

    fun selectDriver(choice: Int) {
        DriverStore.select(getApplication(), choice)
        refresh()
    }

    fun deleteDriver(d: InstalledDriver) {
        DriverStore.delete(getApplication(), d)
        refresh()
    }

    // ---- debug ----

    fun exportLogs(context: Context) {
        if (busy != null) return
        busy = Busy(R.string.busy_logs)
        viewModelScope.launch(Dispatchers.IO) {
            val intent = runCatching { Diagnostics.exportLogs(context, settings) }.getOrNull()
            withContext(Dispatchers.Main) {
                busy = null
                if (intent != null) {
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.dbg_export)))
                } else {
                    message = UiMessage(R.string.msg_logs_failed)
                }
            }
        }
    }

    fun clearShaderCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = Diagnostics.dirSize(AppPaths.cache)
            AppPaths.cache.deleteRecursively()
            withContext(Dispatchers.Main) {
                message = UiMessage(R.string.msg_cache_cleared, listOf(com.dfdx047.dragonblast.ui.components.formatBytes(size)))
            }
        }
    }
}
