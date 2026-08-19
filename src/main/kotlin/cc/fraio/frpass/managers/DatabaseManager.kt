package cc.fraio.frpass.managers

import cc.fraio.frpass.FrPass
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException

class DatabaseManager(private val plugin: FrPass) {

    private var dataSource: HikariDataSource? = null
    var databaseType = "SQLITE"
        private set

    fun connect() {
        databaseType = plugin.config.getString("database.type", "SQLITE")!!.uppercase()
        val config = HikariConfig()
        
        when (databaseType) {
            "MYSQL" -> {
                val host = plugin.config.getString("database.host", "localhost")
                val port = plugin.config.getInt("database.port", 3306)
                val db = plugin.config.getString("database.database", "frpass")
                
                config.driverClassName = "com.mysql.cj.jdbc.Driver"
                config.jdbcUrl = "jdbc:mysql://$host:$port/$db"
                config.username = plugin.config.getString("database.username", "root")
                config.password = plugin.config.getString("database.password", "")
                
                config.addDataSourceProperty("useSSL", plugin.config.getBoolean("database.useSSL", false))
                config.addDataSourceProperty("autoReconnect", "true")
                config.addDataSourceProperty("useUnicode", "true")
                config.addDataSourceProperty("characterEncoding", "UTF-8")
                
                setupPoolDefaults(config)
            }
            "MARIADB" -> {
                val host = plugin.config.getString("database.host", "localhost")
                val port = plugin.config.getInt("database.port", 3306)
                val db = plugin.config.getString("database.database", "frpass")
                
                config.driverClassName = "org.mariadb.jdbc.Driver"
                config.jdbcUrl = "jdbc:mariadb://$host:$port/$db"
                config.username = plugin.config.getString("database.username", "root")
                config.password = plugin.config.getString("database.password", "")
                
                config.addDataSourceProperty("useSSL", plugin.config.getBoolean("database.useSSL", false))
                
                setupPoolDefaults(config)
            }
            "POSTGRESQL", "POSTGRES" -> {
                val host = plugin.config.getString("database.host", "localhost")
                val port = plugin.config.getInt("database.port", 5432)
                val db = plugin.config.getString("database.database", "frpass")
                
                config.driverClassName = "org.postgresql.Driver"
                config.jdbcUrl = "jdbc:postgresql://$host:$port/$db"
                config.username = plugin.config.getString("database.username", "root")
                config.password = plugin.config.getString("database.password", "")
                
                setupPoolDefaults(config)
            }
            else -> {
                // Default to SQLite
                val file = File(plugin.dataFolder, plugin.config.getString("database.file", "database.db")!!)
                config.driverClassName = "org.sqlite.JDBC"
                config.jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
                config.maximumPoolSize = 1
                config.connectionTestQuery = "SELECT 1"
            }
        }

        config.poolName = "FrPass-Hikari"
        
        try {
            dataSource = HikariDataSource(config)
            plugin.logger.info("Connected to $databaseType successfully!")
            createTables()
        } catch (e: Exception) {
            plugin.logger.severe("Database connection failed ($databaseType): " + e.message)
            e.printStackTrace()
        }
    }

    private fun setupPoolDefaults(config: HikariConfig) {
        config.maximumPoolSize = plugin.config.getInt("database.maximumPoolSize", 10)
        config.minimumIdle = plugin.config.getInt("database.minimumIdle", 2)
        config.connectionTimeout = plugin.config.getLong("database.connectionTimeout", 30000L)
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    }

    private fun createTables() {
        val isSqlite = databaseType == "SQLITE"
        val boolType = if (isSqlite) "0" else "FALSE"
        
        val sql = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid VARCHAR(36) PRIMARY KEY,
                xp INT DEFAULT 0,
                level INT DEFAULT 1,
                premium BOOLEAN DEFAULT $boolType,
                claimed_tiers TEXT,
                claimed_premium_tiers TEXT,
                quests_progress TEXT,
                quest_cycle INT DEFAULT 0,
                active_quests TEXT,
                pending_tickets INT DEFAULT 0
            );
        """.trimIndent()

        val conn = getConnection() ?: return
        try {
            val stmt = conn.createStatement()
            stmt.execute(sql)
            
            val cols = mutableSetOf<String>()
            val meta = conn.metaData
            val rs = meta.getColumns(null, null, "player_data", null)
            while (rs.next()) {
                cols.add(rs.getString("COLUMN_NAME").lowercase())
            }
            rs.close()

            if (cols.isEmpty()) {
                val rs2 = meta.getColumns(null, null, "PLAYER_DATA", null)
                while (rs2.next()) {
                    cols.add(rs2.getString("COLUMN_NAME").lowercase())
                }
                rs2.close()
            }

            val needed = listOf(
                "pending_tickets" to "INT DEFAULT 0",
                "claimed_tiers" to "TEXT",
                "claimed_premium_tiers" to "TEXT",
                "quests_progress" to "TEXT",
                "quest_cycle" to "INT DEFAULT 0",
                "active_quests" to "TEXT"
            )

            for ((col, type) in needed) {
                if (!cols.contains(col)) {
                    try {
                        stmt.execute("ALTER TABLE player_data ADD COLUMN $col $type")
                    } catch (e: Exception) {
                    }
                }
            }

            stmt.close()
            conn.close()
        } catch (e: SQLException) {
            plugin.logger.severe("Error creating database tables: ${e.message}")
        }
    }

    fun disconnect() {
        if (dataSource != null && !dataSource!!.isClosed) {
            dataSource!!.close()
        }
    }

    fun getConnection(): Connection? {
        return try {
            dataSource?.connection
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get DB connection: ${e.message}")
            null
        }
    }

    fun isSQLite(): Boolean = databaseType == "SQLITE"
}
