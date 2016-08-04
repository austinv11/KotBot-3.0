package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.KotBot
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.Command
import com.austinv11.kotbot.modules.api.commands.Description
import com.austinv11.kotbot.modules.api.commands.Executor
import org.apache.commons.lang3.SystemUtils
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.kotlin.extensions.on
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RateLimitException
import java.lang.reflect.Method
import java.util.*

class Discord4JHelpModule : KotBotModule() {
    
    companion object {
        const val OWNER_ID = "84745855399129088" //Austinv11's (me) id
        
        val PROJECTS: Array<Pair<String, String>> = arrayOf(
                Pair("sx.blah.discord.kotlin", "https://github.com/Discord4J-Addons/Discord4K"),
                Pair("sx.blah.discord", "https://github.com/austinv11/Discord4J"),
                Pair("com.austinv11.kotbot", "https://github.com/austinv11/KotBot-3.0"), 
                Pair("com.austinv11.modules", "https://github.com/Discord4J-Addons/Module-Loader-Plus"),
                Pair("com.austinv11.d4g", "https://github.com/Discord4J-Addons/Discord4J-Gradle-Plugin"))
    }
    
    override fun initialize() {
        val instance = this
        
        KotBot.CLIENT.on<ReadyEvent> {
            if (!KotBot.OWNER.id.equals(OWNER_ID)) { //This is meant to only be used by me for my personal server
                KotBot.CLIENT.moduleLoader.unloadModule(instance)
            }       
        }
    }
    
    @EventSubscriber
    fun onMessage(event: MessageReceivedEvent) { //Analyzes messages to determine a way to best help people
        
    }
    
    class StackTraceCommand: Command("This analyzes the provided stacktrace.", arrayOf("trace", "stack")) {
        
        companion object {
            const val MAX_LINKS_TO_POST: Int = 3 
        }
        
        fun diagnose(stacktrace: Array<StackTraceLine>): String? { //Run routines to diagnose common problems TODO: Add more fixes to common errors
            when (stacktrace[0].exceptionClass) {
                DiscordException::class.java -> {
                    val message = stacktrace[0].message ?: ""
                    
                    when (message) {
                        "Invalid token!" -> {
                            return "Your bot token was probably incorrect, double check it here: " +
                                    "<https://discordapp.com/developers/applications/me>"
                        }
                        "Login error occurred! Are your login details correct?" -> {
                            return "Your login credentials were likely incorrect, you should double check them."
                        }
                        "Cannot PM yourself!" -> {
                            return "You tried to pm yourself, silly you."
                        }
                        "This action can only be performed by as user" -> {
                            return "Your bot is a \"bot\" account, meaning it cannot do certain actions which are only " +
                                    "available to actual user accounts."
                        }
                        "No login info present!" -> {
                            return "You did not provide any login information to your `ClientBuilder` instance."
                        }
                    }
                }
                RateLimitException::class.java -> {
                    return "You hit the discord ratelimit! You should either slow down your requests or use Discord4J's" +
                            "built-in RequestBuffer."
                }
                MissingPermissionsException::class.java -> {
                    return "You are likely missing one or more permissions, take a look at the error message again: " +
                            "`${stacktrace[0].message}`. If you are *certain* your bot has the required permissions " +
                            "then please report this as a Discord4J bug."
                }
            }
            
            return "Unable to find an answer, try searching stackoverflow: " +
                    "<http://stackoverflow.com/search?q=%5Bjava%5D+${stacktrace[0].line.replace(' ', '+')}>"
        }
        
