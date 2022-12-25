package love.chihuyu.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandPermission
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import love.chihuyu.FishBattleManager
import love.chihuyu.FishData
import love.chihuyu.FishingBattle.Companion.gameTask
import love.chihuyu.FishingBattle.Companion.isStarted
import love.chihuyu.FishingBattle.Companion.owner
import love.chihuyu.FishingBattle.Companion.plugin
import love.chihuyu.utils.runTaskTimer
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import java.time.Instant

object CommandFishBattle {

    val main: CommandAPICommand = CommandAPICommand("fishbattle")
        .withPermission("fishbattle.fishbattle")
        .withPermission(CommandPermission.NONE)
        .withAliases("fb")
        .withSubcommands(
            CommandAPICommand("start")
                .withPermission("fishbattle.start")
                .withPermission(CommandPermission.NONE)
                .withArguments(IntegerArgument("waitingSeconds"), IntegerArgument("gameSeconds"))
                .executesPlayer(
                    PlayerCommandExecutor { sender, args ->
                        if (isStarted) {
                            sender.sendMessage("${ChatColor.RED}Game is already started.")
                            return@PlayerCommandExecutor
                        }

                        plugin.server.onlinePlayers.forEach {
                            it.spigot().sendMessage(
                                TextComponent("${sender.name} started new game. ${ChatColor.GREEN}${ChatColor.BOLD}[Click to join]").apply {
                                    this.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fb join")
                                    this.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("Click to join game"))
                                }
                            )
                        }

                        var remainCountdown = args[0] as Int
                        val gameTime = args[1] as Int

                        plugin.runTaskTimer(0, 20) countdown@{
                            remainCountdown--

                            if (remainCountdown == 0) {
                                val endEpoch = nowEpoch() + gameTime
                                isStarted = true

                                FishData.data.keys.forEach {
                                    val player = Bukkit.getPlayer(it.uniqueId) ?: return@forEach
                                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                                }

                                owner = sender
                                gameTask = plugin.runTaskTimer(0, 20) game@{
                                    val bossbarKey = NamespacedKey(plugin, "fish_battle")
                                    plugin.server.getBossBar(bossbarKey)?.removeAll()
                                    plugin.server.removeBossBar(bossbarKey)

                                    val bossBar = Bukkit.createBossBar(
                                        bossbarKey,
                                        "Time Remain: ${formatTime(endEpoch - nowEpoch())} | Owner: ${owner?.name}",
                                        BarColor.GREEN,
                                        BarStyle.SEGMENTED_6
                                    )

                                    bossBar.progress = (1.0 / gameTime) * (endEpoch - nowEpoch())
                                    bossBar.isVisible = true

                                    FishData.data.keys.forEach {
                                        val player = Bukkit.getPlayer(it.uniqueId) ?: return@forEach
                                        bossBar.addPlayer(player)
                                    }

                                    FishBattleManager.updateScoreboard()

                                    if (nowEpoch() == endEpoch) {
                                        FishBattleManager.broadcastRanking()

                                        plugin.server.getBossBar(bossbarKey)?.removeAll()
                                        plugin.server.removeBossBar(bossbarKey)

                                        FishBattleManager.saveDatabase()
                                        FishData.data.clear()

                                        isStarted = false
                                        FishBattleManager.updateScoreboard()

                                        cancel()
                                        return@game
                                    }
                                }

                                cancel()
                                return@countdown
                            }

                            if (remainCountdown <= 5) {
                                FishData.data.keys.forEach {
                                    val player = Bukkit.getPlayer(it.uniqueId) ?: return@forEach
                                    player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f)
                                    player.sendTitle(
                                        "${ChatColor.LIGHT_PURPLE}${ChatColor.BOLD}${ChatColor.ITALIC}$remainCountdown",
                                        "Fishing Battle",
                                        0,
                                        20,
                                        0
                                    )
                                }
                            }
                        }
                    }
                ),
            CommandAPICommand("stop")
                .withPermission("fishbattle.stop")
                .withPermission(CommandPermission.NONE)
                .executesPlayer(
                    PlayerCommandExecutor { sender, args ->
                        if (!isStarted) {
                            sender.sendMessage("${ChatColor.RED}Game is not running.")
                            return@PlayerCommandExecutor
                        } else if (owner != sender) {
                            sender.sendMessage("${ChatColor.RED}You are not game owner.")
                            return@PlayerCommandExecutor
                        }

                        gameTask?.cancel()

                        FishBattleManager.broadcastRanking()

                        val bossbarKey = NamespacedKey(plugin, "fish_battle")
                        plugin.server.getBossBar(bossbarKey)?.removeAll()
                        plugin.server.removeBossBar(bossbarKey)

                        FishData.data.clear()

                        isStarted = false
                        FishBattleManager.updateScoreboard()
                    }
                ),
            CommandAPICommand("join")
                .withPermission("fishbattle.join")
                .withPermission(CommandPermission.NONE)
                .executesPlayer(PlayerCommandExecutor { sender, args ->
                    FishData.data[sender] = 0
                    sender.sendMessage("${ChatColor.GREEN}You joined game.")
                }),
            CommandAPICommand("leave")
                .withPermission("fishbattle.leave")
                .withPermission(CommandPermission.NONE)
                .executesPlayer(PlayerCommandExecutor { sender, args ->
                    FishData.data.remove(sender)
                    sender.sendMessage("${ChatColor.RED}You left game.")
                })
        )

    private fun formatTime(timeSeconds: Long): String {
        return "${"%02d".format(timeSeconds.floorDiv(3600))}:${"%02d".format(timeSeconds.floorDiv(60) % 60)}:${"%02d".format(timeSeconds % 60)}"
    }

    private fun nowEpoch(): Long {
        return Instant.now().epochSecond
    }
}