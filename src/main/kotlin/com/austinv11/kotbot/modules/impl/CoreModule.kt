package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.context
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.Command
import com.austinv11.kotbot.modules.api.commands.Executor

class CoreModule : KotBotModule() {

    override fun initialize() {

    }

    class BlahCommand : Command("blah") {

        @Executor
        fun blah(): String {
            return "WEW "+context.user
        }

        @Executor
        fun blah(thing: String): String {
            return "WEW2 "+context.user
        }
    }
}

