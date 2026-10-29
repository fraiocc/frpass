package cc.fraio.frpass.utils

import cc.fraio.frpass.FrPass
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

class FrSync(private val plugin: FrPass) {

    private var watcher: WatchService? = null
    private val isRunning = AtomicBoolean(false)
    private val suppressUntil = AtomicLong(0L)
    private val registeredPaths = ConcurrentHashMap.newKeySet<Path>()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "FrSync-Worker").apply { isDaemon = true }
    }
    private var pending: ScheduledFuture<*>? = null
    private val lock = Any()
    @Volatile private var targetFile: String = ""

    fun suppress(durationMs: Long = 2500L) {
        suppressUntil.set(System.currentTimeMillis() + durationMs)
    }

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)

        try {
            watcher = FileSystems.getDefault().newWatchService()
            registerAllDirectories()

            plugin.logger.info("[FrSync] File sync active.")

            plugin.foliaLib.scheduler.runAsync(Consumer { _ ->
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

                    if (changed && System.currentTimeMillis() >= suppressUntil.get()) {
                        reload(file)
                    }

                    if (!key.reset()) break
                }
            })

        } catch (e: Exception) {
            plugin.logger.warning("[FrSync] Failed to initialize: ${e.message}")
        }
    }

    private fun registerAllDirectories() {
        val dataFolder = plugin.dataFolder.toPath()
        register(dataFolder)
        register(dataFolder.resolve("core"))
        register(dataFolder.resolve("core/quests"))
        register(dataFolder.resolve("lang"))
        register(dataFolder.resolve("menus"))
    }

    private fun reload(file: String) {
        synchronized(lock) {
            targetFile = file
            pending?.cancel(false)
            pending = scheduler.schedule({
                if (!isRunning.get() || System.currentTimeMillis() < suppressUntil.get()) return@schedule

                plugin.foliaLib.scheduler.runNextTick(Consumer { _ ->
                    val start = System.currentTimeMillis()
                    registerAllDirectories()
                    plugin.reloadAll()
                    val elapsed = System.currentTimeMillis() - start

                    if (plugin.configManager.config.getBoolean("settings.frsync.notify-console", true)) {
                        plugin.logger.info("[FrSync] Reloaded $targetFile in ${elapsed}ms.")
                    }
                })
            }, 1200L, TimeUnit.MILLISECONDS)
        }
    }

    private fun register(path: Path) {
        try {
            val normalized = path.toAbsolutePath().normalize()
            if (Files.exists(normalized) && Files.isDirectory(normalized) && registeredPaths.add(normalized)) {
                normalized.register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE
                )
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        if (!isRunning.get()) return
        isRunning.set(false)
        synchronized(lock) {
            pending?.cancel(false)
        }
        scheduler.shutdownNow()
        registeredPaths.clear()
        try {
            watcher?.close()
        } catch (_: Exception) {}
    }
}
