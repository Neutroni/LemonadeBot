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
import eternal.lemonadebot.commands.ChatCommand;
import eternal.lemonadebot.commands.CommandContext;
import eternal.lemonadebot.messageparsing.CommandMatcher;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.permissions.PermissionManager;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationCache;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
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
public class MusicCommand extends ChatCommand {

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    /**
     * Constructor
     */
    public MusicCommand() {
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(this.playerManager);
        this.musicManagers = new ConcurrentHashMap<>();
    }

    @Override
    public String getCommand(final ResourceBundle locale) {
        return locale.getString("COMMAND_MUSIC");
    }

    @Override
    public String getDescription(final ResourceBundle locale) {
        return locale.getString("DESCRIPTION_MUSIC");
    }

    @Override
    public String getHelpText(final ResourceBundle locale) {
        return locale.getString("SYNTAX_MUSIC");
    }

    @Override
    public Collection<CommandPermission> getDefaultRanks(final ResourceBundle locale, final long guildID, final PermissionManager permissions) {
        return List.of(
                new CommandPermission(getCommand(locale), MemberRank.MEMBER, guildID, guildID),
                new CommandPermission(getCommand(locale) + ' ' + locale.getString("ACTION_LIST"), MemberRank.USER, guildID, guildID)
        );
    }

    @Override
    protected void respond(final CommandContext context) {
        final CommandMatcher matcher = context.getMatcher();
        final TextChannel textChannel = matcher.getTextChannel();
        final TranslationCache translationCache = context.getTranslation();
        final ResourceBundle locale = translationCache.getResourceBundle();

        //Get arguments and parse accordingly
        final String[] arguments = matcher.getArguments(1);
        if (arguments.length == 0) {
            textChannel.sendMessage(locale.getString("ERROR_MISSING_OPERATION")).queue();
            return;
        }

        final String action = arguments[0];
        final ActionKey key = translationCache.getActionKey(action);
        switch (key) {
            case PLAY: {
                if (arguments.length < 2) {
                    resumeTrack(textChannel, locale);
                    return;
                }
                final String url = arguments[1];
                loadAndPlay(textChannel, url, locale);
                break;
            }
            case SEARCH: {
                if (arguments.length < 2) {
                    textChannel.sendMessage(locale.getString("MUSIC_SEARCH_QUERY_MISSING")).queue();
                    return;
                }
                final String query = arguments[1];
                searchAndPlay(textChannel, query, locale);
                break;
            }
            case SKIP: {
                if (arguments.length < 2) {
                    skipTrack(textChannel, null, locale);
                    return;
                }
                final String url = arguments[1];
                skipTrack(textChannel, url, locale);
                break;
            }
            case PAUSE: {
                pauseTrack(textChannel, locale);
                break;
            }
            case STOP: {
                stopTrack(textChannel, locale);
                break;
            }
            case LIST: {
                showPlaylist(textChannel, locale);
                break;
            }
            default: {
                textChannel.sendMessage(locale.getString("ERROR_UNKNOWN_OPERATION") + arguments[0]).queue();
            }
        }
    }

