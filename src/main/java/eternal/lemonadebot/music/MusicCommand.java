/*
 * The MIT License
 *
 * Copyright 2019 Neutroni.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.DatabaseManager;
import eternal.lemonadebot.database.GuildConfigManager;
import eternal.lemonadebot.messages.CommandMatcher;
import eternal.lemonadebot.messages.CommandPermission;
import java.util.HashMap;
import java.util.List;
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
    private final ConfigManager configManager;

    /**
     * Constructor
     *
     * @param dataBase DataBase to get music playback permission from
     */
    public MusicCommand(DatabaseManager dataBase) {
        this.configManager = dataBase.getConfig();
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
                + "<action> can be either play, skip, stop, pause or list\n"
                + "  play adds song to the song queue or resumes play if paused\n"
                + "  skip skips next song, songs by url, or songs in playlist provided\n"
                + "  stop clears the playlist and stops music playback\n"
                + "  list prints upcoming songs in playlist\n"
                + "[url] is the url of the music to play";
    }

    @Override
    public CommandPermission getPermission(Guild guild) {
        final GuildConfigManager guildConf = this.configManager.getGuildConfig(guild);
        return guildConf.getPlayPermission();
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
                    resumeTrack(textChannel);
                    return;
                }
                final String url = arguments[1];
                loadAndPlay(textChannel, url);
                break;
            }
            case "skip": {
                if (arguments.length < 2) {
                    skipTrack(textChannel, null);
                    return;
                }
                final String url = arguments[1];
                skipTrack(textChannel, url);
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
            case "list": {
                showPlaylist(textChannel);
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
                play(channel.getGuild(), musicManager, track);
                channel.sendMessage("Adding to queue " + track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                //Playlist without track selected
                if (playlist.getSelectedTrack() == null) {
                    for (AudioTrack tr : playlist.getTracks()) {
                        play(channel.getGuild(), musicManager, tr);
                    }
                    channel.sendMessage("Added playlist " + playlist.getName()).queue();
                    return;
                }
                //Single track from playlist
                trackLoaded(playlist.getSelectedTrack());
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

    private void skipTrack(TextChannel channel, String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        //No url, skip current track
        if (trackUrl == null) {
            musicManager.scheduler.nextTrack();
            channel.sendMessage("Skipped to next track.").queue();
            return;
        }

        //Skip tracks from url
        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (musicManager.scheduler.skipTrack(track)) {
                    channel.sendMessage("Removed from queue " + track.getInfo().title).queue();
                } else {
                    channel.sendMessage("Song not in the playlist").queue();
                }

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                //Playlist without track selected
                if (playlist.getSelectedTrack() == null) {
                    boolean skipped = false;
                    for (AudioTrack tr : playlist.getTracks()) {
                        if (musicManager.scheduler.skipTrack(tr)) {
                            skipped = true;
                        }
                    }
                    if (skipped) {
                        channel.sendMessage("Skipped songs in playlist " + playlist.getName()).queue();
                    } else {
                        channel.sendMessage("No songs to be skipped found").queue();
                    }
                    return;
                }

                //Single track from playlist
                final AudioTrack selectedTrack = playlist.getSelectedTrack();
                final boolean skipped = musicManager.scheduler.skipTrack(selectedTrack);
                if (skipped) {
                    channel.sendMessage("Skipped song " + playlist.getName()).queue();
                } else {
                    channel.sendMessage("Song not in queue, nothing skipped").queue();
                }
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not skip: " + exception.getMessage()).queue();
            }
        });
    }

    private void pauseTrack(TextChannel textChannel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.setPaused(true);
        textChannel.sendMessage("Playback paused").queue();
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
    private void resumeTrack(TextChannel textChannel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.setPaused(false);
        textChannel.sendMessage("Playback resumed").queue();
    }

    /**
     * Stop audio playback and clear queue
     *
     * @param textChannel channel for request
     */
    private void stopTrack(TextChannel textChannel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.stopTrack();
        musicManager.scheduler.clearPlaylist();
        textChannel.sendMessage("Playback stopped and playlist cleared").queue();
    }

    /**
     * Print the upcoming songs
     *
     * @param textChannel channel to respond on
     */
    private void showPlaylist(TextChannel textChannel) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        final List<AudioTrack> playlist = musicManager.scheduler.getPlaylist();
        final int songsToPrint = (playlist.size() < 10 ? playlist.size() : 10);
        final StringBuilder sb = new StringBuilder("Upcoming songs:\n");
        for (int i = 0; i < songsToPrint; i++) {
            sb.append(playlist.get(i).getInfo().title);
            sb.append("\n");
        }
        if (songsToPrint == 0) {
            sb.append("No songs in the playlist");
        }
        textChannel.sendMessage(sb.toString()).queue();
    }

}
