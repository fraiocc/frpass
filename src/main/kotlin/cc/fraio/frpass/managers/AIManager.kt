package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import cc.fraio.frpass.ai.AIClient
import cc.fraio.frpass.ai.AIQuestStorage
import cc.fraio.frpass.quests.QuestType
import cc.fraio.frpass.utils.msg
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import java.util.function.Consumer

class AIManager(private val plugin: FrPass) {

    val client: AIClient = AIClient()
    val storage: AIQuestStorage = AIQuestStorage(plugin)

    companion object {
        const val API_URL: String = "https://ai.fraio.cc/v1/chat/completions"
        const val MODEL_NAME: String = "flay-0.1-coder"
        const val TEMPERATURE: Double = 0.1
        const val MAX_TOKENS: Int = 400
        const val TIMEOUT_SECONDS: Long = 25L
        private val KEY_REGEX = Regex("^[a-zA-Z0-9_-]{1,64}$")
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

    private fun validate(config: YamlConfiguration): String? {
        val section = config.getConfigurationSection("quests")
            ?: return "Missing 'quests' root section."

        val keys = section.getKeys(false)
        if (keys.isEmpty()) return "'quests' section is empty."

        val validTypes = QuestType.values().map { it.name.uppercase() }.toSet()
        for (key in keys) {
            if (!KEY_REGEX.matches(key)) {
                return "Quest ID '$key' is invalid (allowed: a-z, 0-9, _, -)."
            }
            val sub = section.getConfigurationSection(key)
                ?: return "Quest '$key' is not a section."
            val rawType = sub.getString("type")?.uppercase()
                ?: return "Quest '$key' has no 'type'."
            if (rawType !in validTypes) {
                return "Quest '$key' has invalid type '$rawType'."
            }
        }
        return null
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
            plugin.foliaLib.scheduler.runNextTick(Consumer { _ ->
                try {
                    val config = YamlConfiguration()
                    config.loadFromString(yaml)

                    val error = validate(config)
                    if (error != null) {
                        sender.sendMessage(sender.msg("messages.ai.invalid-yaml"))
                        plugin.logger.warning("[AI] Quest rejected: $error")
                        return@Consumer
                    }

                    val section = config.getConfigurationSection("quests")!!
                    val questMap = mutableMapOf<String, org.bukkit.configuration.ConfigurationSection>()
                    for (key in section.getKeys(false)) {
                        val sub = section.getConfigurationSection(key) ?: continue
                        questMap[key] = sub
                    }

                    val saved = storage.saveAll(questMap, targetFile)
                    for (res in saved) {
                        if (res.isUpdate) {
                            sender.sendMessage(sender.msg("messages.ai.success-updated", "%quest%" to res.key, "%file%" to res.file.name))
                        } else {
                            sender.sendMessage(sender.msg("messages.ai.success-created", "%quest%" to res.key, "%file%" to res.file.name))
                        }
                    }

                    plugin.questManager.loadAll()
                    sender.sendMessage(sender.msg("messages.ai.reloaded"))
                } catch (e: Exception) {
                    sender.sendMessage(sender.msg("messages.ai.error-save", "%error%" to (e.message ?: "unknown")))
                }
            })
        }.exceptionally { throwable ->
            plugin.foliaLib.scheduler.runNextTick(Consumer { _ ->
                sender.sendMessage(sender.msg("messages.ai.failed", "%error%" to (throwable.cause?.message ?: throwable.message ?: "unknown")))
            })
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

        if (!KEY_REGEX.matches(questId)) {
            sender.sendMessage(sender.msg("messages.ai.quest-not-found", "%quest%" to questId))
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
            plugin.foliaLib.scheduler.runNextTick(Consumer { _ ->
                try {
                    val config = YamlConfiguration()
                    config.loadFromString(yaml)

                    val error = validate(config)
                    if (error != null) {
                        sender.sendMessage(sender.msg("messages.ai.invalid-yaml"))
                        plugin.logger.warning("[AI] Quest rejected: $error")
                        return@Consumer
                    }

                    val section = config.getConfigurationSection("quests")!!
                    val sub = section.getConfigurationSection(questId)
                        ?: section.getConfigurationSection(section.getKeys(false).firstOrNull() ?: "")

                    if (sub == null) {
                        sender.sendMessage(sender.msg("messages.ai.invalid-yaml"))
                        return@Consumer
                    }

                    val (target, _) = storage.saveQuest(questId, sub, file.name)
                    sender.sendMessage(sender.msg("messages.ai.success-updated", "%quest%" to questId, "%file%" to target.name))
                    plugin.questManager.loadAll()
                    sender.sendMessage(sender.msg("messages.ai.reloaded"))
                } catch (e: Exception) {
                    sender.sendMessage(sender.msg("messages.ai.error-save", "%error%" to (e.message ?: "unknown")))
                }
            })
        }.exceptionally { throwable ->
            plugin.foliaLib.scheduler.runNextTick(Consumer { _ ->
                sender.sendMessage(sender.msg("messages.ai.failed", "%error%" to (throwable.cause?.message ?: throwable.message ?: "unknown")))
            })
            null
        }
    }
}
