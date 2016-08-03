package com.austinv11.kotbot.modules.impl

import com.austinv11.kotbot.KotBot
import com.austinv11.kotbot.context
import com.austinv11.kotbot.modules.api.KotBotModule
import com.austinv11.kotbot.modules.api.commands.Command
import com.austinv11.kotbot.modules.api.commands.CommandException
import com.austinv11.kotbot.modules.api.commands.Description
import com.austinv11.kotbot.modules.api.commands.Executor
import net.dv8tion.d4j.player.MusicPlayer
import net.dv8tion.jda.player.Playlist
import net.dv8tion.jda.player.source.AudioTimestamp
import net.dv8tion.jda.player.source.RemoteSource
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.handle.obj.Permissions
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

class MusicModule: KotBotModule() { //NOTE: This requires python, youtube-dl, ffmpeg and ffprobe to be installed
    
    companion object {
        init { //Workaround for JDA-Player being broken on unix
            modifyStaticFinalField(Playlist::class.java.getDeclaredField("YOUTUBE_DL_PLAYLIST_ARGS"), 
                    Collections.unmodifiableList(Arrays.asList(
                        "youtube-dl", //youtube-dl program file
                        "-q", //quiet. No standard out.
                        "-j", //Print JSON
                        "--flat-playlist" //Get ONLY the urls of the playlist if this is a playlist.
                    )))
            
            modifyStaticFinalField(RemoteSource::class.java.getDeclaredField("YOUTUBE_DL_LAUNCH_ARGS"), 
                    Collections.unmodifiableList(Arrays.asList(
                        "youtube-dl", //youtube-dl program file
                        "-q", //quiet. No standard out.
                        "-f", "bestaudio/best", //Format to download. Attempts best audio-only, followed by best video/audio combo
                        "--no-playlist", //If the provided link is part of a Playlist, only grabs the video, not playlist too.
                        "-o", "-" //Output, output to STDout
                )))
        }
        
        private fun modifyStaticFinalField(field: Field, newValue: Any?) {
            field.isAccessible = true

            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

            field.set(null, newValue)
        } 
        
        private val playerPool: MutableMap<IGuild, MusicPlayer> = mutableMapOf()
        
        internal fun getPlayer(guild: IGuild): MusicPlayer {
            if (!playerPool.containsKey(guild)) {
                val player = MusicPlayer()
                guild.audioManager.audioProvider = player
                playerPool[guild] = player
            }
            
            return playerPool[guild]!!
        }

        internal fun getPlayer(channel: IChannel): MusicPlayer {
            return getPlayer(channel.guild)
        }
        
        internal fun reset(guild: IGuild) {
            val player = getPlayer(guild)
            player.stop()
            playerPool.remove(guild)
        }
        
        internal fun reset(channel: IChannel) {
            reset(channel.guild)
        }
    }
    
    override fun initialize() {
        
    }
    
    class JoinCommand: Command("This makes the bot join the provided voice channel.", 
            requiredPermissions = EnumSet.of(Permissions.VOICE_CONNECT, Permissions.VOICE_SPEAK),
            allowInPrivateChannels = false) {
        
        @Executor
        fun execute(@Description("channel", "The name or id of the channel to join in this guild.") channel: String) {
            val vChannel: IVoiceChannel? = context.channel.guild.getVoiceChannelByID(channel)
                    ?: context.channel.guild.voiceChannels.find { it.name.equals(channel.replaceFirst("#", ""), true) } 
                    ?: throw CommandException("Unable to locate channel $channel")
            
            vChannel?.join()
        }
    }
    
    class LeaveCommand: Command("This makes the bot leave the channel it is currently playing audio in.",
            requiredPermissions = EnumSet.noneOf(Permissions::class.java), allowInPrivateChannels = false) {
        
        @Executor
        fun execute() {
            KotBot.CLIENT.connectedVoiceChannels.find { it.guild == context.channel.guild }?.leave()
        }
    }
    
    class VolumeCommand: Command("This sets the volume of the music player in the current guild.",
            requiredPermissions = EnumSet.noneOf(Permissions::class.java), allowInPrivateChannels = false) {
        
        @Executor
        fun execute(@Description("volume", "The new volume (recommended to be between 0-1).") volume: Float) {
            getPlayer(context.channel).volume = volume
        }
    }
    
    class PlaylistCommand: Command("This gets various playlist information.", allowInPrivateChannels = false) {
        
        @Executor
        fun execute(): String {
            return buildString {
                appendln("```xl")
                
                val queue = getPlayer(context.channel).audioQueue

                if (queue.isEmpty())
                    appendln("Empty Queue!")
                else {
                    val header = "Queue (${queue.size} Tracks)"
                    appendln(header)
                    appendln("=".repeat(header.length))
                    
                    var totalTime: Int = 0 
                    
                    queue.forEachIndexed { i, audio ->
                        append("${i+1}. ")
                        
                        if (audio == null)
                            appendln("[Unknown]")
                        else {
                            val timestamp = audio.info.duration?.timestamp ?: "Unknown"
                            appendln("${audio.info.title} ($timestamp)")
                            totalTime += audio.info.duration?.totalSeconds ?: 0
                        }
                    }
                    
                    appendln()
                    appendln("Total Queue Time: ${AudioTimestamp.fromSeconds(totalTime).timestamp}")
                }
                
                append("```")
            }
        }
    }
    
