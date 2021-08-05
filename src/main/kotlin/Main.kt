import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.`object`.presence.Status
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class Config(val token: String, val guild: String, val member: String, val target: String, val format: String)

fun loadProps(where: File): Config {
    val props = where.bufferedReader().let { Properties().apply { load(it) } }
    return Config(
        props["token"]!!.toString(),
        props["guild"]!!.toString(),
        props["member"]!!.toString(),
        props["target"]!!.toString(),
        props["format"]!!.toString(),
    )
}

fun format(text: String, data: MutableMap<String, Any?>): String {
    return text.replace(Regex("\\{[^}]+}")) { data[it.value.drop(1).dropLast(1)]?.toString() ?: "" }
}

fun main() {
    val cfg = loadProps(File("config.properties"))
    val bot = DiscordClient.create(cfg.token)
    val gateway = bot.login().block()!!//.apply { updatePresence(Presence.invisible()).block() }
    var latest = Status.OFFLINE
    val df = SimpleDateFormat("H:mm")
    var err = 0

    gateway.getGuildById(Snowflake.of(cfg.guild)).block()?.run {
        println("get guild '$name' successful")

        println("waiting...")
        while (true) {
            try {
                val member =
                    getMemberById(Snowflake.of(cfg.member)).block() ?: throw RuntimeException("member not found")
                val status = member?.run { presence.block()?.status }
                if (latest != status && status != Status.OFFLINE) {
                    (getChannelById(Snowflake.of(cfg.target)).block() as TextChannel).run {
                        createMessage(
                            format(
                                cfg.format,
                                mutableMapOf(
                                    "time" to df.format(Date()),
                                    "name" to member.displayName,
                                )
                            )
                        ).block()
                    }
                    println("online now")
                } else if (latest != status) println("offline")

                if (status != null) latest = status
                Thread.sleep(1000)
            } catch (t: Throwable) {
                if (err++ > 5) throw t
            }
        }
    }
    println("guild not found?")
}
