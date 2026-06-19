package cc.fraio.frpass.utils

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.utils.ColorUtils
import java.nio.file.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

class ConfigWatcher(private val plugin: FrPass) {

    private var watchService: WatchService? = null
    private val isRunning = AtomicBoolean(false)
    private var lastReloadTime = AtomicLong(0)

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)
        
        val dataFolder = plugin.dataFolder.toPath()
        
        try {
            watchService = FileSystems.getDefault().newWatchService()
            
            // Register paths safely
            registerPath(dataFolder)
            registerPath(dataFolder.resolve("core"))
            registerPath(dataFolder.resolve("core/quests"))
            registerPath(dataFolder.resolve("lang"))
            registerPath(dataFolder.resolve("menus"))
            
            Thread({
                while (isRunning.get()) {
                    val key: WatchKey
                    try {
                        key = watchService?.take() ?: break
                    } catch (e: InterruptedException) {
                        break
                    } catch (e: ClosedWatchServiceException) {
                        break
                    }
                    
                    var reloadNeeded = false
                    for (event in key.pollEvents()) {
                        val kind = event.kind()
                        if (kind == StandardWatchEventKinds.OVERFLOW) continue
                        
                        val context = event.context() as? Path ?: continue
                        if (context.toString().endsWith(".yml")) {
                            reloadNeeded = true
                        }
                    }
                    
                    if (reloadNeeded) {
                        val now = System.currentTimeMillis()
                        // 1.5 second debounce logic
                        if (now - lastReloadTime.get() > 1500) { 
                            lastReloadTime.set(now)
                            // Run synchronously on the main server thread
                            plugin.foliaLib.impl.runNextTick(Consumer { _ ->
                                plugin.reloadAll()
                            })
                        }
                    }
                    
                    if (!key.reset()) {
                        break
                    }
                }
            }, "FrPass-ConfigWatcher").start()
            
        } catch (e: Exception) {
            plugin.logger.warning("Could not start ConfigWatcher: ${e.message}")
        }
    }
    
    private fun registerPath(path: Path) {
        try {
            if (Files.exists(path) && Files.isDirectory(path)) {
                path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
            }
        } catch (ignored: Exception) {}
    }

    fun stop() {
        isRunning.set(false)
        try {
            watchService?.close()
        } catch (ignored: Exception) {}
    }
}
