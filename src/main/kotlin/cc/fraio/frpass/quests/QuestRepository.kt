package cc.fraio.frpass.quests

import cc.fraio.frpass.FrPass
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class QuestRepository(private val plugin: FrPass) {

    val folder: File
        get() = File(plugin.dataFolder, "core/quests").also {
            if (!it.exists()) it.mkdirs()
        }

    fun listQuestFiles(): List<File> {
        return folder.listFiles()?.filter { it.extension.equals("yml", ignoreCase = true) } ?: emptyList()
    }

    fun findQuestFile(questId: String?): File? {
        if (questId.isNullOrBlank()) return null
        for (file in listQuestFiles()) {
            val config = YamlConfiguration.loadConfiguration(file)
            if (config.contains("quests.$questId")) {
                return file
            }
        }
        return null
    }

    fun resolveSafeFile(fileName: String): File {
        val baseDirPath = folder.toPath().toAbsolutePath().normalize()
        val targetFile = File(folder, fileName)
        val targetPath = targetFile.toPath().toAbsolutePath().normalize()
        if (!targetPath.startsWith(baseDirPath)) {
            throw SecurityException("Security violation: path traversal detected (${targetFile.path})")
        }
        return targetFile
    }

    fun saveConfig(file: File, config: YamlConfiguration) {
        if (!file.exists()) file.createNewFile()
        plugin.frSync.suppress()
        config.save(file)
    }
}
