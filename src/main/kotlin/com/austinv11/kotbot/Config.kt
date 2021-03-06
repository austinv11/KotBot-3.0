package com.austinv11.kotbot

data class Config(var COMMAND_PREFIX: String = "~", 
                  var HELP_MESSAGE_TO_PM: Boolean = false, 
                  var GITHUB_WEBHOOKS: MutableList<Webhook> = mutableListOf(),
                  var PORT: Int = 3000,
                  var FORWARD_PRIVATE_MESSAGES_TO_OWNER: Boolean = true) {
    
    data class Webhook(var TYPE: Type, var CHANNEL: String, var REPO: String) {
        
        enum class Type {
            ISSUES, PULL_REQUESTS, RELEASES, COMMITS, ALL
        }
    }
    
    internal fun update() {
        KotBot.CONFIG_FILE.writeText(KotBot.GSON.toJson(this))
    }
}
