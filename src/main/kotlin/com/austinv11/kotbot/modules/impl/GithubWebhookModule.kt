package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.Config
import com.austinv11.kotbot.KotBot
import com.austinv11.kotbot.context
import com.austinv11.kotbot.findChannelFromMessage
import com.austinv11.kotbot.github.events.Commit
import com.austinv11.kotbot.github.events.Issue
import com.austinv11.kotbot.github.events.PullRequest
import com.austinv11.kotbot.github.events.Release
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.ApprovedUsers
import com.austinv11.kotbot.modules.api.commands.Command
import com.austinv11.kotbot.modules.api.commands.Description
import com.austinv11.kotbot.modules.api.commands.Executor
import spark.Spark

class GithubWebhookModule: KotBotModule() { //Suck it voltbot!
    
    companion object {
        var isEnabled: Boolean = false
    }
    
    override fun initialize() {
        isEnabled = true
        
        Spark.port(KotBot.CONFIG.PORT)
        
        Spark.post("/github", { request, response -> 
            if (isEnabled) {
                when (request.headers("X-GitHub-Event")) {
                    "ping" -> {
                        //Ignored for now, pinged when the webhook is activated on a repo
                    }
                    "issues" -> {
                        val issue: Issue = KotBot.GSON.fromJson(request.body(), Issue::class.java)
                        
                        if (issue.action == "opened" || issue.action == "closed" || issue.action == "reopened") {
                            val hook = KotBot.CONFIG.GITHUB_WEBHOOKS.find {
                                (it.TYPE == Config.Webhook.Type.ISSUES || it.TYPE == Config.Webhook.Type.ALL)
                                        && issue.issue.html_url.contains(it.REPO)
                            }

                            if (hook != null) {
                                val channel = KotBot.CLIENT.getChannelByID(hook.CHANNEL)!!

                                channel.sendMessage(buildString { 
                                    appendln("__**${issue.repository.full_name}**__")
                                    append("Issue #${issue.issue.number}: **${issue.issue.title}** has been " +
                                        "*${issue.action}* [${issue.issue.user.login}]\n<${issue.issue.html_url}>")
                                })
                            }
                        }
                    }
                    "pull_request" -> {
                        val pullRequest: PullRequest = KotBot.GSON.fromJson(request.body(), PullRequest::class.java)
                        
                        if (pullRequest.action == "opened" || pullRequest.action == "closed" 
                                || pullRequest.action == "reopened") {
                            val hook = KotBot.CONFIG.GITHUB_WEBHOOKS.find {
                                (it.TYPE == Config.Webhook.Type.PULL_REQUESTS || it.TYPE == Config.Webhook.Type.ALL)
                                        && pullRequest.pull_request.html_url.contains(it.REPO)
                            }

                            if (hook != null) {
                                val channel = KotBot.CLIENT.getChannelByID(hook.CHANNEL)!!
                                
                                pullRequest.action = 
                                        if (pullRequest.action == "closed" && pullRequest.pull_request.merged) "merged" 
                                        else pullRequest.action
                                
                                channel.sendMessage(buildString {
                                    appendln("__**${pullRequest.repository.full_name}**__")
                                    append("Pull Request #${pullRequest.number}: " + 
                                            "**${pullRequest.pull_request.title}** " +
                                            "(+${pullRequest.pull_request.additions} " +
                                            "-${pullRequest.pull_request.deletions}) " +
                                            "has been *${pullRequest.action}* " +
                                            "[${pullRequest.pull_request.user.login}]\n" +
                                            "<${pullRequest.pull_request.html_url}>")
                                })
                            }
                        }
                    }
                    "push" -> {
                        val commit: Commit = KotBot.GSON.fromJson(request.body(), Commit::class.java)
                        
                        val hook = KotBot.CONFIG.GITHUB_WEBHOOKS.find { 
                            (it.TYPE == Config.Webhook.Type.COMMITS || it.TYPE == Config.Webhook.Type.ALL)
                                && commit.commits[0].url.contains(it.REPO) }

                        if (hook != null) {
                            val channel = KotBot.CLIENT.getChannelByID(hook.CHANNEL)!!

                            channel.sendMessage(buildString {
                                appendln("__**${commit.repository.full_name} (${commit.ref.removePrefix("refs/heads/")})**__")
                                
                                commit.commits.forEach { 
                                    appendln("`${it.id.substring(0..6)}` ${it.message.lines()[0]} [${it.author.username}]")
                                }
                            })
                        }
                    }
                    "release" -> {
                        val release: Release = KotBot.GSON.fromJson(request.body(), Release::class.java)
                        
                        if (!release.release.draft) {
                            val hook = KotBot.CONFIG.GITHUB_WEBHOOKS.find {
                                (it.TYPE == Config.Webhook.Type.ISSUES || it.TYPE == Config.Webhook.Type.ALL)
                                        && release.release.html_url.contains(it.REPO)
                            }

                            if (hook != null) {
                                val channel = KotBot.CLIENT.getChannelByID(hook.CHANNEL)!!

                                channel.sendMessage(buildString {
                                    appendln("__**${release.repository.full_name}**__")

                                    appendln("${if (release.release.prerelease) "Prer" else "R"}elease: " +
                                            "**${release.release.name}**\n<${release.release.html_url}>")
                                    
                                    val changelogPreview = buildString { 
                                        val split = release.release.body.lines()
                                        
                                        split.forEachIndexed { i, line -> 
                                            if (i < 10) //limits to ten lines
                                                appendln(line)
                                            else {
                                                appendln("...")
                                                return@forEachIndexed
                                            }
                                        }
                                    }
                                    
                                    append(changelogPreview.removeSuffix("\n"))
                                })
                            }
                        }
                    }
                }
            }
        })
    }
    
