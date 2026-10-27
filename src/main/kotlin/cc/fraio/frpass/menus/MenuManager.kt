package cc.fraio.frpass.menus

import cc.fraio.frpass.FrPass
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class MenuManager(private val plugin: FrPass) {
    enum class MenuType { MAIN, QUESTS, PASS }

    private val openMenus = ConcurrentHashMap<Player, Pair<MenuType, Int>>()
    private val configCache = ConcurrentHashMap<String, YamlConfiguration>()
    private val activeRerolls = ConcurrentHashMap<Player, MutableSet<String>>()

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
                val defaultStream = plugin.getResource("menus/$name")
                if (defaultStream != null) {
                    val defaultConfig = YamlConfiguration.loadConfiguration(InputStreamReader(defaultStream, Charsets.UTF_8))
                    var modified = false
                    for (key in defaultConfig.getKeys(true)) {
                        if (!config.contains(key)) {
                            config.set(key, defaultConfig.get(key))
                            modified = true
                        }
                    }
                    if (modified) {
                        config.save(file)
                    }
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
            configCache[name] = config
            config
        }
    }

    fun setOpenMenu(player: Player, type: MenuType, page: Int) {
        openMenus[player] = Pair(type, page)
    }

    fun getOpenMenu(player: Player): Pair<MenuType, Int>? {
        return openMenus[player]
    }

    fun isPluginMenu(player: Player): Boolean {
        return openMenus.containsKey(player)
    }

    fun removeOpenMenu(player: Player) {
        openMenus.remove(player)
    }

    fun refreshOpenMenus() {
        openMenus.toMap().forEach { (player, pair) ->
            if (player.isOnline) {
                when (pair.first) {
                    MenuType.MAIN -> openMainMenu(player)
                    MenuType.QUESTS -> openQuestsMenu(player, pair.second)
                    MenuType.PASS -> openPassMenu(player, pair.second)
                }
            }
        }
    }

    fun openMainMenu(player: Player) = mainMenu.open(player)
    fun openQuestsMenu(player: Player, page: Int = 1) = questsMenu.open(player, page)
    fun openPassMenu(player: Player, page: Int = 1) = passMenu.open(player, page)

    // Reroll Session Management
    fun isRerollSessionActive(player: Player): Boolean = activeRerolls.containsKey(player)
    fun getRerollSelection(player: Player): MutableSet<String>? = activeRerolls[player]
    fun startRerollSession(player: Player) {
        activeRerolls[player] = mutableSetOf()
    }
    fun stopRerollSession(player: Player) {
        activeRerolls.remove(player)
    }
}
