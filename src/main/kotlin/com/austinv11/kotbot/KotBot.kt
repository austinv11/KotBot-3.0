package com.austinv11.kotbot

import com.austinv11.kotbot.modules.impl.GithubWebhookModule
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import spark.Spark
import sx.blah.discord.Discord4J
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.impl.events.DiscordDisconnectedEvent
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.kotlin.bot
import sx.blah.discord.kotlin.extensions.on
import sx.blah.discord.kotlin.extensions.waitFor
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val startTime = System.currentTimeMillis()
    
    if (args.size < 1)
        throw IllegalArgumentException("At least one argument (token) required!")
    
    KotBot.TOKEN = args[0]
    
    bot {
        if (Discord4J.LOGGER is Discord4J.Discord4JLogger)
            (Discord4J.LOGGER as Discord4J.Discord4JLogger).setLevel(Discord4J.Discord4JLogger.Level.DEBUG)

        token = KotBot.TOKEN
        KotBot.CLIENT = this

        on<ReadyEvent> {
            KotBot.CLIENT = it.client

            LOGGER.info("KotBot v${KotBot.VERSION} has been initialized!")
            LOGGER.debug("Started in ${System.currentTimeMillis() - startTime}ms")
        }

        login()
    }
}

/**
 * The bot's main class.
 */
class KotBot {
    
    companion object {
        
        private var _token: String? = null
        private var _client: IDiscordClient? = null
        private var _config: Config? = null
        private var _owner: IUser? = null

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
         * The bot's account.
         */
        val SELF: IUser
            get() = CLIENT.ourUser
        /**
         * The owner of this bot.
         */
        val OWNER: IUser
            get() {
                if (_owner == null)
                    _owner = CLIENT.applicationOwner

                return _owner!!
            }
        /**
         * The gson instance used by this bot.
         */
        val GSON: Gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
        /**
         * The bot's config file.
         */
        val CONFIG_FILE: File = File("config.json")
        /**
         * The bot's config.
         */
        val CONFIG: Config
            get() {
                if (_config == null) {
                    if (CONFIG_FILE.exists()) {
                        _config = GSON.fromJson(CONFIG_FILE.reader(), Config::class.java)
                    } else {
                        CONFIG_FILE.createNewFile()
                        _config = Config()
                    }

                    val writer = CONFIG_FILE.writer()
                    GSON.toJson(_config, writer)
                    writer.close()
                }
                
                return _config!!
            }
        /**
         * The bot's .jar path.
         */
        val JAR_PATH: String = KotBot::class.java.protectionDomain.codeSource.location.toURI().path //Locates jar file (this doesn't work too well when it isn't compiled to a jar)

        /**
         * This shuts down the bot.
         */
        fun shutdown() {
            KotBot.CLIENT.logout()
            KotBot.CLIENT.waitFor<DiscordDisconnectedEvent>()
            exitProcess(0)
        }
        
        /**
         * This restarts the bot.
         */
        fun restart() {
            Spark.stop()
            ProcessBuilder("java", "-jar", KotBot.JAR_PATH.toString(), KotBot.TOKEN).inheritIO().start()
            shutdown()
        }
    }
}
