package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException

class DatabaseManager(private val plugin: FrPass) {

    private var dataSource: HikariDataSource? = null
    private var databaseType: String = "SQLITE"

    fun connect() {
        databaseType = plugin.config.getString("database.type", "SQLITE")?.uppercase() ?: "SQLITE"
        
        try {
            val hikariConfig = HikariConfig()
            
            when (databaseType) {
                "SQLITE" -> {
                    val fileName = plugin.config.getString("database.file", "database.db") ?: "database.db"
                    val file = File(plugin.dataFolder, fileName)
                    hikariConfig.driverClassName = "org.sqlite.JDBC"
                    hikariConfig.jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
                    hikariConfig.maximumPoolSize = 1
                    hikariConfig.connectionTestQuery = "SELECT 1"
                }
                "MYSQL" -> {
                    val host = plugin.config.getString("database.host", "localhost")
                    val port = plugin.config.getInt("database.port", 3306)
                    val database = plugin.config.getString("database.database", "frpass")
                    val useSSL = plugin.config.getBoolean("database.useSSL", false)
                    
                    hikariConfig.jdbcUrl = "jdbc:mysql://$host:$port/$database?useSSL=$useSSL&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8"
                    hikariConfig.username = plugin.config.getString("database.username", "root")
                    hikariConfig.password = plugin.config.getString("database.password", "")
                    hikariConfig.driverClassName = "com.mysql.cj.jdbc.Driver"
                    configurePool(hikariConfig)
                }
                "MARIADB" -> {
                    val host = plugin.config.getString("database.host", "localhost")
                    val port = plugin.config.getInt("database.port", 3306)
                    val database = plugin.config.getString("database.database", "frpass")
                    val useSSL = plugin.config.getBoolean("database.useSSL", false)
                    
                    hikariConfig.jdbcUrl = "jdbc:mariadb://$host:$port/$database?useSSL=$useSSL"
                    hikariConfig.username = plugin.config.getString("database.username", "root")
                    hikariConfig.password = plugin.config.getString("database.password", "")
                    hikariConfig.driverClassName = "org.mariadb.jdbc.Driver"
                    configurePool(hikariConfig)
                }
                "POSTGRESQL", "POSTGRES" -> {
                    val host = plugin.config.getString("database.host", "localhost")
                    val port = plugin.config.getInt("database.port", 5432)
                    val database = plugin.config.getString("database.database", "frpass")
                    
                    hikariConfig.jdbcUrl = "jdbc:postgresql://$host:$port/$database"
                    hikariConfig.username = plugin.config.getString("database.username", "root")
                    hikariConfig.password = plugin.config.getString("database.password", "")
                    hikariConfig.driverClassName = "org.postgresql.Driver"
                    configurePool(hikariConfig)
                }
                else -> {
                    plugin.logger.warning("Unknown database type '$databaseType'. Defaulting to SQLITE.")
                    databaseType = "SQLITE"
                    val fileName = plugin.config.getString("database.file", "database.db") ?: "database.db"
                    val file = File(plugin.dataFolder, fileName)
                    hikariConfig.driverClassName = "org.sqlite.JDBC"
                    hikariConfig.jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
                    hikariConfig.maximumPoolSize = 1
                }
            }

            hikariConfig.poolName = "FrPass-Pool"
            dataSource = HikariDataSource(hikariConfig)
            plugin.logger.info("Successfully connected to $databaseType database using HikariCP!")
            createTables()
        } catch (e: Exception) {
            plugin.logger.severe("Could not connect to $databaseType database: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun configurePool(config: HikariConfig) {
        config.maximumPoolSize = plugin.config.getInt("database.maximumPoolSize", 10)
        config.minimumIdle = plugin.config.getInt("database.minimumIdle", 2)
        config.connectionTimeout = plugin.config.getLong("database.connectionTimeout", 30000L)
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    }

    private fun createTables() {
        val query = if (databaseType == "SQLITE") {
            """
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    xp INT DEFAULT 0,
                    level INT DEFAULT 1,
                    premium BOOLEAN DEFAULT 0,
                    claimed_tiers TEXT,
                    claimed_premium_tiers TEXT,
                    quests_progress TEXT,
                    quest_cycle INT DEFAULT 0,
                    active_quests TEXT,
                    pending_tickets INT DEFAULT 0
                );
            """.trimIndent()
        } else {
            """
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    xp INT DEFAULT 0,
                    level INT DEFAULT 1,
                    premium BOOLEAN DEFAULT FALSE,
                    claimed_tiers TEXT,
                    claimed_premium_tiers TEXT,
                    quests_progress TEXT,
                    quest_cycle INT DEFAULT 0,
                    active_quests TEXT,
                    pending_tickets INT DEFAULT 0
                );
            """.trimIndent()
        }
        
        try {
            getConnection()?.use { conn ->
                conn.createStatement().use { statement ->
                    statement.execute(query)
                    
                    // Add missing columns safely for legacy databases
                    val columns = listOf(
                        "pending_tickets" to "INT DEFAULT 0",
                        "claimed_tiers" to "TEXT",
                        "claimed_premium_tiers" to "TEXT",
                        "quests_progress" to "TEXT",
                        "quest_cycle" to "INT DEFAULT 0",
                        "active_quests" to "TEXT"
                    )
                    
                    for ((column, def) in columns) {
                        try {
                            statement.execute("ALTER TABLE player_data ADD COLUMN $column $def")
                        } catch (ignored: SQLException) {}
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to create tables: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            if (dataSource != null && !dataSource!!.isClosed) {
                dataSource?.close()
                plugin.logger.info("Database connection pool closed successfully.")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to close database pool: ${e.message}")
        }
    }

    fun getConnection(): Connection? {
        return try {
            dataSource?.connection
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to get connection from pool: ${e.message}")
            null
        }
    }

    fun isSQLite(): Boolean = databaseType == "SQLITE"
}
