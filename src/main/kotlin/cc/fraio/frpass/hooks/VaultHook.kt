package cc.fraio.frpass.hooks

import org.bukkit.Bukkit
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.RegisteredServiceProvider

object VaultHook {
    var economy: Economy? = null
        private set

    fun setupEconomy(): Boolean {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false
        }
        val rsp: RegisteredServiceProvider<Economy>? = Bukkit.getServicesManager().getRegistration(Economy::class.java)
        if (rsp == null) {
            return false
        }
        economy = rsp.provider
        return economy != null
    }
}
