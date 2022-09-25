package love.chihuyu.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandPermission
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import love.chihuyu.FishBattleManager
import love.chihuyu.FishData
import love.chihuyu.FishingBattle.Companion.gameTask
import love.chihuyu.FishingBattle.Companion.isStarted
import love.chihuyu.FishingBattle.Companion.owner
import love.chihuyu.FishingBattle.Companion.plugin
import love.chihuyu.utils.runTaskTimer
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
                .executesPlayer(
                    PlayerCommandExecutor { sender, args ->
                        if (isStarted) {
                            sender.sendMessage("${ChatColor.RED}Game is already started.")
                            return@PlayerCommandExecutor
                        }

                        var remainCountdown = 6

                        plugin.runTaskTimer(0, 20) countdown@{
                            remainCountdown--

                            if (remainCountdown == 0) {
                                val endEpoch = nowEpoch() + 180
                                isStarted = true

                                plugin.server.onlinePlayers.forEach {
                                    it.playSound(it, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                                    FishData.data[it] = 0
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

                                    bossBar.progress = (1.0 / 180.0) * (endEpoch - nowEpoch())
                                    bossBar.isVisible = true

                                    plugin.server.onlinePlayers.forEach {
                                        bossBar.addPlayer(it)
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

                            plugin.server.onlinePlayers.forEach {
                                it.playSound(it, Sound.UI_BUTTON_CLICK, 1f, 1f)
                                it.sendTitle(
                                    "${ChatColor.LIGHT_PURPLE}${ChatColor.BOLD}${ChatColor.ITALIC}$remainCountdown",
                                    "Fishing Battle",
                                    0,
                                    20,
                                    0
                                )
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
                )
        )

    private fun formatTime(timeSeconds: Long): String {
        return "${"%02d".format(timeSeconds.floorDiv(60))}:" + "%02d".format(timeSeconds % 60)
    }

    private fun nowEpoch(): Long {
        return Instant.now().epochSecond
    }
}
