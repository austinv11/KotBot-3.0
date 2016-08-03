package com.austinv11.kotbot

data class Config(var COMMAND_PREFIX: String = "~", var HELP_MESSAGE_TO_PM: Boolean = false, 
                  var ADMINISTATORS: MutableList<String> = mutableListOf()) {
    
    internal fun update() {
        KotBot.CONFIG_FILE.writeText(KotBot.GSON.toJson(this))
    }
}
