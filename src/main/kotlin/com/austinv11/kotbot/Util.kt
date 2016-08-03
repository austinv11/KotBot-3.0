package com.austinv11.kotbot

import com.austinv11.kotbot.modules.api.commands.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser
import java.util.*

fun findUserFromMessage(input: String, context: IMessage): IUser? {
    val channel = context.channel
    var user: IUser? = null
    if (input.contains('#') && input.split("#").size == 2) { //User#Discrim
        val split = input.split('#')
        if (split[1].filter { it.isDigit() }.length == split[1].length) { //Ensures the discrim is all numbers
            KotBot.CLIENT.guilds.forEach {
                user = it.users.find { usr ->
                    return@find usr.getDisplayName(it) == split[0] && usr.discriminator == split[1]
                }

                if (user != null)
                    return@forEach
            }
        }
    } else if (input.startsWith("<@") && context.mentions.filter { it != KotBot.SELF }.size > 0) { //@Mention
        user = context.mentions.find { it != KotBot.SELF }
    } else if (input.filter { it.isDigit() }.length == input.length) { //Id as its all numeric
        user = KotBot.CLIENT.getUserByID(input)
    } else { //Name (probably)
        user = (if (channel.isPrivate) KotBot.CLIENT.users else channel.guild.users)
                .find { it.getDisplayName(channel.guild) == input }
    }
    
    return user
}

fun findChannelFromMessage(input: String, context: IMessage): IChannel? {
    var channel: IChannel? = null
    if (input.startsWith("<#") && input.endsWith(">")) { //#Mention
        channel = KotBot.CLIENT.getChannelByID(input.removePrefix("<#").removeSuffix(">"))
    } else if (input.filter { it.isDigit() }.length == input.length) { //Id, numeric
        channel = KotBot.CLIENT.getChannelByID(input)
    } else { //Name
        channel = KotBot.CLIENT.getChannels(false).find { it.name == input }
    }
    
    return channel
}

val IUser.isOwner: Boolean
    get() = this == KotBot.OWNER

val IUser.isSelf: Boolean
    get() = this == KotBot.SELF

val IUser.isSelfOrOwner: Boolean
    get() = this.isSelf || this.isOwner

private val logger: Logger = LoggerFactory.getLogger("KotBot")

/**
 * This represents KotBot's built-in logger instance.
 */
val Any.LOGGER: Logger
    get() = logger

/**
 * This represents caller information.
 */
val Any.caller: Caller
    get() = Caller()

internal val contextMap: MutableMap<Int, CommandContext> = mutableMapOf()

/**
 * This represents a command's context.
 */
val Command.context: CommandContext
    get() {
        val callr = caller
        return contextMap[Objects.hash(callr.thread, callr.`class`)]!!
    }

/**
 * This represents a caller context.
 */
data class Caller(val thread: Thread = Thread.currentThread(),
                  val `class`: String = spoofManager.getCallerClassName(4))

private val spoofManager = SpoofSecurityManager()

internal class SpoofSecurityManager : SecurityManager() {
    fun getCallerClassName(callStackDepth: Int): String {
        return classContext[callStackDepth].name
    }
}

/**
 * This represents the context for a command.
 */
data class CommandContext(val message: IMessage,
                          val channel: IChannel = message.channel,
                          val user: IUser = message.author,
                          val content: String = message.content)
