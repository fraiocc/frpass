package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.ai.AIClient
import cc.fraio.frpass.ai.AIQuestStorage
import cc.fraio.frpass.utils.msg
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration

class AIManager(private val plugin: FrPass) {

    val client: AIClient = AIClient()
    val storage: AIQuestStorage = AIQuestStorage(plugin)

    companion object {
        const val API_URL: String = "https://ai.fraio.cc/v1/chat/completions"
        const val MODEL_NAME: String = "flay-0.1-coder"
        const val TEMPERATURE: Double = 0.1
        const val MAX_TOKENS: Int = 400
        const val TIMEOUT_SECONDS: Long = 25L
    }

    init {
        if (isEnabled()) {
            plugin.logger.info("[AI] AI module initialized.")
        }
    }

    fun reload() {}

    fun onDisable() {}

    fun isEnabled(): Boolean = plugin.configManager.config.getBoolean("ai.enabled", true)

    fun showStatus(sender: CommandSender) {
        val enabledStr = if (isEnabled()) "&aYES" else "&cNO"
        sender.sendMessage(sender.msg("messages.ai.status-format", "%enabled%" to enabledStr, "%model%" to MODEL_NAME))
    }

    fun createQuest(
        sender: CommandSender,
        prompt: String,
        targetFile: String? = null
    ) {
        if (!isEnabled()) {
            sender.sendMessage(sender.msg("messages.ai.disabled"))
            return
        }

        sender.sendMessage(sender.msg("messages.ai.generating", "%prompt%" to prompt))

        client.complete(
            apiUrl = API_URL,
            modelName = MODEL_NAME,
            temperature = TEMPERATURE,
            maxTokens = MAX_TOKENS,
            timeoutSeconds = TIMEOUT_SECONDS,
            prompt = prompt
        ).thenAccept { yaml ->
            plugin.foliaLib.impl.runNextTick { _ ->
                try {
                    val config = YamlConfiguration()
                    config.loadFromString(yaml)
                    val section = config.getConfigurationSection("quests")
                    if (section == null || section.getKeys(false).isEmpty()) {
                        sender.sendMessage(sender.msg("messages.ai.invalid-yaml"))
                        return@runNextTick
                    }

                    for (key in section.getKeys(false)) {
                        val sub = section.getConfigurationSection(key) ?: continue
                        val (file, isUpdate) = storage.saveQuest(key, sub, targetFile)
                        if (isUpdate) {
                            sender.sendMessage(sender.msg("messages.ai.success-updated", "%quest%" to key, "%file%" to file.name))
                        } else {
                            sender.sendMessage(sender.msg("messages.ai.success-created", "%quest%" to key, "%file%" to file.name))
                        }
                    }

                    plugin.questManager.loadAll()
                    sender.sendMessage(sender.msg("messages.ai.reloaded"))
                } catch (e: Exception) {
                    sender.sendMessage(sender.msg("messages.ai.error-save", "%error%" to (e.message ?: "unknown")))
                }
            }
        }.exceptionally { throwable ->
            plugin.foliaLib.impl.runNextTick { _ ->
                sender.sendMessage(sender.msg("messages.ai.failed", "%error%" to (throwable.cause?.message ?: throwable.message ?: "unknown")))
            }
            null
        }
    }

    fun editQuest(
        sender: CommandSender,
        questId: String,
        prompt: String
    ) {
        if (!isEnabled()) {
            sender.sendMessage(sender.msg("messages.ai.disabled"))
            return
        }

        val pair = storage.readQuest(questId)
        if (pair == null) {
            sender.sendMessage(sender.msg("messages.ai.quest-not-found", "%quest%" to questId))
            return
        }

        val (file, existingYaml) = pair
        sender.sendMessage(sender.msg("messages.ai.modifying", "%quest%" to questId, "%file%" to file.name))

        val instruction = """
            Modify this existing quest configuration according to the following instructions:
            $prompt

            Existing YAML:
            $existingYaml
        """.trimIndent()

        client.complete(
            apiUrl = API_URL,
            modelName = MODEL_NAME,
            temperature = TEMPERATURE,
            maxTokens = MAX_TOKENS,
            timeoutSeconds = TIMEOUT_SECONDS,
            prompt = instruction
        ).thenAccept { yaml ->
            plugin.foliaLib.impl.runNextTick { _ ->
                try {
                    val config = YamlConfiguration()
                    config.loadFromString(yaml)
                    val section = config.getConfigurationSection("quests")
                    val sub = section?.getConfigurationSection(questId) ?: section?.getConfigurationSection(section.getKeys(false).firstOrNull() ?: "")

                    if (sub == null) {
                        sender.sendMessage(sender.msg("messages.ai.invalid-yaml"))
                        return@runNextTick
                    }

                    val (target, _) = storage.saveQuest(questId, sub, file.name)
                    sender.sendMessage(sender.msg("messages.ai.success-updated", "%quest%" to questId, "%file%" to target.name))
                    plugin.questManager.loadAll()
                    sender.sendMessage(sender.msg("messages.ai.reloaded"))
                } catch (e: Exception) {
                    sender.sendMessage(sender.msg("messages.ai.error-save", "%error%" to (e.message ?: "unknown")))
                }
            }
        }.exceptionally { throwable ->
            plugin.foliaLib.impl.runNextTick { _ ->
                sender.sendMessage(sender.msg("messages.ai.failed", "%error%" to (throwable.cause?.message ?: throwable.message ?: "unknown")))
            }
            null
        }
    }
}
