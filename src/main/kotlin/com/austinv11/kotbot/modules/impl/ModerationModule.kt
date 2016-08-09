package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.*
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.ApprovedUsers
import com.austinv11.kotbot.modules.api.commands.Command
import com.austinv11.kotbot.modules.api.commands.Description
import com.austinv11.kotbot.modules.api.commands.Executor
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import sx.blah.discord.handle.obj.Permissions
import sx.blah.discord.kotlin.extensions.buffer
import java.util.*
import java.util.concurrent.TimeUnit

class ModerationModule: KotBotModule() {
    
    override fun initialize() {
        
    }
    
    class PromoteCommand: Command("This promotes a user to administrator (for this bot).", 
            approvedUsers = ApprovedUsers.OWNER) {
        
        @Executor
        fun execute(@Description("user", "The user to promote.") user: String): String {
            val userObj = findUserFromMessage(user, context.message) ?: return ":poop: Can't find user $user"
            
            if (!userObj.isSelfOrOwner && !isAdmin(userObj)) {
                transaction { 
                    Administrators.insert { it[id] = userObj.id }
                }
            }
            
            return ":ok_hand:"
        }
    }

    class DemoteCommand: Command("This demotes a user from administrator (for this bot).",
            approvedUsers = ApprovedUsers.OWNER) {

        @Executor
        fun execute(@Description("user", "The user to demote.") user: String): String {
            val userObj = findUserFromMessage(user, context.message) ?: return ":poop: Can't find user $user"

            if (!userObj.isSelfOrOwner && isAdmin(userObj)) {
                transaction {
                    Administrators.deleteWhere { Administrators.id like userObj.id }
                }
            }

            return ":ok_hand:"
        }
    }
    
    class BanCommand: Command("This bans the provided user.", allowInPrivateChannels = false, 
            requiredPermissions = EnumSet.of(Permissions.SEND_MESSAGES, Permissions.BAN), 
            approvedUsers = ApprovedUsers.ADMINISTRATORS)  {

        @Executor
        fun execute(@Description("user", "The user to ban.") user: String): String {
            val userObj = findUserFromMessage(user, context.message) ?: return ":poop: Can't find user $user"

            if (!userObj.isSelfOrOwner)
                context.channel.guild.banUser(userObj)

            return ":ok_hand:"
        }

        @Executor
        fun execute(@Description("user", "The user to ban.") user: String,
                    @Description("deleteMessageForDays", "The amount of days to purge messages from.") messages: Int): String {
            val userObj = findUserFromMessage(user, context.message) ?: return ":poop: Can't find user $user"

            if (!userObj.isSelfOrOwner)
                context.channel.guild.banUser(userObj, messages)

            return ":ok_hand:"
        }
    }

    class UnbanCommand: Command("This unbans the provided user.", arrayOf("pardon"), allowInPrivateChannels = false,
            requiredPermissions = EnumSet.of(Permissions.SEND_MESSAGES, Permissions.BAN),
            approvedUsers = ApprovedUsers.ADMINISTRATORS)  {

        @Executor
        fun execute(@Description("user", "The user to unban.") user: String): String {
            val userObj = findUserFromMessage(user, context.message) ?: return ":poop: Can't find user $user"

            if (!userObj.isSelfOrOwner)
                context.channel.guild.pardonUser(userObj.id)

            return ":ok_hand:"
        }
    }
    
    class SoftBanCommand: Command("This bans and then unbans the user in order to purge his/her messages.", 
            allowInPrivateChannels = false, 
            requiredPermissions = EnumSet.of(Permissions.SEND_MESSAGES, Permissions.BAN), 
            approvedUsers = ApprovedUsers.ADMINISTRATORS) {

        @Executor
        fun execute(@Description("user", "The user to softban.") user: String): String {
            val userObj = findUserFromMessage(user, context.message) ?: return ":poop: Can't find user $user"

            if (!userObj.isSelfOrOwner) {
                context.channel.guild.banUser(userObj, 7)
                context.channel.guild.pardonUser(userObj.id)
            }

            return ":ok_hand:"
        }
    }
    
    class KickCommand: Command("This kicks the user.", allowInPrivateChannels = false,
            requiredPermissions = EnumSet.of(Permissions.SEND_MESSAGES, Permissions.KICK),
            approvedUsers = ApprovedUsers.ADMINISTRATORS) {

        @Executor
        fun execute(@Description("user", "The user to kick.") user: String): String {
            val userObj = findUserFromMessage(user, context.message) ?: return ":poop: Can't find user $user"

            if (!userObj.isSelfOrOwner)
                context.channel.guild.kickUser(userObj)

            return ":ok_hand:"
        }
    }
    
    class PurgeCommand: Command("This prunes a configurable amount of messages.", allowInPrivateChannels = false, 
            requiredPermissions = EnumSet.of(Permissions.MANAGE_MESSAGES, Permissions.SEND_MESSAGES), 
            approvedUsers = ApprovedUsers.ADMINISTRATORS) {
        
        @Executor
        fun execute(): String {
            return execute(50)
        }
        
        @Executor
        fun execute(@Description("messages", "The number of messages to purge.") messages: Int): String {
            var toDelete: Int = messages
            val channel = context.channel
            while (toDelete > 0) {
                val deleting = toDelete - Math.min(toDelete, 100)
                toDelete -= deleting
                buffer { 
                    if (channel.messages.size < deleting)
                        channel.messages.load(deleting)
                    
                    channel.messages.bulkDelete(channel.messages.subList(0, deleting))
                }
            }
            return ":ok_hand:"
        }
    }
    
    class TempBanCommand: Command("This bans a user for a temporary amount of time.", allowInPrivateChannels = false,
            requiredPermissions = EnumSet.of(Permissions.BAN, Permissions.SEND_MESSAGES),
            approvedUsers = ApprovedUsers.ADMINISTRATORS) {
        
        companion object {
            val tempbanTimer = Timer("Temp-Ban Timer", true)
        }
        
        @Executor
        fun execute(@Description("user", "The user to temporarily ban.") user: String): String {
            return execute(user, 24)
        }

        @Executor
        fun execute(@Description("user", "The user to temporarily ban.") user: String,
                    @Description("time", "The amount of time to ban the user for.") time: Int): String {
            return execute(user, time, TimeUnit.HOURS)
        }

        @Executor
        fun execute(@Description("user", "The user to temporarily ban.") user: String,
                    @Description("time", "The amount of time to ban the user for.") time: Int,
                    @Description("unit", "The unit of time used for the time parameter.") unit: TimeUnit): String {
            val userObj = findUserFromMessage(user, context.message) ?: return ":poop: Cannot find user $user"
            val userId = userObj.id
            val guild = context.channel.guild
            
            KotBot.CONFIG.TEMP_BANS.put(userId, System.currentTimeMillis()+unit.toMillis(time.toLong()))
            KotBot.CONFIG.update()
            
            tempbanTimer.schedule(object : TimerTask() {
                override fun run() {
                    LOGGER.info("Pardoning user with the id $userId (Temp ban)")
                    guild.pardonUser(userId)
                    
                    this.cancel()
                }
            }, unit.toMillis(time.toLong()))
            
            guild.banUser(userObj)
            
            return ":ok_hand:"
        }
    }
}
