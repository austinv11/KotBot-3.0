package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.KotBot
import com.austinv11.kotbot.context
import com.austinv11.kotbot.findChannelFromMessage
import com.austinv11.kotbot.findUserFromMessage
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.ApprovedUsers
import com.austinv11.kotbot.modules.api.commands.Command
import com.austinv11.kotbot.modules.api.commands.Description
import com.austinv11.kotbot.modules.api.commands.Executor
import sx.blah.discord.Discord4J
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

    class PrefixCommand: Command("This changes the bot's command prefix.", approvedUsers = ApprovedUsers.OWNER) {

        @Executor
        fun execute(@Description("prefix", "This is the new prefix to use") prefix: String): String {
            KotBot.CONFIG.COMMAND_PREFIX = prefix

            KotBot.CONFIG.update()

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
    
    class UptimeCommand: Command("This retrieves the bot's current uptime.") {
        
        @Executor
        fun execute(): String {
            return buildString { 
                append("This bot has been online for: ")
                
                val instanceDifference = System.currentTimeMillis()-Discord4J.getLaunchTime()
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                var difference = instanceDifference
                val days: Int = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS).toInt()
                difference -= TimeUnit.MILLISECONDS.convert(days.toLong(), TimeUnit.DAYS)
                val hours: Int = TimeUnit.HOURS.convert(difference, TimeUnit.MILLISECONDS).toInt()
                difference -= TimeUnit.MILLISECONDS.convert(hours.toLong(), TimeUnit.HOURS)
                val minutes: Int = TimeUnit.MINUTES.convert(difference, TimeUnit.MILLISECONDS).toInt()
                difference -= TimeUnit.MILLISECONDS.convert(minutes.toLong(), TimeUnit.MINUTES)
                val seconds = TimeUnit.SECONDS.convert(difference, TimeUnit.MILLISECONDS)
                
                append("$days days, $hours hours, $minutes minutes, and $seconds seconds")
            }
        }
    }
    
    class WhoisCommand: Command("This retrieves various information regarding a user.") {
        
        @Executor
        fun execute(): String {
            return execute(context.user.id)
        }
        
        @Executor
        fun execute(@Description("user", "The user to find information for (either Id, Username#Discrim, Username or @Mention).") arg: String): String {
            val user = findUserFromMessage(arg, context.message)
            
            if (user != null) {
                return buildString {
                    appendln("```xl")

                    val header = "Information for user ${user.name}:"
                    appendln(header)
                    appendln("=".repeat(header.length))

                    appendln("Name: ${user.getDisplayName(context.channel.guild)}#${user.discriminator}")
                    appendln("ID: ${user.id}")
                    appendln("Is a Bot?: ${user.isBot}")
                    appendln("Avatar: ${user.avatarURL}")
                    appendln("User Account Creation Date: ${user.creationDate}")
                    if (!context.channel.isPrivate)
                        appendln("`${context.channel.guild.name}` Guild Join Date: ${context.channel.guild.getJoinTimeForUser(user)}")

                    append("```")
                }
            }
            
            return "Unable to find information for user $arg"
        }
    }

    class WhereisCommand: Command("This retrieves various information regarding a channel.") {
        
        @Executor
        fun execute(): String {
            return execute(context.channel.id)
        }

        @Executor
        fun execute(@Description("channel", "The channel to find information for (either Id, Name, or #Mention).") arg: String): String {
           val channel = findChannelFromMessage(arg, context.message)

            if (channel != null) {
                return buildString {
                    appendln("```xl")

                    val header = "Information for channel ${channel.name}:"
                    appendln(header)
                    appendln("=".repeat(header.length))
                    
                    appendln("Name: #${channel.name}")
                    appendln("ID: ${channel.id}")
                    appendln("Channel Creation Date: ${channel.creationDate}")
                    
                    appendln("```")

                    appendln("```xl")
                    
                    val guild = channel.guild
                    
                    val guildHeader = "In guild ${guild.name}:"
                    appendln(guildHeader)
                    appendln("=".repeat(guildHeader.length))
                    
                    appendln("ID: ${guild.id}")
                    appendln("Owner: ${guild.owner.name}#${guild.owner.discriminator}")
                    appendln("Icon: ${guild.iconURL}")
                    appendln("Region: ${guild.region}")
                    appendln("Guild Creation Date: ${guild.creationDate}")
                    appendln("Text Channel Count: ${guild.channels.size}")
                    appendln("Voice Channel Count ${guild.voiceChannels.size}")
                    appendln("User Count: ${guild.users.size}")
                    
                    appendln("Roles:")
                    guild.roles.forEach { 
                        val name = if (it.isEveryoneRole) "@ everyone" else it.name
                        
                        appendln("* $name (ID: ${it.id})")
                    }
                    
                    append("```")
                }
            }

            return "Unable to find information for channel $arg"
        }
    }
}
