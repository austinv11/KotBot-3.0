package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.KotBot
import com.austinv11.kotbot.LOGGER
import com.austinv11.kotbot.context
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.Command
import com.austinv11.kotbot.modules.api.commands.CommandException
import com.austinv11.kotbot.modules.api.commands.Description
import com.austinv11.kotbot.modules.api.commands.Executor
import com.github.kittinunf.fuel.Fuel
import sx.blah.discord.Discord4J
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.DiscordDisconnectedEvent
import sx.blah.discord.handle.impl.events.ModuleDisabledEvent
import sx.blah.discord.handle.impl.events.ModuleEnabledEvent
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.kotlin.extensions.waitFor
import sx.blah.discord.util.BotInviteBuilder
import java.io.File
import java.lang.reflect.Method
import java.util.*
import kotlin.system.exitProcess

class CoreModule : KotBotModule() {

    companion object {
        internal val commandMap: MutableMap<KotBotModule, List<Command>> = mutableMapOf()
    }

    override fun initialize() {

    }

    @EventSubscriber
    fun onReady(e: ReadyEvent) {
        e.client.moduleLoader.loadedModules.forEach {
            if (it is KotBotModule) {
                commandMap[it] = it.commands
            }
        }
    }

    @EventSubscriber
    fun onModuleEnable(e: ModuleEnabledEvent) {
        if (e.module is KotBotModule && !commandMap.containsKey(e.module as KotBotModule)) {
            commandMap[e.module as KotBotModule] = (e.module as KotBotModule).commands
        }
    }

    @EventSubscriber
    fun onModuleDisable(e: ModuleDisabledEvent) {
        if (e.module is KotBotModule && commandMap.containsKey(e.module as KotBotModule)) {
            commandMap.remove(e.module as KotBotModule)
        }
    }

    class HelpCommand : Command("This provides usage information about the bot and various commands.",
            aliases = arrayOf("?", "halp", "h")) {

        @Executor
        fun list() {
            val keys = CoreModule.commandMap.keys.toMutableList()
            keys.sortWith(Comparator { o1, o2 -> o1.name.compareTo(o2.name) })

            val channel = if (KotBot.CONFIG.HELP_MESSAGE_TO_PM) context.user.orCreatePMChannel else context.channel

            channel.sendMessage(buildString {
                appendln("```xl")

                keys.forEach {
                    appendln(it.name.removeSuffix("Module"))

                    it.commands.forEach {
                        appendln("*${it.name}")
                    }
                }

                append("```")
            })
        }

        private fun sortByParamCount(list: MutableList<Method>): MutableList<Method> {
            list.sortWith(Comparator { o1, o2 ->
                val initialResult = o1.parameterCount.compareTo(o2.parameterCount)
                if (initialResult == 0) {
                    return@Comparator o1.name.compareTo(o2.name)
                } else
                    return@Comparator initialResult
            })
            return list
        }

        @Executor
        fun helpPage(@Description("command", "The command to get help for.") cmd: String) {
            val channel = if (KotBot.CONFIG.HELP_MESSAGE_TO_PM) context.user.orCreatePMChannel else context.channel

            CoreModule.commandMap.forEach {
                val command = it.value.find {
                    return@find it.name == cmd.toLowerCase() || it.aliases.contains(cmd.toLowerCase())
                }

                if (command != null) {
                    channel.sendMessage(buildString {
                        appendln("```xl")

                        val header = "Help page for command ${command.name}"
                        appendln(header)
                        appendln("=".repeat(header.length))

                        appendln(command.description)
                        appendln()

                        appendln("Aliases: "+command.aliases.joinToString(", "))
                        appendln()

                        appendln("Can be used in public channels: "+command.allowInPublicChannels)
                        appendln("Can be used in private channels: "+command.allowInPrivateChannels)
                        appendln()

                        appendln("Requires permissions: "+command.requiredPermissions.joinToString(", "))
                        appendln()

                        appendln("Usage:")
                        val executors = sortByParamCount(command.executors.toMutableList())
                        executors.forEachIndexed { i, executor ->
                            append("${i+1}. ${command.name} ")
                            command.params[executor]?.forEach {
                                append("<")
                                append(it.first)
                                append("> ")
                            }
                            appendln()
                        }

                        append("```")
                    })
                    return@helpPage
                }
            }

            throw CommandException("Unable to find command `$cmd`")
        }

        @Executor
        fun extendedHelpPage(@Description("command", "The command to get help for.") cmd: String,
                             @Description("usage index", "The usage to get additional information about.") usageType: Int) {
            val channel = if (KotBot.CONFIG.HELP_MESSAGE_TO_PM) context.user.orCreatePMChannel else context.channel

            CoreModule.commandMap.forEach {
                val command = it.value.find {
                    return@find it.name == cmd.toLowerCase() || it.aliases.contains(cmd.toLowerCase())
                }

                if (command != null) {
                    channel.sendMessage(buildString {
                        appendln("```xl")

                        var index = usageType-1
                        if (index < 1)
                            index = 1
                        if (index >= command.executors.size)
                            index = command.executors.size-1

                        val executor = sortByParamCount(command.executors.toMutableList())[index]

                        val header = buildString {
                            append("Argument page for ${command.name} ")
                            command.params[executor]?.forEach {
                                append("<")
                                append(it.first)
                                append("> ")
                            }
                        }
                        appendln(header)
                        appendln("=".repeat(header.length))

                        var argCount: Int = 1
                        command.params[executor]?.forEach {
                            appendln("${argCount++}. ${it.first} - ${it.second}")
                        }

                        append("```")
                    })
                    return@extendedHelpPage
                }
            }

            throw CommandException("Unable to find command `$cmd`")
        }
    }

