package cc.fraio.frpass

import cc.fraio.frpass.api.FrPassAPI
import cc.fraio.frpass.api.FrPassProvider
import cc.fraio.frpass.commands.AdminCommand
import cc.fraio.frpass.commands.PassCommand
import cc.fraio.frpass.editor.QuestEditorListener
import cc.fraio.frpass.editor.QuestEditorManager
import cc.fraio.frpass.hooks.FrPassExpansion
import cc.fraio.frpass.hooks.VaultHook
import cc.fraio.frpass.listeners.InventoryListener
import cc.fraio.frpass.listeners.MenuPacketListener
import cc.fraio.frpass.listeners.PlayerConnectionListener
import cc.fraio.frpass.listeners.QuestListener
import cc.fraio.frpass.listeners.TicketListener
import cc.fraio.frpass.managers.AIManager
import cc.fraio.frpass.managers.ConfigManager
import cc.fraio.frpass.managers.CycleManager
import cc.fraio.frpass.managers.DatabaseManager
import cc.fraio.frpass.managers.LangManager
import cc.fraio.frpass.managers.PlayerDataManager
import cc.fraio.frpass.managers.QuestManager
import cc.fraio.frpass.managers.RewardManager
import cc.fraio.frpass.managers.TicketManager
import cc.fraio.frpass.managers.TierManager
import cc.fraio.frpass.menus.MenuManager
import cc.fraio.frpass.quests.QuestType
import cc.fraio.frpass.tasks.QuestTasks
import cc.fraio.frpass.utils.CommandAliasUtil
import cc.fraio.frpass.utils.FrSync
import cc.fraio.frpass.utils.ItemBuilder
import cc.fraio.frpass.utils.UpdateChecker
import com.tcoded.folialib.FoliaLib
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class FrPass : JavaPlugin(), FrPassAPI {
    
    companion object {
        lateinit var instance: FrPass private set
    }
    
    lateinit var configManager: ConfigManager private set
    lateinit var langManager: LangManager private set
    lateinit var databaseManager: DatabaseManager private set
    lateinit var menuManager: MenuManager private set
    lateinit var rewardManager: RewardManager private set
    lateinit var tierManager: TierManager private set
    lateinit var playerDataManager: PlayerDataManager private set
    lateinit var questManager: QuestManager private set
    lateinit var questTasks: QuestTasks private set
    lateinit var cycleManager: CycleManager private set
    lateinit var updateChecker: UpdateChecker private set
    lateinit var ticketManager: TicketManager private set
    lateinit var aiManager: AIManager private set
    lateinit var questEditorManager: QuestEditorManager private set
    lateinit var frSync: FrSync private set
    lateinit var foliaLib: FoliaLib private set
    var menuPacketListener: MenuPacketListener? = null
        private set
    var passCommand: PassCommand? = null
        private set

    override fun onEnable() {
        val pePlugin = server.pluginManager.getPlugin("packetevents")
        if (pePlugin == null || !pePlugin.isEnabled) {
            logger.severe("==========================================")
            logger.severe("PacketEvents was not found on this server!")
            logger.severe("Please download and install PacketEvents:")
            logger.severe("https://modrinth.com/plugin/packetevents")
            logger.severe("Disabling FrPass...")
            logger.severe("==========================================")
            server.pluginManager.disablePlugin(this)
            return
        }

        instance = this
        foliaLib = FoliaLib(this)
        
        if (VaultHook.setupEconomy()) {
            logger.info("Vault hooked successfully!")
        } else {
            logger.warning("Vault not found! Money rewards will not work.")
        }
        
        configManager = ConfigManager(this)
        configManager.loadAll()
        
        langManager = LangManager(this)
        langManager.load()
        
        databaseManager = DatabaseManager(this)
        databaseManager.connect()
        
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            FrPassExpansion(this).register()
        }
        
        tierManager = TierManager(this)
        tierManager.loadAll()
        
        questManager = QuestManager(this)
        questManager.loadAll()
        
        rewardManager = RewardManager(this)
        cycleManager = CycleManager(this)
        
        playerDataManager = PlayerDataManager(this)
        playerDataManager.init()
        
        menuManager = MenuManager(this)
        questEditorManager = QuestEditorManager(this)
        
        questTasks = QuestTasks(this)
        questTasks.startTasks()
        
        ticketManager = TicketManager(this)
        aiManager = AIManager(this)
        
        server.pluginManager.apply {
            registerEvents(InventoryListener(this@FrPass), this@FrPass)
            registerEvents(PlayerConnectionListener(this@FrPass), this@FrPass)
            registerEvents(QuestListener(this@FrPass), this@FrPass)
            registerEvents(TicketListener(this@FrPass), this@FrPass)
            registerEvents(QuestEditorListener(this@FrPass), this@FrPass)
        }
        menuPacketListener = MenuPacketListener(this).also { it.register() }
        
        frSync = FrSync(this)
        frSync.start()
        
        val passCmd = PassCommand(this)
        passCommand = passCmd
        getCommand("frpass")?.let {
            it.setExecutor(passCmd)
            it.tabCompleter = passCmd
        }
        getCommand("frpassadmin")?.let {
            val adminCmd = AdminCommand(this)
            it.setExecutor(adminCmd)
            it.tabCompleter = adminCmd
        }
        
        CommandAliasUtil.registerAliases(this)
        
        FrPassProvider.register(this)
        
        updateChecker = UpdateChecker(this)
        updateChecker.checkForUpdates()
        
        val pluginId = 33981
        org.bstats.bukkit.Metrics(this, pluginId)

        logger.info("FrPass enabled successfully!")
    }

    fun reloadAll() {
        ItemBuilder.clearCache()
        configManager.reload()
        langManager.load()
        tierManager.loadAll()
        questManager.loadAll()
        playerDataManager.syncOnlinePlayers()
        menuManager.loadMenuConfigs()
        menuManager.refreshOpenMenus()
        questEditorManager.refreshOpenEditors()
        CommandAliasUtil.registerAliases(this)
        if (this::aiManager.isInitialized) {
            aiManager.reload()
        }
    }

    override fun onDisable() {
        if (::frSync.isInitialized) {
            frSync.stop()
        }
        if (::questTasks.isInitialized) {
            questTasks.stopTasks()
        }
        if (::playerDataManager.isInitialized) {
            playerDataManager.shutdown()
        }
        if (this::databaseManager.isInitialized) {
            databaseManager.disconnect()
        }
        if (this::aiManager.isInitialized) {
            aiManager.onDisable()
        }
        menuPacketListener?.unregister()
        menuPacketListener = null
        logger.info("FrPass has been disabled!")
    }

    // FrPassAPI Implementation

    override fun getPlayerLevel(uuid: UUID): Int {
        return playerDataManager.getPlayer(uuid)?.level ?: 0
    }

    override fun getPlayerXp(uuid: UUID): Int {
        return playerDataManager.getPlayer(uuid)?.xp ?: 0
    }

    override fun isPremium(uuid: UUID): Boolean {
        return playerDataManager.getPlayer(uuid)?.premium ?: false
    }

    override fun setPremium(uuid: UUID, premium: Boolean) {
        val data = playerDataManager.getPlayer(uuid) ?: return
        data.premium = premium
        playerDataManager.savePlayer(uuid)
    }

    override fun addXp(uuid: UUID, amount: Int) {
        playerDataManager.addXp(uuid, amount)
    }

    override fun setXp(uuid: UUID, xp: Int) {
        playerDataManager.setXp(uuid, xp)
    }

    override fun setLevel(uuid: UUID, level: Int) {
        playerDataManager.setLevel(uuid, level)
    }

    override fun resetPlayer(uuid: UUID) {
        playerDataManager.resetPlayer(uuid)
    }

    override fun giveTicket(player: Player, amount: Int) {
        ticketManager.giveTicket(player, amount)
    }

    override fun completeQuest(player: Player, questId: String): Boolean {
        return questManager.completeQuest(player, questId)
    }

    override fun getUnclaimedRewardCount(uuid: UUID): Int {
        return playerDataManager.getUnclaimedRewardCount(uuid)
    }

    override fun getRequiredXpForNextLevel(uuid: UUID): Int {
        val data = playerDataManager.getPlayer(uuid) ?: return 0
        val nextTier = tierManager.getTier(data.level + 1)
        return nextTier?.requiredXp ?: 0
    }

    override fun getRemainingXpForNextLevel(uuid: UUID): Int {
        val data = playerDataManager.getPlayer(uuid) ?: return 0
        val nextTier = tierManager.getTier(data.level + 1)
        return if (nextTier != null) (nextTier.requiredXp - data.xp).coerceAtLeast(0) else 0
    }

    override fun getActiveQuests(uuid: UUID): List<String> {
        return playerDataManager.getPlayer(uuid)?.activeQuests?.toList() ?: emptyList()
    }

    override fun getQuestProgress(uuid: UUID, questId: String): Int {
        return playerDataManager.getPlayer(uuid)?.questProgress?.get(questId) ?: 0
    }

    override fun addQuestProgress(player: Player, type: QuestType, amount: Int) {
        questManager.handleProgress(player, type, null, amount)
    }

    override fun addQuestProgress(player: Player, questId: String, amount: Int): Boolean {
        return questManager.addQuestProgress(player, questId, amount)
    }
}
