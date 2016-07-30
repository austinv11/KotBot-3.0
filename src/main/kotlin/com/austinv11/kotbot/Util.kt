package com.austinv11.kotbot

import com.austinv11.kotbot.modules.api.commands.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser
import java.lang.reflect.Method
import java.util.*

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
                  val `class`: String = spoofManager.getCallerClassName(3))

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