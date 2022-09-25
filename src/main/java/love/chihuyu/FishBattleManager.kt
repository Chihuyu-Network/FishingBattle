package love.chihuyu

import love.chihuyu.FishingBattle.Companion.plugin
import love.chihuyu.database.User
import org.bukkit.ChatColor
import org.bukkit.Sound
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object FishBattleManager {

    fun updateScoreboard() {
        plugin.server.onlinePlayers.forEach {
            val board = it.scoreboard
            board.getObjective(DisplaySlot.SIDEBAR)?.unregister()
            val obj = board.registerNewObjective("fish_battle", Criteria.create("fished"), "    ${ChatColor.GOLD}${ChatColor.BOLD}${ChatColor.ITALIC}Fish Battle${ChatColor.RESET}    ")

            if (!FishingBattle.isStarted) {
                obj.unregister()
                return@forEach
            }

            val scores = mutableListOf(
                " ".repeat(1),
                " ".repeat(2),
                "You: ${FishData.data[it]}pt",
                " ".repeat(3)
            )

            FishData.data.toList().sortedByDescending { score -> score.second }.forEachIndexed { index, pair ->
                scores.add(index.inc(), "${index.inc()}. ${pair.first.name} ${pair.second}pt")
            }

            scores.forEachIndexed { index, s -> obj.getScore(s).score = -index }

            obj.displaySlot = DisplaySlot.SIDEBAR
        }
    }

    fun saveDatabase() {
        FishData.data.forEach { (player, score) ->
            transaction {
                val user = User.select { User.uuid eq player.uniqueId }
                if (user.count() > 0L) {
                    User.update({ User.uuid eq player.uniqueId }) {
                        it[highscore] = score.coerceAtLeast(user.single()[highscore])
                        it[totalscore] = user.single()[totalscore] + score
                        it[totalplays] = user.single()[totalplays].inc()
                    }
                } else {
                    User.insert {
                        it[uuid] = player.uniqueId
                        it[highscore] = score
                        it[totalscore] = score
                        it[totalplays] = 1
                    }
                }
            }
        }
    }

    fun broadcastRanking() {
        val sortedList = FishData.data.toList().sortedByDescending { it.second }
        plugin.server.onlinePlayers.forEach {
            it.playSound(it, Sound.ENTITY_ENDER_DRAGON_DEATH, 1f, 1f)
            it.sendTitle(
                "${ChatColor.RED}${ChatColor.BOLD}${ChatColor.ITALIC}Game Over！",
                "Winner is ${ChatColor.BOLD}${sortedList[0].first.name}",
                20, 100, 20
            )

            transaction {
                val user = User.select { User.uuid eq it.uniqueId }.single()
                if (user[User.highscore] < (FishData.data[it] ?: 0)) it.sendMessage("${ChatColor.LIGHT_PURPLE}You beated high score!\n${user[User.highscore]}pt -> ${FishData.data[it]}pt")
            }
        }

        val messages = mutableListOf(
            "\n" +
                "${ChatColor.GOLD}${ChatColor.STRIKETHROUGH}${" ".repeat(8)}" +
                "${ChatColor.GOLD}${ChatColor.BOLD}Score Ranking" +
                "${ChatColor.GOLD}${ChatColor.STRIKETHROUGH}${" ".repeat(8)}" +
                "${ChatColor.RESET}"
        )

        sortedList.forEachIndexed { index, pair ->
            if (index > 4) return@forEachIndexed
            messages.add("${index.inc()}. ${pair.first.name} ${pair.second}pt")
        }

        messages.add("\n")
        plugin.server.broadcastMessage(messages.joinToString("\n"))
    }
}
