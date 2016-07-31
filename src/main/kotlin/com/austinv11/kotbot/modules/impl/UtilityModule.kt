package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.KotBot
import com.austinv11.kotbot.context
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.Command
import com.austinv11.kotbot.modules.api.commands.Executor
import sx.blah.discord.api.DiscordStatus
import java.time.Instant
import java.time.ZoneId

class UtilityModule : KotBotModule() {

    override fun initialize() {

    }

    class PingCommand: Command("This provides information about the response time for this bot.") {

        @Executor
        fun execute(): String {
            return buildString {
                appendln("Pong!")
                println("Current Time: ${Instant.now().toEpochMilli()} Message Time: ${context.message.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}")
                appendln("Received this message in: `${System.currentTimeMillis()-context.message.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}`ms")
                appendln("Last websocket ping response time: `${KotBot.CLIENT.responseTime}`ms")
            }
        }
    }
}