    class CurrentCommand: Command("This gets the currently playing song.", arrayOf("nowplaying"), 
            allowInPrivateChannels = false) {
        
        @Executor
        fun execute(): String {
            val player = getPlayer(context.channel)
            
            if (!player.isPlaying)
                return "There is currently no song playing."
            
            val info = player.currentAudioSource.info
            val title = if (info.error != null) info.title else player.currentAudioSource.source
            val totalTime = if (info.error != null) info.duration.timestamp else "Unknown"
            
            return "Playing: **%s** (%s/%s)".format(title, player.currentTimestamp, totalTime)
        }
    }
    
    class SkipCommand: Command("This skips the current track.", 
            requiredPermissions = EnumSet.noneOf(Permissions::class.java), allowInPrivateChannels = false) {
        
        @Executor
        fun execute() {
            getPlayer(context.channel).skipToNext()
        }
    }
    
    class LoopCommand: Command("This toggles whether the player is looping.", arrayOf("repeat"), 
            allowInPrivateChannels = false) {
        
        @Executor
        fun execute(): String {
            return execute(!getPlayer(context.channel).isRepeat)
        }
        
        @Executor
        fun execute(@Description("status", "Whether to loop or not.") status: Boolean): String {
            val player = getPlayer(context.channel)
            val currentStatus = status
            player.isRepeat = currentStatus

            return "The player is now ${if (!currentStatus) "*not* " else ""}repeating"
        }
    }
    
    class ShuffleCommand: Command("This toggles whether the player is shuffling.", allowInPrivateChannels = false) {

        @Executor
        fun execute(): String {
            return execute(!getPlayer(context.channel).isShuffle)
        }

        @Executor
        fun execute(@Description("status", "Whether to shuffle or not.") status: Boolean): String {
            val player = getPlayer(context.channel)
            val currentStatus = status
            player.isShuffle = currentStatus

            return "The player is now ${if (!currentStatus) "*not* " else ""}repeating"
        }
    }
    
    class ClearCommand: Command("This clears the current audio queue.", 
            requiredPermissions = EnumSet.noneOf(Permissions::class.java), allowInPrivateChannels = false) {
        
        @Executor
        fun execute() {
            getPlayer(context.channel).audioQueue.clear()
        }
    }
    
    class PauseCommand: Command("This pauses audio playback.",
            requiredPermissions = EnumSet.noneOf(Permissions::class.java), allowInPrivateChannels = false) {
        
        @Executor
        fun execute() {
            getPlayer(context.channel).pause()
        }
    }
    
    class ResumeCommand: Command("This resumes audio playback.",
            requiredPermissions = EnumSet.noneOf(Permissions::class.java), allowInPrivateChannels = false) {
        
        @Executor
        fun execute() {
            getPlayer(context.channel).play()
        }
    }
    
    class ResetCommand: Command("This resets the audio player.",
            requiredPermissions = EnumSet.noneOf(Permissions::class.java), allowInPrivateChannels = false) {
        
        @Executor
        fun execute() {
            reset(context.channel)
        }
    }
    
    class StopCommand: Command("This stops playback completely.",
            requiredPermissions = EnumSet.noneOf(Permissions::class.java), allowInPrivateChannels = false) {
        
        @Executor
        fun execute() {
            getPlayer(context.channel).stop()
        }
    }
    
    class ReplayCommand: Command("This restarts the current track.",
            requiredPermissions = EnumSet.noneOf(Permissions::class.java), allowInPrivateChannels = false) {
        
        @Executor
        fun execute() {
            val player = getPlayer(context.channel)
            
            if (player.previousAudioSource != null || !player.isStopped)
                player.reload(true)
        }
    }
    
    class PlayCommand: Command("This plays the provided track.", arrayOf("queue"), allowInPrivateChannels = false,
            expensive = true) {
        
        @Executor
        fun execute(@Description("url", "The source url for the song.") url: String): String {
            val playlist = Playlist.getPlaylist(url)
            val sources = mutableListOf(*playlist.sources.toTypedArray())
            var queuedTracks = 0
            
            context.channel.sendMessage("Found ${sources.size} playlist entries, processing...")
            context.channel.toggleTypingStatus()
            
            val player = getPlayer(context.channel)
            
            sources.forEach { 
                if (it.info.error == null) {
                    player.audioQueue.add(it)
                    
                    queuedTracks++
                } else {
                    context.channel.sendMessage("Error detected on source: ${it.info.error}")
                    context.channel.toggleTypingStatus()
                    sources.remove(it)
                }
            }
            
            return "$queuedTracks tracks have been queued!"
        } 
    }
}
