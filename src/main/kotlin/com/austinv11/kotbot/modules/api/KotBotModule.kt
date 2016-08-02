package com.austinv11.kotbot.modules.api

import com.austinv11.kotbot.CommandContext
import com.austinv11.kotbot.KotBot
import com.austinv11.kotbot.contextMap
import com.austinv11.kotbot.modules.api.commands.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.api.internal.DiscordUtils
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.Permissions
import sx.blah.discord.kotlin.extensions.buffer
import sx.blah.discord.modules.IModule
import sx.blah.discord.util.MissingPermissionsException
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.Executors

/**
 * This is the base module class for KotBot modules.
 */
abstract class KotBotModule : IModule {
    
    private val name = this.javaClass.simpleName.replace("Kt", "")
    val commands: MutableList<Command> = mutableListOf()
    private val executor = Executors.newCachedThreadPool()
    
    /**
     * The logger for this module.
     */
    val LOGGER: Logger = LoggerFactory.getLogger(name)
    
    override fun getName() = name

    override fun enable(client: IDiscordClient?): Boolean {
        javaClass.declaredClasses.forEach {
            if (Command::class.java.isAssignableFrom(it))
                registerCommand(it.newInstance() as Command)
        }
        initialize()
        return true
    }

    override fun getVersion() = KotBot.VERSION

    override fun getMinimumDiscord4JVersion() = KotBot.MINIMUM_DISCORD4J_VERSION

    override fun getAuthor() = "austinv11"

    override fun disable() {
        LOGGER.info("Disabled the $name module")
        commands.clear()
    }

    /**
     * This registers a command for this module.
     * 
     * @param command The command to register.
     */
    fun registerCommand(command: Command) {
        this.commands.add(command)
    }
    
    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) { //Handles command
        var msg = event.message.content
        //Strip command prefix (either mention or actual prefix)
        if (msg.startsWith(KotBot.CONFIG.COMMAND_PREFIX)) {
            msg = msg.removePrefix(KotBot.CONFIG.COMMAND_PREFIX)
        } else if (event.message.mentions.size > 0 && event.message.mentions[0] == KotBot.SELF) {
            val usesMention: Boolean
           if (msg.startsWith(KotBot.SELF.mention(false)))
               usesMention = false
           else if (msg.startsWith(KotBot.SELF.mention(true)))
               usesMention = true
           else
               return //False alarm

            msg = msg.removePrefix(KotBot.SELF.mention(usesMention)).trimStart()
        } else {
            return //Not a command at all
        }

        //Find command to execute
        val commandString = msg.split(" ")[0]
        var command: Command? = null
        for (cmd in commands) {
            if (cmd.name == commandString || cmd.aliases.contains(commandString)) {
                command = cmd
                break
            }
        }
        if (command == null)
            return //No suitable command found

        if (!command.allowInPrivateChannels && event.message.channel.isPrivate) {
            buffer { event.message.reply("This command cannot be executed in a private channel!") }
            return
        }
        if (!command.allowInPublicChannels && !event.message.channel.isPrivate) {
            buffer { event.message.reply("This command cannot be executed in a public channel!") }
            return
        }

        try {
            DiscordUtils.checkPermissions(KotBot.CLIENT, event.message.channel, command.requiredPermissions)
        } catch (e: MissingPermissionsException) {
            if (event.message.channel.getModifiedPermissions(KotBot.SELF).contains(Permissions.SEND_MESSAGES)) { //Only send an error message if the bot has the correct permissions to do so
                buffer { event.message.reply("I do not have the required permissions to do that action. "+e.errorMessage) }
                return
            }
        }

        if (command.ownerOnly && event.message.author != KotBot.OWNER) {
            buffer { event.message.reply("Only my owner can use that command!") }
            return
        }

        //Executing the actual command
        val commandArgs = msg.removePrefix(commandString).trim()
        if (command.expensive) {
            executor.submit {
                val channel = event.message.channel
                if (!channel.typingStatus)
                    channel.toggleTypingStatus()

                executeCommand(command as Command, commandArgs, event.message)

                if (channel.typingStatus)
                    channel.toggleTypingStatus()
            }
        } else {
            executeCommand(command, commandArgs, event.message)
        }
    }

    private fun executeCommand(command: Command, args: String, message: IMessage, channel: IChannel = message.channel) {
        val hash = Objects.hash(Thread.currentThread(), command.javaClass.name) //TODO: Make sure this works
        contextMap[hash] = CommandContext(message)

        val result: String?
        try {
            result = command._execute(args)

            if (result != null)
                buffer { channel.sendMessage(result) }
        } catch (e: Throwable) {
            var exception: Throwable = e
            if (e is InvocationTargetException)
                exception = e.targetException
            buffer { channel.sendMessage("ERROR: ${exception.javaClass.name}: ${exception.message}") }
            exception.printStackTrace()
        } finally {
            contextMap.remove(hash)
        }
    }
    
    /**
     * This is called to initialize the module.
     */
    abstract fun initialize()
}
