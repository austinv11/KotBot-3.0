package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.KotBot
import com.austinv11.kotbot.LOGGER
import com.austinv11.kotbot.clamp
import com.austinv11.kotbot.context
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.*
import com.github.kittinunf.fuel.Fuel
import com.google.gson.JsonElement
import sx.blah.discord.Discord4J
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ModuleDisabledEvent
import sx.blah.discord.handle.impl.events.ModuleEnabledEvent
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.handle.obj.Permissions
import sx.blah.discord.modules.IModule
import sx.blah.discord.util.BotInviteBuilder
import java.io.File
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.TimeUnit

class CoreModule : KotBotModule() {

    companion object {
        internal val commandMap: MutableMap<KotBotModule, List<Command>> = mutableMapOf()
        internal val disabledModules: MutableList<IModule> = mutableListOf()
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
        if (disabledModules.contains(e.module))
            disabledModules.remove(e.module)
        
        if (e.module is KotBotModule && !commandMap.containsKey(e.module as KotBotModule)) {
            commandMap[e.module as KotBotModule] = (e.module as KotBotModule).commands
        }
    }

    @EventSubscriber
    fun onModuleDisable(e: ModuleDisabledEvent) {
        disabledModules.add(e.module)
        
        if (e.module is KotBotModule && commandMap.containsKey(e.module as KotBotModule)) { 
            commandMap.remove(e.module as KotBotModule)
        }
    }

