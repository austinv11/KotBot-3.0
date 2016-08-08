package com.austinv11.kotbot

data class Config(var COMMAND_PREFIX: String = "~", var HELP_MESSAGE_TO_PM: Boolean = false, 
                  var ADMINISTATORS: MutableList<String> = mutableListOf(), 
                  var GITHUB_WEBHOOKS: MutableList<Webhook> = mutableListOf(),
                  var TEMP_BANS: MutableMap<String, Long> = mutableMapOf(),
                  var TAGS: MutableMap<String, MutableMap<String, String>> = mutableMapOf(),
                  var PORT: Int = 3000) {
    
    data class Webhook(var TYPE: Type, var CHANNEL: String, var REPO: String) {
        
        enum class Type {
            ISSUES, PULL_REQUESTS, RELEASES, COMMITS, ALL
        }
    }
    
    internal fun update() {
        KotBot.CONFIG_FILE.writeText(KotBot.GSON.toJson(this))
    }
}
