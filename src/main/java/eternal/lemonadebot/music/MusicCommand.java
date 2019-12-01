/*
 * Mostly Apache-2-0 Licenced from https://github.com/sedmelluq/lavaplayer
 * Functions relatinng to ChatCommand under MIT License
 */
package eternal.lemonadebot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.messages.CommandManager;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandPermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

/**
 * Command for playing music
 *
 * @author Neutroni
 */
public class MusicCommand implements ChatCommand {

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    /**
     * Constructor
     *
     * @param parser Parser to use for parsing messages
     */
    public MusicCommand(CommandManager parser) {
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        this.musicManagers = new HashMap<>();
    }

    @Override
    public String getCommand() {
        return "music";
    }

    @Override
    public String getHelp() {
        return "Syntax: music <action> [url]\n"
                + "<action> can be either play, skip or pause\n"
                + "[url] is the url of the music to play";
    }

    @Override
    public CommandPermission getPermission() {
        return CommandPermission.MEMBER;
    }

    @Override
    public void respond(CommandMatcher message) {
        //Check that we are in a server and not a private chat
        final Optional<TextChannel> optChannel = message.getTextChannel();
        if (optChannel.isEmpty()) {
            message.getMessageChannel().sendMessage("Music can only be played on servers.").queue();
            return;
        }
        final TextChannel textChannel = optChannel.get();

        //Get arguments and parse accordingly
        final String[] arguments = message.getArguments(2);
        if (arguments.length == 0) {
            textChannel.sendMessage("Provide operation to perform, check help for possible operations.").queue();
            return;
        }
        switch (arguments[0]) {
            case "play": {
                if (arguments.length < 2) {
                    unPauseTrack(textChannel);
                    return;
                }
                final String url = arguments[1];
                loadAndPlay(textChannel, url);
                break;
            }
            case "skip": {
                skipTrack(textChannel);
                break;
            }
            case "pause": {
                pauseTrack(textChannel);
                break;
            }
            case "stop": {
                stopTrack(textChannel);
                break;
            }
            default: {
                textChannel.sendMessage("Unkown operation: " + arguments[0]).queue();
            }
        }
    }

    private void loadAndPlay(TextChannel channel, final String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

                play(channel.getGuild(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(channel.getGuild(), musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager, guild.getAudioManager());
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    private void skipTrack(TextChannel textChannel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.scheduler.nextTrack();

        textChannel.sendMessage("Skipped to next track.").queue();
    }

    private void pauseTrack(TextChannel textChannel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.setPaused(true);
    }

    private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        connectToFirstVoiceChannel(guild.getAudioManager());
        musicManager.scheduler.queue(track);
    }

    private static void connectToFirstVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(voiceChannel);
                break;
            }
        }
    }

    /**
     * Resume audio playback
     *
     * @param textChannel channel for the request
     */
    private void unPauseTrack(TextChannel textChannel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.setPaused(false);
    }

    /**
     * Stop audio playback and clear queue
     * @param textChannel channel for request
     */
    private void stopTrack(TextChannel textChannel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.stopTrack();
        musicManager.scheduler.clearPlaylist();
    }

}