    class HelpCommand : Command("This provides usage information about the bot and various commands.",
            aliases = arrayOf("?", "halp", "h")) {

        @Executor
        fun list() {
            helpPage(0)
        }
        
        @Executor
        fun helpPage(@Description("page", "The help page to get.") page: Int) {
            val channel = if (KotBot.CONFIG.HELP_MESSAGE_TO_PM) context.user.orCreatePMChannel else context.channel

            channel.sendMessage(buildString {
                appendln("```xl")

                val helpPages = getHelpPages()
                var page = (page-1).clamp(0, helpPages.size-1)
                
                val header = "Command List (Page ${page+1}/${helpPages.size})"
                appendln(header)
                appendln("=".repeat(header.length))
                
                appendln(helpPages[page])

                append("```")
            })
        }
        
        private fun getHelpPages(): List<String> {
            val keys = CoreModule.commandMap.keys.toMutableList()
            keys.sortWith(Comparator { o1, o2 -> o1.name.compareTo(o2.name) })

            val commands = mutableListOf<String>()
            var builder = StringBuilder()
            var lineCount = 0
            
            keys.forEach {
                if (it.commands.size > 0) {
                    if (lineCount >= 9) { //Don't want the last line being a module name
                        lineCount = 0
                        commands.add(builder.toString())
                        builder = StringBuilder()
                    }
                    builder.appendln(it.name.removeSuffix("Module"))
                    lineCount++

                    it.commands.forEach {
                        builder.appendln("*${it.name}")
                        lineCount++
                        if (lineCount == 10) {
                            lineCount = 0
                            commands.add(builder.toString())
                            builder = StringBuilder()
                        }
                    }
                }
            }
            
            return commands
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
                        
                        appendln("Approved Users: "+command.approvedUsers)
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

                        val index = (usageType-1).clamp(0, command.executors.size-1)

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
            return "${user.getDisplayName(context.channel.guild)}#${user.discriminator}"
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

    class UpdateCommand: Command("This attempts to update this bot (This can take awhile)." , arrayOf("upgrade"), 
            expensive = true, approvedUsers = ApprovedUsers.OWNER)  {

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
                            context.channel.sendMessage("Successfully downloaded the new version of KotBot!")
                            backup.delete()
                            KotBot.restart()
                        },{
                            LOGGER.error("Error downloading KotBot!", it.exception)
                            throw it.exception
                        })
                    }.timeout(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toInt())
                            .timeoutRead(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toInt())
                } else {
                    throw FileSystemException(jarFile, backup, "Unable to move files.")
                }
            } catch (e: Exception) {
                backup.renameTo(File(KotBot.JAR_PATH)) //Restore backup
                throw e
            }
        }
    }
    
    class ShutdownCommand: Command("This kills the bot.", arrayOf("kill", "close"), 
            requiredPermissions = EnumSet.noneOf(Permissions::class.java), approvedUsers = ApprovedUsers.OWNER) {
        
        @Executor
        fun execute/*(Literally)*/() {
            KotBot.shutdown()
        }
    }
    
    class RestartCommand: Command("This restarts the bot.", arrayOf("reboot"), 
            requiredPermissions = EnumSet.noneOf(Permissions::class.java), approvedUsers = ApprovedUsers.OWNER) {
        
        @Executor
        fun execute() {
            KotBot.restart()
        }
    }
    
    class ModuleCommand: Command("This allows you to configure modules for the bot.", arrayOf("modules"), 
            approvedUsers = ApprovedUsers.OWNER) {
        
        @Executor
        fun execute(): String {
            return buildString { 
                appendln("```xl")
                
                appendln("Loaded Modules")
                appendln("==============")
                
                KotBot.CLIENT.moduleLoader.loadedModules.forEachIndexed { i, module ->
                    appendln("${i+1}. ${module.name.removeSuffix("Module")} v${module.version} by ${module.author}") 
                }
                
                appendln()
                appendln("Unloaded Modules")
                appendln("================")

                disabledModules.forEachIndexed { i, module ->
                    appendln("${i+1}. ${module.name.removeSuffix("Module")} v${module.version} by ${module.author}")
                }
                
                append("```")
            }
        }
        
        @Executor
        fun execute(@Description("action", "The action you want to perform on the module.") action: ActionType,
                    @Description("module", "The module you want to perform the action on.") module: String): String {
            if (action == ActionType.ENABLE) {
                val foundModule = disabledModules.find { it.name.removeSuffix("Module").equals(module, true) } ?: return "Cannot find module `$module`"

                if (KotBot.CLIENT.moduleLoader.loadModule(foundModule))
                    return "Successfully enabled module `$module`"
            } else if (action == ActionType.DISABLE) {
                val foundModule = KotBot.CLIENT.moduleLoader.loadedModules.find { it.name.removeSuffix("Module").equals(module, true) } ?: return "Cannot find module `$module`"
                
                if (foundModule is CoreModule)
                    return "Cannot disable the core module!"

                KotBot.CLIENT.moduleLoader.unloadModule(foundModule)
                return "Successfully disabled module `$module`"
            }
            
            return "Unable to ${action.toString().toLowerCase()} `$module`"
        }
        
        enum class ActionType {
            ENABLE, DISABLE
        }
    }
    
    class ConfigCommand: Command("This gets or sets a config value.", approvedUsers = ApprovedUsers.OWNER) {
        
        @Executor
        fun execute() = "```json\n${KotBot.GSON.toJson(KotBot.CONFIG)}\n```"
        
        @Executor
        fun execute(@Description("key", "The key value to get the value from.") key: String) = "`${getFieldFromInput(key)}`"

        @Executor
        fun execute(@Description("key", "The key value to get the value from.") key: String,
                    @Description("value", "The new value to set the config key to.") value: String): String {
            val changed = updateField(key, value)
            
            if (changed) {
                return "The new value of `${key.toUpperCase()}` is now `$value`"
            } else
                return "Unable to modify `${key.toUpperCase()}`!"
        }
        
        fun getFieldFromInput(field: String): String? {
            val fields = field.toUpperCase().split(" ").toMutableList()
            var iterator = 0
            var set = KotBot.GSON.toJsonTree(KotBot.CONFIG).asJsonObject.entrySet()
            
            while (fields.size > iterator) {
                val element = set.find { it.key == fields[iterator] } ?: return null

                iterator++

                if (fields.size == iterator) { //Reached the end of the list, no need to search for additional objects again
                    return element.value.toString()
                } else {
                    if (element.value.isJsonObject) {
                        set = element.value.asJsonObject.entrySet()
                    } else
                        return null
                }
            }
            
            return null
        }
        
        fun updateField(field: String, value: String): Boolean {
            val config = KotBot.GSON.toJsonTree(KotBot.CONFIG).asJsonObject
            val fields = field.toUpperCase().split(" ").toMutableList()
            var iterator = 0
            var set = config.entrySet()
            var parent: String = ""
            
            while (fields.size > iterator) {
                val element = set.find { it.key == fields[iterator] } ?: return false
                
                if (parent.isBlank())
                    parent = element.key
                
                iterator++
                
                if (fields.size == iterator) { //Reached the end of the list, no need to search for additional objects again
                    val json = coerceToObject(value, element.value)
                    val field = KotBot.CONFIG.javaClass.getDeclaredField(parent)
                    field.isAccessible = true
                    field.set(KotBot.CONFIG, json)
                    KotBot.CONFIG.update()
                    return true
                } else {
                    if (element.value.isJsonObject) {
                        set = element.value.asJsonObject.entrySet()
                    } else 
                        return false
                }
            }
            
            return false
        }
        
        fun coerceToObject(value: String, oldElement: JsonElement): Any? {
            if (oldElement.isJsonObject) { //This is probably a json string
                return KotBot.GSON.toJsonTree(value)
            } else { //Either a boolean, number or string
                if (oldElement.isJsonPrimitive) { //String or java primitive type
                    if (oldElement.asJsonPrimitive.isString) {
                        return value
                    } else if (oldElement.asJsonPrimitive.isBoolean) {
                        return value.toBoolean()
                    } else if (oldElement.asJsonPrimitive.isNumber) {
                        if (value.contains('.')) {
                            return value.toLong()
                        } else {
                            return value.toInt()
                        }
                    }
                } else if (oldElement.isJsonNull) { //No way to tell the type so just pass it as a string
                    return value
                } else if (oldElement.isJsonArray) { //It's an array
                    val list = mutableListOf<Any>()
                    val array = oldElement.asJsonArray
                    value.removePrefix("[").removeSuffix("]").split(",").forEachIndexed { i, str ->
                        list.add(coerceToObject(str.trim(), array.get(Math.max(array.size()-1, i)))!!)
                    }
                    return list
                }
            }
            
            return null
        }
    }
}