    class InfoCommand: Command("This provides various information relating to this bot", aliases = arrayOf("about")) {

        private fun formatName(user: IUser): String {
            val name = if (context.channel.isPrivate) user.name else user.getDisplayName(context.channel.guild)
            return "$name#${user.discriminator}"
        }

        @Executor
        fun execute(): String {
            return buildString {
                appendln("```xl")

                val header = "KotBot v${KotBot.VERSION}"
                appendln(header)
                appendln("=".repeat(header.length))

                appendln("KotBot is a bot written in Kotlin and built on the Discord4J API.")
                appendln("Invite Link: ${BotInviteBuilder(KotBot.CLIENT).build()}")
                appendln("Github Link: https://github.com/austinv11/KotBot-3.0")
                appendln("Prefix: ${KotBot.CONFIG.COMMAND_PREFIX} or @${formatName(KotBot.SELF)}")
                appendln("Connected to ${KotBot.CLIENT.guilds.size} servers.")
                appendln("Bot User: ${formatName(KotBot.SELF)} (ID: ${KotBot.SELF.id})")
                appendln("Bot Owner: ${formatName(KotBot.OWNER)} (ID: ${KotBot.OWNER.id})")
                appendln("Bot started at ${Discord4J.getLaunchTime()}")
                appendln("Available Memory: ${Runtime.getRuntime().freeMemory()/100000}mb / ${Runtime.getRuntime().maxMemory()/100000}mb")
                appendln("Discord4J Version: ${Discord4J.VERSION}")
                appendln("Kotlin Version: ${Package.getPackage("kotlin").implementationVersion}")
                appendln("JVM Version: ${System.getProperty("java.version")}")

                append("```")
            }
        }
    }

    class UpdateCommand: Command("This attempts to update this bot.", arrayOf("upgrade"), expensive = true, ownerOnly = true)  {

        companion object {
            const val DOWNLOAD_URL: String = "https://jitpack.io/com/github/austinv11/KotBot-3.0/-SNAPSHOT/KotBot-3.0--SNAPSHOT-all.jar"
        }

        @Executor
        fun execute() {
            val jarFile = File(KotBot.JAR_PATH)
            val backup = File(jarFile.parent, "KotBot-backup.jar")

            try {
                if (jarFile.renameTo(backup)) { //Want a backup in case the download goes wrong
                    LOGGER.info("Downloading a new version of KotBot...")
                    Fuel.download(DOWNLOAD_URL).destination { response, url -> 
                        File(KotBot.JAR_PATH)
                    }.response { req, res, result -> 
                        result.fold({
                            LOGGER.info("Successfully downloaded the new version of KotBot!")
                            backup.delete()
                            ProcessBuilder("java", "-jar", KotBot.JAR_PATH.toString(), KotBot.TOKEN).inheritIO().start()
                            KotBot.CLIENT.logout()
                            KotBot.CLIENT.waitFor<DiscordDisconnectedEvent>()
                            exitProcess(0)
                        },{
                            LOGGER.error("Error downloading KotBot!", it.exception)
                            throw it.exception
                        })
                    }
                } else {
                    throw FileSystemException(jarFile, backup, "Unable to move files.")
                }
            } catch (e: Exception) {
                backup.renameTo(File(KotBot.JAR_PATH)) //Restore backup
                throw e
            }
        }
    }
}

