package cc.fraio.frpass.menus

import cc.fraio.frpass.FrPass
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

class MenuInventoryHolder(val type: MenuManager.MenuType, val page: Int) : InventoryHolder {
    override fun getInventory(): Inventory = error("FrPass Menu GUI")
}

class MenuManager(private val plugin: FrPass) {
    enum class MenuType { MAIN, QUESTS, PASS }

    private val openMenus = ConcurrentHashMap<UUID, Pair<MenuType, Int>>()
    private val configCache = ConcurrentHashMap<String, YamlConfiguration>()
    private val activeRerolls = ConcurrentHashMap<UUID, MutableSet<String>>()

    val mainMenu = MainMenu(plugin)
    val questsMenu = QuestsMenu(plugin)
    val passMenu = PassMenu(plugin)

    init {
        saveDefaultMenus()
        loadMenuConfigs()
    }

    private fun saveDefaultMenus() {
        val autoGenerate = plugin.configManager.config.getBoolean("settings.generate-default-files", true)
        if (!autoGenerate) return

        listOf("main_menu.yml", "quests_menu.yml", "pass_menu.yml", "editor_list_menu.yml", "editor_edit_menu.yml").forEach { fileName ->
            val file = File(plugin.dataFolder, "menus/$fileName")
            if (!file.exists()) {
                file.parentFile.mkdirs()
                plugin.saveResource("menus/$fileName", false)
            }
        }
    }

    fun loadMenuConfigs() {
        configCache.clear()
        val menuNames = listOf("main_menu.yml", "quests_menu.yml", "pass_menu.yml", "editor_list_menu.yml", "editor_edit_menu.yml")
        for (name in menuNames) {
            val file = File(plugin.dataFolder, "menus/$name")
            if (file.exists()) {
                val config = YamlConfiguration.loadConfiguration(file)
                plugin.getResource("menus/$name")?.use { defaultStream ->
                    val defaultConfig = YamlConfiguration.loadConfiguration(InputStreamReader(defaultStream, Charsets.UTF_8))
                    config.setDefaults(defaultConfig)
                }
                configCache[name] = config
            }
        }
    }

    fun getMenuConfig(name: String): YamlConfiguration {
        return configCache[name] ?: run {
            val file = File(plugin.dataFolder, "menus/$name")
            if (!file.exists() && plugin.configManager.config.getBoolean("settings.generate-default-files", true)) {
                file.parentFile.mkdirs()
                plugin.saveResource("menus/$name", false)
            }
            val config = YamlConfiguration.loadConfiguration(file)
            plugin.getResource("menus/$name")?.use { defaultStream ->
                val defaultConfig = YamlConfiguration.loadConfiguration(InputStreamReader(defaultStream, Charsets.UTF_8))
                config.setDefaults(defaultConfig)
            }
            configCache[name] = config
            config
        }
    }

    fun resolveRerollSlot(size: Int, menuConfig: YamlConfiguration): Int {
        val configured = plugin.configManager.config.getInt("reroll.slot", -1)
        if (configured in 0 until size) return configured

        val reserved = mutableSetOf<Int>()
        reserved.addAll(menuConfig.getIntegerList("menu.quest-slots"))
        reserved.addAll(menuConfig.getIntegerList("menu.items.back_button.slots"))
        reserved.addAll(menuConfig.getIntegerList("menu.items.prev_page.slots"))
        reserved.addAll(menuConfig.getIntegerList("menu.items.next_page.slots"))

        val bottomStart = (size - 9).coerceAtLeast(0)
        val candidates = listOf(4, 5, 6, 7, 8, 3, 2, 1, 0).map { bottomStart + it }.filter { it in 0 until size }
        return candidates.firstOrNull { it !in reserved } ?: (bottomStart + 4).coerceAtMost(size - 1)
    }

    fun setOpenMenu(player: Player, type: MenuType, page: Int) {
        openMenus[player.uniqueId] = Pair(type, page)
    }

    fun getOpenMenu(player: Player): Pair<MenuType, Int>? {
        return openMenus[player.uniqueId]
    }

    fun isPluginMenu(player: Player): Boolean {
        return openMenus.containsKey(player.uniqueId)
    }

    fun removeOpenMenu(player: Player) {
        openMenus.remove(player.uniqueId)
    }

    fun refreshOpenMenus() {
        openMenus.toMap().forEach { (uuid, pair) ->
            val player = Bukkit.getPlayer(uuid)
            if (player != null && player.isOnline) {
                plugin.foliaLib.scheduler.runAtEntity(player, Consumer { _ ->
                    if (!player.isOnline) return@Consumer
                    when (pair.first) {
                        MenuType.MAIN -> openMainMenu(player)
                        MenuType.QUESTS -> openQuestsMenu(player, pair.second)
                        MenuType.PASS -> openPassMenu(player, pair.second)
                    }
                })
            } else {
                openMenus.remove(uuid)
            }
        }
    }

    fun openMainMenu(player: Player) = mainMenu.open(player)
    fun openQuestsMenu(player: Player, page: Int = 1) = questsMenu.open(player, page)
    fun openPassMenu(player: Player, page: Int = 1) = passMenu.open(player, page)

    fun isRerollSessionActive(player: Player): Boolean = activeRerolls.containsKey(player.uniqueId)
    fun getRerollSelection(player: Player): MutableSet<String>? = activeRerolls[player.uniqueId]
    fun startRerollSession(player: Player) {
        activeRerolls[player.uniqueId] = ConcurrentHashMap.newKeySet()
    }
    fun stopRerollSession(player: Player) {
        activeRerolls.remove(player.uniqueId)
    }
}