        fun getStackFrames(stacktrace: String): Array<StackTraceLine> {
            val tokenizer = StringTokenizer(stacktrace, SystemUtils.LINE_SEPARATOR)
            val list = mutableListOf<StackTraceLine>()
            
            while (tokenizer.hasMoreTokens()) {
                list.add(StackTraceLine(tokenizer.nextToken()))
            }
            
            return list.toTypedArray()
        }
        
        
        @Executor
        fun execute(@Description("stacktrace", "The actual stacktrace.") stacktrace: String): String {
            val trace = getStackFrames(stacktrace.removePrefix("```").removePrefix("\n").removeSuffix("```").removeSuffix("\n").trim()) //Strip codeblock args
            return buildString { 
                var linkCount: Int = 0
                trace.forEach { 
                    if (it.isHeader) {
                        appendln("Detected a *${it.exceptionClass?.simpleName ?: "Unknown Exception"}*")
                        appendln()
                        
                        val analysis = diagnose(trace)
                        appendln("Diagnosis: ${analysis ?: "Cannot suggest a solution to this particular error."}")
                        appendln()
                        
                        appendln("Relevant links (Limited to $MAX_LINKS_TO_POST links):")
                    }
                    if (linkCount < MAX_LINKS_TO_POST) {
                        val link = it.getGithubLink(false)
                        if (link != null) {
                            appendln("* $link")
                            linkCount++
                        }
                    }
                }
            }
        }
        
        class StackTraceLine(val line: String, var exceptionClass: Class<in Throwable>? = null) {
            
            var isHeader: Boolean
            var isBody: Boolean
            var message: String? = null
            var lineNumber: Int? = null
            var clazz: Class<*>? = null
            var method: Method? = null
            var `package`: String? = null
            var fileName: String? = null
            
            init {
                var modifiableLine = line.trim()
                
                isHeader = line.contains(": ")
                isBody = !isHeader
                
                if (isHeader) {
                    val split = line.split(": ")
                    
                    try {
                        exceptionClass = Class.forName(split[0]) as Class<in Throwable>?
                        message = split[1]
                    } catch (e: Exception) {}
                } else {
                    modifiableLine = modifiableLine.removePrefix("at ")
                    `package` = buildString { 
                        modifiableLine.toCharArray().forEach {
                            if (it.isLowerCase() || it.isDigit() || it == '.')
                                append(it)
                            else
                                return@buildString
                        }
                    }.removeSuffix(".")
                    
                    try {
                        val classString = `package`+"."+modifiableLine.removePrefix(`package`!!+".").split(".")[0]
                        clazz = Class.forName(classString)
                        modifiableLine = modifiableLine.removePrefix(classString)
                        
                        val methodString = modifiableLine.split("(")[0]
                        method = clazz!!.methods.find { it.name == methodString }
                        modifiableLine = modifiableLine.removePrefix(modifiableLine.split("(")[0])
                        
                        modifiableLine = modifiableLine.removePrefix("(").removeSuffix(")")
                        val split = modifiableLine.split(":")
                        fileName = split[0]
                        lineNumber = split[1].toInt()
                    } catch (e: Exception) {}
                }
            }
            
            fun getGithubLink(useDev: Boolean): String? {
                PROJECTS.forEach { 
                    if (isHeader) {
                        if (exceptionClass?.name?.contains(it.first) ?: false) {
                            val `class` = exceptionClass!!.name!!.substringAfterLast('.')
                            val `package` = exceptionClass!!.name!!.removeSuffix("."+`class`)
                            
                            return "<${it.second}/blob/${if (useDev) "dev" else "master"}/src/main/" +
                                    "${if (`package`.contains("kot")) "kotlin" else "java"}/" +
                                    "${`package`.replace('.', '/')}/$`class`." +
                                    "${if (`package`.contains("kot")) "kt" else "java"}>"
                        }
                    } else {
                        if (`package`?.contains(it.first) ?: false) {
                            return "<${it.second}/blob/${if (useDev) "dev" else "master"}/src/main/" +
                                    "${if (`package`?.contains("kot") ?: false) "kotlin" else "java"}/" +
                                    "${`package`?.replace('.', '/')}/$fileName#L$lineNumber>"
                        }
                    }
                }
                
                return null
            }
            
            override fun toString(): String {
                return line
            }
        }
    }
}
