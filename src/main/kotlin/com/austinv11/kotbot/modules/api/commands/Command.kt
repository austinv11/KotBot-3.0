package com.austinv11.kotbot.modules.api.commands

import sx.blah.discord.handle.obj.Permissions
import java.util.*

/**
 * Use this to declare a command.
 */
open class Command(val aliases: Array<String> = emptyArray(), 
              val description: String,
              val allowInPrivateChannels: Boolean = true,
              val allowInPublicChannels: Boolean = true,
              val expensive: Boolean = false,
              val requiredPermissions: EnumSet<Permissions> = EnumSet.of(Permissions.SEND_MESSAGES))
