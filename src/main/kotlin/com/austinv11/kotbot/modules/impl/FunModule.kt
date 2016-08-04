package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.KotBot
import com.austinv11.kotbot.context
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.Command
import com.austinv11.kotbot.modules.api.commands.Description
import com.austinv11.kotbot.modules.api.commands.Executor
import java.util.*

class FunModule: KotBotModule() {
    
    override fun initialize() {
        
    }

    class TagCommand: Command("This gets or modifies a tag for this guild.", arrayOf("tags", "ahh"), 
            allowInPrivateChannels = false) {
        
        @Executor
        fun execute(): String {
            return buildString { 
                val tags = KotBot.CONFIG.TAGS[context.channel.guild.id] ?: return ":poop: No tags!"
                
                appendln("`${tags.size}` available tags:")
                
                val joiner = StringJoiner(", ")
                tags.keys.forEach {
                    joiner.add(it)
                }
                
                append(joiner.toString())
            }
        }
        
        @Executor
        fun execute(@Description("tag", "The tag to find.") tag: String): String {
            return execute(Actions.GET, tag)
        }

        @Executor
        fun execute(@Description("action", "The action to perform.") action: Actions,
                    @Description("tag", "The tag to find.") tag: String): String {
            return execute(action, tag, null)
        }
        
        @Executor
        fun execute(@Description("action", "The action to perform.") action: Actions,
                    @Description("tag", "The tag to find.") tag: String,
                    @Description("content", "The content to put in the tag.") content: String?): String {
            when (action) {
                Actions.PUT -> {
                    if (content == null || content.isBlank())
                        return ":poop: Cannot input an empty tag!"
                    
                    var tags = KotBot.CONFIG.TAGS[context.channel.guild.id]
                    if (tags == null) {
                        tags = mutableMapOf()
                        KotBot.CONFIG.TAGS.put(context.channel.guild.id, tags)
                    }
                    
                    tags.put(tag, content)
                }
                Actions.REMOVE -> {
                    KotBot.CONFIG.TAGS[context.channel.guild.id]?.remove(tag) ?: return ":poop: Cannot find tag `$tag`"
                }
                Actions.GET -> {
                    val tagContent = KotBot.CONFIG.TAGS[context.channel.guild.id]?.getOrElse(tag, {null})
                            ?: return ":poop: Cannot find tag `$tag`"

                    return tagContent
                }
            }
            
            KotBot.CONFIG.update()
            return ":ok_hand:"
        }
        
        enum class Actions {
            PUT, REMOVE, GET
        }
    }
}