    private void loadAndPlay(final TextChannel channel, final String trackUrl, final ResourceBundle locale) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        this.playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(final AudioTrack track) {
                play(channel.getGuild(), musicManager, track);
                final String template = locale.getString("MUSIC_ADDED_SONG");
                channel.sendMessageFormat(template, track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(final AudioPlaylist playlist) {
                //Playlist without track selected
                if (playlist.getSelectedTrack() == null) {
                    for (final AudioTrack tr : playlist.getTracks()) {
                        play(channel.getGuild(), musicManager, tr);
                    }
                    final String template = locale.getString("MUSIC_ADDED_PLAYLIST");
                    channel.sendMessageFormat(template, playlist.getName()).queue();
                    return;
                }
                //Single track from playlist
                trackLoaded(playlist.getSelectedTrack());
            }

            @Override
            public void noMatches() {
                final String template = locale.getString("MUSIC_NOT_FOUND");
                channel.sendMessageFormat(template, trackUrl).queue();
            }

            @Override
            public void loadFailed(final FriendlyException exception) {
                final String template = locale.getString("MUSIC_LOAD_FAILED");
                channel.sendMessageFormat(template, exception.getMessage()).queue();
            }
        });
    }

    private void searchAndPlay(final TextChannel channel, final String queryString, final ResourceBundle locale) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        final String search = "ytsearch:" + queryString;

        this.playerManager.loadItemOrdered(musicManager, search, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(final AudioTrack track) {
                play(channel.getGuild(), musicManager, track);
                final String template = locale.getString("MUSIC_ADDED_SONG");
                channel.sendMessageFormat(template, track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(final AudioPlaylist playlist) {
                final List<AudioTrack> tracks = playlist.getTracks();
                if (tracks.isEmpty()) {
                    noMatches();
                    return;
                }
                final AudioTrack track = tracks.get(0);
                trackLoaded(track);
            }

            @Override
            public void noMatches() {
                final String template = locale.getString("MUSIC_NOT_FOUND");
                channel.sendMessageFormat(template, queryString).queue();
            }

            @Override
            public void loadFailed(final FriendlyException exception) {
                final String template = locale.getString("MUSIC_LOAD_FAILED");
                channel.sendMessageFormat(template, exception.getMessage()).queue();
            }
        });
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(final Guild guild) {
        final long guildId = guild.getIdLong();

        final GuildMusicManager musicManager = this.musicManagers.computeIfAbsent(guildId, (Long id) -> {
            return new GuildMusicManager(this.playerManager, guild.getAudioManager());
        });

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    /**
     * Skip tracks in playlist
     *
     * @param channel Channel to send messages on
     * @param trackUrl URL of the track or playlist to skip, null to skip
     * current track
     * @param locale Locale to send replies in
     */
    private void skipTrack(final TextChannel channel, final String trackUrl, final ResourceBundle locale) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        //Check if the player is playing
        if (musicManager.player.getPlayingTrack() == null) {
            channel.sendMessage(locale.getString("MUSIC_SKIP_NO_TRACK_TO_SKIP")).queue();
            return;
        }

        //No url, skip current track
        if (trackUrl == null) {
            if (musicManager.scheduler.nextTrack()) {
                channel.sendMessage(locale.getString("MUSIC_TRACK_SKIPPED")).queue();
            } else {
                channel.sendMessage(locale.getString("MUSIC_SKIP_PLAYLIST_END")).queue();
            }
            return;
        }

        //Skip tracks from url
        this.playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(final AudioTrack track) {
                if (musicManager.scheduler.skipTrack(track)) {
                    final String template = locale.getString("MUSIC_TRACK_IN_QUEUE_SKIPPED");
                    channel.sendMessageFormat(template, track.getInfo().title).queue();
                } else {
                    channel.sendMessage(locale.getString("MUSIC_SKIP_TRACK_NOT_IN_PLAYLIST")).queue();
                }
            }

            @Override
            public void playlistLoaded(final AudioPlaylist playlist) {
                //Playlist without track selected
                if (playlist.getSelectedTrack() == null) {
                    boolean skipped = false;
                    for (final AudioTrack tr : playlist.getTracks()) {
                        if (musicManager.scheduler.skipTrack(tr)) {
                            skipped = true;
                        }
                    }
                    if (skipped) {
                        final String template = locale.getString("MUSIC_SKIPPED_PLAYLIST");
                        channel.sendMessageFormat(template, playlist.getName()).queue();
                    } else {
                        channel.sendMessage(locale.getString("MUSIC_SKIP_SONGS_NOT_FOUND")).queue();
                    }
                    return;
                }

                //Single track from playlist
                final AudioTrack selectedTrack = playlist.getSelectedTrack();
                final boolean skipped = musicManager.scheduler.skipTrack(selectedTrack);
                if (skipped) {
                    final String template = locale.getString("MUSIC_SKIP_SONG");
                    channel.sendMessageFormat(template, playlist.getName()).queue();
                } else {
                    channel.sendMessage(locale.getString("MUSIC_SKIP_TRACK_NOT_IN_PLAYLIST")).queue();
                }
            }

            @Override
            public void noMatches() {
                final String template = locale.getString("MUSIC_NOT_FOUND");
                channel.sendMessageFormat(template, trackUrl).queue();
            }

            @Override
            public void loadFailed(final FriendlyException exception) {
                final String template = locale.getString("MUSIC_SKIP_FAILED");
                channel.sendMessageFormat(template, exception.getMessage()).queue();
            }
        });
    }

    /**
     * Pause music playback
     *
     * @param textChannel textChannel for request
     * @param locale Locale to respond in
     */
    private void pauseTrack(final TextChannel textChannel, final ResourceBundle locale) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.setPaused(true);
        textChannel.sendMessage(locale.getString("MUSIC_PLAYBACK_PAUSED")).queue();
    }

    /**
     * Resume audio playback
     *
     * @param textChannel channel for the request
     * @param locale Locale to respond in
     */
    private void resumeTrack(final TextChannel textChannel, final ResourceBundle locale) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.setPaused(false);
        textChannel.sendMessage(locale.getString("MUSIC_PLAYBACK_RESUMED")).queue();
    }

    /**
     * Stop audio playback and clear queue
     *
     * @param textChannel channel for request
     * @param locale Locale to respond in
     */
    private void stopTrack(final TextChannel textChannel, final ResourceBundle locale) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.stopTrack();
        musicManager.scheduler.clearPlaylist();
        textChannel.sendMessage(locale.getString("MUSIC_PLAYBACK_STOPPED")).queue();
    }

    /**
     * Print the upcoming songs
     *
     * @param textChannel channel to respond on
     * @param locale Locale to print playlist in
     */
    private void showPlaylist(final TextChannel textChannel, final ResourceBundle locale) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        final List<AudioTrack> playlist = musicManager.scheduler.getPlaylist();
        final EmbedBuilder eb = new EmbedBuilder();

        final AudioTrack currentTrack = musicManager.player.getPlayingTrack();
        if (currentTrack == null) {
            eb.setTitle(locale.getString("MUSIC_PLAYLIST_EMPTY"));
            eb.setDescription(locale.getString("MUSIC_HELP_ADD_MUSIC"));
            textChannel.sendMessageEmbeds(eb.build()).queue();
            return;
        }

        eb.setTitle(locale.getString("MUSIC_CURRENTLY_PLAYING"));
        eb.setDescription(" " + currentTrack.getInfo().title);

        //Get upcoming songs
        final StringBuilder sb = new StringBuilder();
        final int songsToPrint = (Math.min(playlist.size(), 10));
        for (int i = 0; i < songsToPrint; i++) {
            sb.append(playlist.get(i).getInfo().title);
            sb.append('\n');
        }

        //Check if there is any songs in playlist
        if (playlist.isEmpty()) {
            sb.append(locale.getString("MUSIC_END_OF_PLAYLIST"));
        }

        final String fieldName = locale.getString("MUSIC_UPCOMING_SONGS");
        final MessageEmbed.Field upcomingSongsField = new MessageEmbed.Field(fieldName, sb.toString(), false);
        eb.addField(upcomingSongsField);

        long playlistLengthMS = 0;
        for (final AudioTrack t : playlist) {
            playlistLengthMS += t.getDuration();
        }
        final Duration playlistDuration = Duration.ofMillis(playlistLengthMS);
        final long hoursRemaining = playlistDuration.toHours();
        final int minutesPart = playlistDuration.toMinutesPart();
        final int secondsPart = playlistDuration.toSecondsPart();
        final String durationTemplate = locale.getString("MUSIC_DURATION_TEMPLATE");
        final String durationString = String.format(durationTemplate, hoursRemaining, minutesPart, secondsPart);
        final String playlistLength = locale.getString("MUSIC_PLAYLIST_LENGTH");
        final MessageEmbed.Field playlistLengthField = new MessageEmbed.Field(playlistLength, durationString, false);
        eb.addField(playlistLengthField);

        //Send the message
        textChannel.sendMessageEmbeds(eb.build()).queue();
    }

    /**
     * Play AudioTrack
     *
     * @param guild guild to play on
     * @param musicManager musicManager to use
     * @param track AudioTrack to play
     */
    private static void play(final Guild guild, final GuildMusicManager musicManager, final AudioTrack track) {
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

}
