package com.austinv11.kotbot

data class Config(var COMMAND_PREFIX: String = "~", var HELP_MESSAGE_TO_PM: Boolean = false, 
                  var ADMINISTATORS: MutableList<String> = mutableListOf(), 
                  var GITHUB_WEBHOOKS: MutableList<Webhook> = mutableListOf()) {
    
    data class Webhook(var TYPE: Type, var CHANNEL: String, var REPO: String) {
        
        enum class Type {
            ISSUES, PULL_REQUESTS, RELEASES, COMMITS, ALL
        }
    }
    
    internal fun update() {
        KotBot.CONFIG_FILE.writeText(KotBot.GSON.toJson(this))
    }
}
