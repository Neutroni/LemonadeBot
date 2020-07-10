/*
 * The MIT License
 *
 * Copyright 2020 Neutroni.
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
import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.permissions.MemberRank;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
     */
    public MusicCommand() {
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
    public String getDescription() {
        return "Play music";
    }

    @Override
    public String getHelpText() {
        return "Syntax: music <action> [url]\n"
                + "<action> can be either play, skip, stop, pause or list\n"
                + " play - adds song to the song queue or resumes play if paused\n"
                + " skip - skips next song, songs by url, or songs in playlist provided\n"
                + " stop - clears the playlist and stops music playback\n"
                + " list - prints upcoming songs in playlist\n"
                + "[url] is the url of the music to play";
    }

    @Override
    public MemberRank getDefaultRank() {
        return MemberRank.MEMBER;
    }

    @Override
    public void respond(CommandMatcher message, GuildDataStore guildData) {
        final TextChannel textChannel = message.getTextChannel();

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
                textChannel.sendMessage("Unknown operation: " + arguments[0]).queue();
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
                    for (final AudioTrack tr : playlist.getTracks()) {
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

    private synchronized GuildMusicManager getGuildAudioPlayer(final Guild guild) {
        final long guildId = guild.getIdLong();

        final GuildMusicManager musicManager = musicManagers.computeIfAbsent(guildId, (Long id) -> {
            return new GuildMusicManager(playerManager, guild.getAudioManager());
        });

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    private void skipTrack(TextChannel channel, String trackUrl) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

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
                    for (final AudioTrack tr : playlist.getTracks()) {
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

    /**
     * Pause music plaback
     *
     * @param textChannel textchannel for reques
     */
    private void pauseTrack(TextChannel textChannel) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.setPaused(true);
        textChannel.sendMessage("Playback paused").queue();
    }

    /**
     * Play audiotrack
     *
     * @param guild guild to play on
     * @param musicManager musicManager to use
     * @param track AudioTrack to play
     */
    private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        //Make sure we are connected
        final AudioManager audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            for (final VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(voiceChannel);
                break;
            }
        }

        //Queue the track, starts playback if queue is empty
        musicManager.scheduler.queue(track);
    }

    /**
     * Resume audio playback
     *
     * @param textChannel channel for the request
     */
    private void resumeTrack(TextChannel textChannel) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
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
        final EmbedBuilder eb = new EmbedBuilder();

        final AudioTrack currentTrack = musicManager.player.getPlayingTrack();
        if (currentTrack == null) {
            eb.setTitle("No songs currently in playlist.");
            eb.setDescription("Add music using \"music play <url>\"");
            textChannel.sendMessage(eb.build()).queue();
            return;
        }

        eb.setTitle("Currently playing:");
        eb.setDescription(" " + currentTrack.getInfo().title);

        //Get upcoming songs
        final StringBuilder sb = new StringBuilder();
        final int songsToPrint = (playlist.size() < 10 ? playlist.size() : 10);
        for (int i = 0; i < songsToPrint; i++) {
            sb.append(playlist.get(i).getInfo().title);
            sb.append('\n');
        }

        //Check if there is any songs in playlist
        if (playlist.isEmpty()) {
            sb.append("No music in playlist");
        }

        final MessageEmbed.Field upcomingSongsField = new MessageEmbed.Field("Upcoming songs:", sb.toString(), false);
        eb.addField(upcomingSongsField);

        long playlistLengthMS = 0;
        for (final AudioTrack t : playlist) {
            playlistLengthMS += t.getDuration();
        }
        Duration playlistDuration = Duration.ofMillis(playlistLengthMS);
        final long hoursRemining = playlistDuration.toHours();
        final int minutesPart = playlistDuration.toMinutesPart();
        final int secondsPart = playlistDuration.toSecondsPart();
        final String durationString = String.format("%d:%02d:%02d", hoursRemining, minutesPart, secondsPart);
        MessageEmbed.Field playlistLenghtField = new MessageEmbed.Field("Playlist lenght:", " " + durationString + " remaining.", false);
        eb.addField(playlistLenghtField);

        //Send the message
        textChannel.sendMessage(eb.build()).queue();
    }

}
