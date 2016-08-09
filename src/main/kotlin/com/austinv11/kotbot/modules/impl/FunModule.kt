package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.Tags
import com.austinv11.kotbot.context
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.Command
import com.austinv11.kotbot.modules.api.commands.Description
import com.austinv11.kotbot.modules.api.commands.Executor
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class FunModule: KotBotModule() {
    
    override fun initialize() {
        
    }

    class TagCommand: Command("This gets or modifies a tag for this guild.", arrayOf("tags", "ahh"), 
            allowInPrivateChannels = false) {
        
        @Executor
        fun execute(): String {
            val guild = context.channel.guild
            return buildString { 
                val tags = mutableListOf<String>()
                
                transaction { 
                    Tags.select { Tags.guild_id like guild.id }.forEach { tags.add(it[Tags.tag_id]) }
                }
                
                appendln("`${tags.size}` available tags:")
                
                val joiner = StringJoiner(", ")
                tags.forEach {
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
            val guild = context.channel.guild
            var returnMessage = ":ok_hand:"
            when (action) {
                Actions.PUT -> {
                    if (content == null || content.isBlank())
                        return ":poop: Cannot input an empty tag!"
                    
                    transaction { 
                        Tags.insert { 
                            it[guild_id] = guild.id
                            it[tag_id] = tag
                            it[value] = content
                        }
                    }
                }
                Actions.REMOVE -> {
                    transaction {
                        if (Tags.select { (Tags.guild_id like guild.id) and (Tags.tag_id like tag) }.firstOrNull() == null)
                                returnMessage = ":poop: Cannot find tag `$tag`"
                        else
                            Tags.deleteWhere { (Tags.guild_id like guild.id) and (Tags.tag_id like tag) }
                    }
                }
                Actions.GET -> {
                    transaction {
                        if (Tags.select { (Tags.guild_id like guild.id) and (Tags.tag_id like tag) }.firstOrNull() == null)
                            returnMessage = ":poop: Cannot find tag `$tag`"
                        else
                            returnMessage = Tags.select { (Tags.guild_id like guild.id) and (Tags.tag_id like tag) }.first()[Tags.value]
                    }
                }
            }
            
            return returnMessage
        }
        
        enum class Actions {
            PUT, REMOVE, GET
        }
    }
}
