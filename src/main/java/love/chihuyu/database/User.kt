package love.chihuyu.database

import org.jetbrains.exposed.sql.Table

object User : Table() {
    val uuid = uuid("uuid")
    val highscore = integer("highscore")
    val totalscore = integer("totalscore")
    val totalplays = integer("totalplays")

    override val primaryKey: PrimaryKey = PrimaryKey(arrayOf(uuid), "UUID")
}
