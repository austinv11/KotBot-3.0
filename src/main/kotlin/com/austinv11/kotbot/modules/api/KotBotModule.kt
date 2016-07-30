package com.austinv11.kotbot.modules.api

import com.austinv11.kotbot.KotBot
import com.austinv11.kotbot.modules.api.commands.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.modules.IModule

/**
 * This is the base module class for KotBot modules.
 */
abstract class KotBotModule : IModule {
    
    private val name = this.javaClass.simpleName.replace("Kt", "")
    private val commands: MutableList<Command> = mutableListOf()
    
    /**
     * The logger for this module.
     */
    val LOGGER: Logger = LoggerFactory.getLogger(name)
    
    override fun getName() = name

    override fun enable(client: IDiscordClient?): Boolean {
        initialize()
        return true
    }

    override fun getVersion() = KotBot.VERSION

    override fun getMinimumDiscord4JVersion() = KotBot.MINIMUM_DISCORD4J_VERSION

    override fun getAuthor() = "austinv11"

    override fun disable() {
        LOGGER.info("Disabled the $name module")
    }

    /**
     * This registers a command for this module.
     * 
     * @param command The command to register.
     */
    fun registerCommand(command: Command) {
        commands + command
    }
    
    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {
        
    }
    
    /**
     * This is called to initialize the module.
     */
    abstract fun initialize()
}