    override fun disable() {
        isEnabled = false
        super.disable()
    }
    
    class GithubCommand: Command("This allows you to modify Github webhooks which are monitored by this bot.",
            arrayOf("git"), approvedUsers = ApprovedUsers.ADMINISTRATORS) {
        
        @Executor
        fun execute(): String {
            return buildString { 
                appendln("${KotBot.CONFIG.GITHUB_WEBHOOKS.size} Hooks currently being monitored:")
                
                KotBot.CONFIG.GITHUB_WEBHOOKS.forEachIndexed { i, webhook -> 
                    appendln("${i+1}. Repo: <${webhook.REPO}>. Monitored Actions: ${webhook.TYPE}. " +
                            "Reporting Channel: ${KotBot.CLIENT.getChannelByID(webhook.CHANNEL)}")
                }
            }
        }

        @Executor
        fun execute(@Description("action", "The action to perform.") action: Action,
                    @Description("repo", "The repository to monitor.") repo: String): String {
            return execute(action, Config.Webhook.Type.ALL, repo, context.channel.id)
        }
        
        @Executor
        fun execute(@Description("action", "The action to perform.") action: Action,
                    @Description("repo", "The repository to monitor.") repo: String,
                    @Description("channel", "The channel to post information to.") channel: String): String {
            return execute(action, Config.Webhook.Type.ALL, repo, channel)
        }

        @Executor
        fun execute(@Description("action", "The action to perform.") action: Action,
                    @Description("type", "The type of event to monitor for.") type: Config.Webhook.Type,
                    @Description("repo", "The repository to monitor.") repo: String,
                    @Description("channel", "The channel to post information to.") channelId: String): String {
            val channel = findChannelFromMessage(channelId, context.message) ?: return ":poop: Cannot find that channel."
            
            if (action == Action.ADD)
                KotBot.CONFIG.GITHUB_WEBHOOKS.add(Config.Webhook(type, channel.id, repo.removeSurrounding("<", ">")))
            else
                KotBot.CONFIG.GITHUB_WEBHOOKS.removeAll { it.REPO == repo 
                        && (it.TYPE == Config.Webhook.Type.ALL || it.TYPE == type) }
            
            KotBot.CONFIG.update()
            
            return ":ok_hand:"
        }
        
        
        enum class Action {
            ADD, REMOVE
        }
    }
}
