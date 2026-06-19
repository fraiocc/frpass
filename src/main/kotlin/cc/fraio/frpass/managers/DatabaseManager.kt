package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseManager(private val plugin: FrPass) {

    private var connection: Connection? = null

    fun connect() {
        val type = plugin.config.getString("database.type", "SQLITE")?.uppercase()
        if (type == "SQLITE") {
            val fileName = plugin.config.getString("database.file", "database.db")
            val file = File(plugin.dataFolder, fileName!!)
            try {
                Class.forName("org.sqlite.JDBC")
                connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
                plugin.logger.info("Connected to SQLite database successfully!")
                createTables()
            } catch (e: Exception) {
                plugin.logger.severe("Could not connect to SQLite database: ${e.message}")
            }
        }
    }

    private fun createTables() {
        val query = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid VARCHAR(36) PRIMARY KEY,
                xp INT DEFAULT 0,
                level INT DEFAULT 1,
                premium BOOLEAN DEFAULT 0,
                claimed_tiers TEXT DEFAULT '',
                claimed_premium_tiers TEXT DEFAULT '',
                quests_progress TEXT DEFAULT '',
                quest_cycle INT DEFAULT 0,
                active_quests TEXT DEFAULT '',
                pending_tickets INT DEFAULT 0
            );
        """.trimIndent()
        
        try {
            connection?.createStatement()?.use { statement ->
                statement.execute(query)
                // Add column for existing databases safely
                try {
                    statement.execute("ALTER TABLE player_data ADD COLUMN pending_tickets INT DEFAULT 0;")
                } catch (ignored: Exception) {}
                // If table already exists, try adding the columns safely
                try {
                    statement.execute("ALTER TABLE player_data ADD COLUMN claimed_tiers TEXT DEFAULT ''")
                } catch (ignored: SQLException) {}
                try {
                    statement.execute("ALTER TABLE player_data ADD COLUMN claimed_premium_tiers TEXT DEFAULT ''")
                } catch (ignored: SQLException) {}
                try {
                    statement.execute("ALTER TABLE player_data ADD COLUMN quests_progress TEXT DEFAULT ''")
                } catch (ignored: SQLException) {}
                try {
                    statement.execute("ALTER TABLE player_data ADD COLUMN quest_cycle INT DEFAULT 0")
                } catch (ignored: SQLException) {}
                try {
                    statement.execute("ALTER TABLE player_data ADD COLUMN active_quests TEXT DEFAULT ''")
                } catch (ignored: SQLException) {}
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to create tables: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            if (connection != null && !connection!!.isClosed) {
                connection?.close()
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to close database connection: ${e.message}")
        }
    }

    fun getConnection(): Connection? {
        return connection
    }
}
