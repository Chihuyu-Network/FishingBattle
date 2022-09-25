package love.chihuyu

import love.chihuyu.commands.CommandFBStats
import love.chihuyu.commands.CommandFishBattle
import love.chihuyu.database.User
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

class FishingBattle : JavaPlugin(), Listener {

    companion object {
        lateinit var plugin: JavaPlugin
        var gameTask: BukkitTask? = null
        var owner: OfflinePlayer? = null
        var isStarted = false
    }

    init {
        plugin = this
    }

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)

        val db = File("${plugin.dataFolder}/fishing_stats.db")
        if (!db.exists()) {
            File("${plugin.dataFolder}").mkdir()
            db.createNewFile()
        }
        Database.connect("jdbc:sqlite:${plugin.dataFolder}/fishing_stats.db", "org.sqlite.JDBC")

        transaction {
            SchemaUtils.createMissingTablesAndColumns(User, withLogs = true)
        }

        CommandFishBattle.main.register()
        CommandFBStats.main.register()
    }

    @EventHandler
    fun onFish(event: PlayerFishEvent) {
        val result = event.caught as? Item
        val player = event.player

        if (!isStarted || result == null || !FishData.data.containsKey(player)) return

        if (player.inventory.itemInMainHand.enchantments.isNotEmpty()) {
            player.sendMessage("${ChatColor.RED}Please use unenchanted fishing rod!")
        }

        val point = when (result.itemStack.type) {
            Material.COD -> 1
            Material.SALMON -> 2
            Material.TROPICAL_FISH -> 10
            Material.PUFFERFISH -> 5

            Material.BOW -> 10
            Material.ENCHANTED_BOOK -> 10
            Material.FISHING_ROD -> {
                if (result.itemStack.enchantments.isNotEmpty()) {
                    20
                } else {
                    10
                }
            }
            Material.NAME_TAG -> 8
            Material.NAUTILUS_SHELL -> 8
            Material.SADDLE -> 8

            Material.LILY_PAD -> 8
            Material.BAMBOO -> 30
            Material.COCOA_BEANS -> 30
            Material.BOWL -> 10
            Material.LEATHER -> 10
            Material.LEATHER_BOOTS -> 10
            Material.ROTTEN_FLESH -> 10
            Material.STICK -> 15
            Material.STRING -> 15
            Material.POTION -> 15
            Material.BONE -> 15
            Material.INK_SAC -> 50
            Material.TRIPWIRE_HOOK -> 15

            else -> 0
        }

        FishData.data[player] = (FishData.data[player] ?: 0) + point
        player.sendTitle("", "${ChatColor.GRAY}${ChatColor.ITALIC}Fish! ${ChatColor.WHITE}+$point" + " ".repeat(16), 0, 25, 5)
        FishBattleManager.updateScoreboard()
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        transaction {
            val user = User.select { User.uuid eq player.uniqueId }
            if (user.count() == 0L) {
                User.insert {
                    it[uuid] = player.uniqueId
                    it[highscore] = 0
                    it[totalscore] = 0
                    it[totalplays] = 0
                }
            }
        }
        if (!isStarted) return
        FishBattleManager.updateScoreboard()
        server.bossBars.forEach { it.removePlayer(player) }
    }
}