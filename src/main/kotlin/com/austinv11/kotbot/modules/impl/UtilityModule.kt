package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.KotBot
import com.austinv11.kotbot.context
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.Command
import com.austinv11.kotbot.modules.api.commands.Description
import com.austinv11.kotbot.modules.api.commands.Executor
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

class UtilityModule : KotBotModule() {

    override fun initialize() {

    }

    class PingCommand: Command("This provides information about the response time for this bot.") {

        @Executor
        fun execute(): String {
            return buildString {
                appendln("Pong!")
                appendln("Received this message in: `${System.currentTimeMillis()-context.message.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}`ms")
                appendln("Last websocket ping response time: `${KotBot.CLIENT.responseTime}`ms")
            }
        }
    }

    class PrefixCommand: Command("This changes the bot's command prefix.", ownerOnly = true) {

        @Executor
        fun execute(@Description("prefix", "This is the new prefix to use") prefix: String): String {
            KotBot.CONFIG.COMMAND_PREFIX = prefix

            val writer = KotBot.CONFIG_FILE.writer()
            KotBot.GSON.toJson(KotBot.CONFIG, writer)
            writer.close()

            return "Command prefix changed to $prefix"
        }
    }

    class AlarmCommand: Command("This allows you to set a reminder.", aliases = arrayOf("timer", "reminder")) {

        companion object {
            const val VALID_UNITS: String = "nanoseconds, microseconds, milliseconds, seconds, minutes, hours, days"
            val TIMER: Timer = Timer("Alarm Timer", true)
        }

        @Executor
        fun execute(@Description("time", "This is the time to wait for the reminder.") time: Long): String {
            return execute(time, TimeUnit.MILLISECONDS.name)
        }

        @Executor
        fun execute(@Description("time", "This is the time to wait for the reminder.") time: Long,
                    @Description("unit", "The time unit, valid units are: $VALID_UNITS") unit: String): String {
            return execute(time, unit, null)
        }

        @Executor
        fun execute(@Description("time", "This is the time to wait for the reminder.") time: Long,
                    @Description("unit", "The time unit, valid units are: $VALID_UNITS") unit: String,
                    @Description("description", "The message to be sent with the reminder") description: String?): String {

            var unitString = unit.toUpperCase()
            if (!unitString.endsWith("S"))
                unitString += "S"

            val timeUnit: TimeUnit = TimeUnit.valueOf(unitString)

            val channel = context.channel
            val author = context.user
            TIMER.schedule(timerTask {
                channel.sendMessage(buildString {
                    append("Times up $author!")

                    if (description != null)
                        append(" This is a reminder for `$description`")

                    cancel()
                })
            }, timeUnit.toMillis(time))

            return "Successfully scheduled a reminder for $time ${timeUnit.toString().toLowerCase()} from now."
        }
    }
}
