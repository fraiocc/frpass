package cc.fraio.frpass.utils

import cc.fraio.frpass.FrPass
import java.nio.file.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

class FrSync(private val plugin: FrPass) {

    private var watcher: WatchService? = null
    private val isRunning = AtomicBoolean(false)
    private var lastReload = AtomicLong(0)

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)
        
        val dataFolder = plugin.dataFolder.toPath()
        
        try {
            watcher = FileSystems.getDefault().newWatchService()
            
            // Auto reload directories
            register(dataFolder)
            register(dataFolder.resolve("core"))
            register(dataFolder.resolve("core/quests"))
            register(dataFolder.resolve("lang"))
            register(dataFolder.resolve("menus"))
            
            plugin.logger.info("[FrSync] File sync active.")

            // Watch events in async thread
            plugin.foliaLib.impl.runAsync(Consumer { _ ->
                while (isRunning.get()) {
                    val key: WatchKey
                    try {
                        key = watcher?.take() ?: break
                    } catch (_: InterruptedException) {
                        break
                    } catch (_: ClosedWatchServiceException) {
                        break
                    }
                    
                    var changed = false
                    var file = ""
                    for (event in key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue
                        
                        val context = event.context() as? Path ?: continue
                        val name = context.toString()
                        if (name.endsWith(".yml", ignoreCase = true)) {
                            changed = true
                            file = name
                        }
                    }
                    
                    if (changed) {
                        val now = System.currentTimeMillis()
                        // 1.5s debounce to prevent multiple reloads on rapid disk writes
                        if (now - lastReload.get() > 1500) { 
                            lastReload.set(now)
                            // Reload on server tick thread for Folia/Paper thread safety
                            plugin.foliaLib.impl.runNextTick(Consumer { _ ->
                                val start = System.currentTimeMillis()
                                plugin.reloadAll()
                                val elapsed = System.currentTimeMillis() - start

                                if (plugin.configManager.config.getBoolean("settings.frsync.notify-console", true)) {
                                    plugin.logger.info("[FrSync] Reloaded $file in ${elapsed}ms.")
                                }
                            })
                        }
                    }
                    
                    if (!key.reset()) {
                        break
                    }
                }
            })
            
        } catch (e: Exception) {
            plugin.logger.warning("[FrSync] Failed to initialize: ${e.message}")
        }
    }
    
    private fun register(path: Path) {
        try {
            if (Files.exists(path) && Files.isDirectory(path)) {
                path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE)
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        if (!isRunning.get()) return
        isRunning.set(false)
        try {
            watcher?.close()
        } catch (_: Exception) {}
    }
}
