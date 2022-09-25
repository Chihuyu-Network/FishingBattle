package love.chihuyu.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandPermission
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.OfflinePlayerArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import love.chihuyu.FishingBattle.Companion.plugin
import love.chihuyu.database.User
import love.chihuyu.database.User.highscore
import love.chihuyu.database.User.totalplays
import love.chihuyu.database.User.totalscore
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object CommandFBStats {

    val main = CommandAPICommand("fishingbattlestats")
        .withPermission("fishbattle.stats")
        .withPermission(CommandPermission.NONE)
        .withAliases("fbstats")
        .withSubcommands(
            CommandAPICommand("highscore")
                .withPermission("fishbattle.stats.highscore")
                .withPermission(CommandPermission.NONE)
                .withArguments(
                    OfflinePlayerArgument("player").replaceSafeSuggestions {
                        ArgumentSuggestions.strings { plugin.server.offlinePlayers.map { it.name }.toTypedArray() }
                    }
                )
                .executesPlayer(
                    PlayerCommandExecutor { sender, args ->
                        transaction {
                            val player = args[0] as OfflinePlayer
                            val user = User.select { User.uuid eq player.uniqueId }
                            if (user.count() == 0L) {
                                sender.sendMessage("${ChatColor.RED}User data not found.")
                                return@transaction
                            }
                            sender.sendMessage("${ChatColor.LIGHT_PURPLE}${player.name}'s highscore is ${user.single()[highscore]}pt.")
                        }
                    }
                ),
            CommandAPICommand("totalscore")
                .withPermission("fishbattle.stats.totalscore")
                .withPermission(CommandPermission.NONE)
                .withArguments(
                    OfflinePlayerArgument("player").replaceSafeSuggestions {
                        ArgumentSuggestions.strings { plugin.server.offlinePlayers.map { it.name }.toTypedArray() }
                    }
                )
                .executesPlayer(
                    PlayerCommandExecutor { sender, args ->
                        transaction {
                            val player = args[0] as OfflinePlayer
                            val user = User.select { User.uuid eq player.uniqueId }
                            if (user.count() == 0L) {
                                sender.sendMessage("${ChatColor.RED}User data not found.")
                                return@transaction
                            }
                            sender.sendMessage("${ChatColor.LIGHT_PURPLE}${player.name}'s totalscore is ${user.single()[totalscore]}pt.")
                        }
                    }
                ),
            CommandAPICommand("totalplays")
                .withPermission("fishbattle.stats.totalplays")
                .withPermission(CommandPermission.NONE)
                .withArguments(
                    OfflinePlayerArgument("player").replaceSafeSuggestions {
                        ArgumentSuggestions.strings { plugin.server.offlinePlayers.map { it.name }.toTypedArray() }
                    }
                )
                .executesPlayer(
                    PlayerCommandExecutor { sender, args ->
                        transaction {
                            val player = args[0] as OfflinePlayer
                            val user = User.select { User.uuid eq player.uniqueId }
                            if (user.count() == 0L) {
                                sender.sendMessage("${ChatColor.RED}User data not found.")
                                return@transaction
                            }
                            sender.sendMessage("${ChatColor.LIGHT_PURPLE}${player.name}'s totalplays is ${user.single()[totalplays]}plays.")
                        }
                    }
                ),
            CommandAPICommand("ranking")
                .withPermission("fishbattle.stats.ranking")
                .withPermission(CommandPermission.NONE)
                .withSubcommands(
                    CommandAPICommand("totalscore")
                        .withPermission("fishbattle.stats.ranking.totalscore")
                        .withPermission(CommandPermission.NONE)
                        .executesPlayer(
                            PlayerCommandExecutor { sender, args ->
                                val top5 = mutableListOf(
                                    "\n" +
                                        "${ChatColor.GOLD}${ChatColor.STRIKETHROUGH}${" ".repeat(8)}" +
                                        "${ChatColor.GOLD}${ChatColor.BOLD}Totalscore Ranking" +
                                        "${ChatColor.GOLD}${ChatColor.STRIKETHROUGH}${" ".repeat(8)}" +
                                        "${ChatColor.RESET}"
                                )
                                transaction {
                                    val list = mutableMapOf<OfflinePlayer, Int>()
                                    plugin.server.offlinePlayers.forEach {
                                        val user = User.select { User.uuid eq it.uniqueId }
                                        if (user.count() == 0L) return@forEach
                                        list[it] = user.single()[totalscore]
                                    }
                                    list.toList().sortedByDescending { it.second }.forEachIndexed { index, pair ->
                                        if (index > 4) return@forEachIndexed
                                        top5.add("${index.inc()}. ${pair.first.name} ${pair.second}pt")
                                    }
                                }
                                sender.sendMessage(top5.joinToString("\n"))
                            }
                        ),
                    CommandAPICommand("highscore")
                        .withPermission("fishbattle.stats.ranking.highscore")
                        .withPermission(CommandPermission.NONE)
                        .executesPlayer(
                            PlayerCommandExecutor { sender, args ->
                                val top5 = mutableListOf(
                                    "\n" +
                                        "${ChatColor.GOLD}${ChatColor.STRIKETHROUGH}${" ".repeat(8)}" +
                                        "${ChatColor.GOLD}${ChatColor.BOLD}Highscore Ranking" +
                                        "${ChatColor.GOLD}${ChatColor.STRIKETHROUGH}${" ".repeat(8)}" +
                                        "${ChatColor.RESET}"
                                )
                                transaction {
                                    val list = mutableMapOf<OfflinePlayer, Int>()
                                    plugin.server.offlinePlayers.forEach {
                                        val user = User.select { User.uuid eq it.uniqueId }
                                        if (user.count() == 0L) return@forEach
                                        list[it] = user.single()[highscore]
                                    }
                                    list.toList().sortedByDescending { it.second }.forEachIndexed { index, pair ->
                                        if (index > 4) return@forEachIndexed
                                        top5.add("${index.inc()}. ${pair.first.name} ${pair.second}pt")
                                    }
                                }
                                sender.sendMessage(top5.joinToString("\n"))
                            }
                        )
                )
        )
}
