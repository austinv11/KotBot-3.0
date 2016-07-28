package com.austinv11.kotbot

import org.reflections.Reflections
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sx.blah.discord.Discord4J
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.kotlin.bot
import sx.blah.discord.kotlin.extensions.on

fun main(args: Array<String>) {
    //Disabling Reflections logging
    Reflections.log = null
    
    val startTime = System.currentTimeMillis()
    
    if (args.size < 1)
        throw IllegalArgumentException("At least one argument (token) required!")
    
    KotBot.TOKEN = args[0]
    
    bot {
        if (Discord4J.LOGGER is Discord4J.Discord4JLogger)
            (Discord4J.LOGGER as Discord4J.Discord4JLogger).setLevel(Discord4J.Discord4JLogger.Level.DEBUG)
        
        token = KotBot.TOKEN
        
        on<ReadyEvent> { 
            KotBot.CLIENT = it.client
            
            LOGGER.info("KotBot v${KotBot.VERSION} has been initialized!")
            LOGGER.debug("Started in ${System.currentTimeMillis()-startTime}ms")
        }
        
        login()
    }
}

val logger: Logger = LoggerFactory.getLogger("KotBot")
val Any.LOGGER: Logger
    get() = logger

/**
 * The bot's main class.
 */
class KotBot {
    
    companion object {
        
        private var _token: String? = null
        private var _client: IDiscordClient? = null

        /**
         * The version of the bot.
         */
        val VERSION = "1.0.0-SNAPSHOT"
        /**
         * The minimum required version of Discord4J.
         */
        val MINIMUM_DISCORD4J_VERSION = "2.5.3"
        /**
         * The client representing this bot instance.
         */
        var CLIENT: IDiscordClient
            get() = _client!!
            set(value) {
                _client = value
            }
        /**
         * The token this bot is using.
         */
        var TOKEN: String
            get() = if (_token != null) _token!! else ""
            set(value) {
                _token = value
            }
        /**
         * The owner of this bot.
         */
        val OWNER: IUser
            get() = CLIENT.applicationOwner
    }
}
