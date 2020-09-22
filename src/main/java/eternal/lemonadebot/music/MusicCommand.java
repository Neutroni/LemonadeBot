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
import eternal.lemonadebot.CommandMatcher;
import eternal.lemonadebot.commandtypes.ChatCommand;
import eternal.lemonadebot.database.ConfigManager;
import eternal.lemonadebot.database.GuildDataStore;
import eternal.lemonadebot.permissions.CommandPermission;
import eternal.lemonadebot.permissions.MemberRank;
import eternal.lemonadebot.translation.ActionKey;
import eternal.lemonadebot.translation.TranslationKey;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
public class MusicCommand implements ChatCommand {

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    /**
     * Constructor
     */
    public MusicCommand() {
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        this.musicManagers = new ConcurrentHashMap<>();
    }

    @Override
    public String getCommand(Locale locale) {
        return TranslationKey.COMMAND_MUSIC.getTranslation(locale);
    }

    @Override
    public String getDescription(Locale locale) {
        return TranslationKey.DESCRIPTION_MUSIC.getTranslation(locale);
    }

    @Override
    public String getHelpText(Locale locale) {
        return TranslationKey.SYNTAX_MUSIC.getTranslation(locale);
    }

    @Override
    public Map<String, CommandPermission> getDefaultRanks(Locale locale, long guildID) {
        return Map.of(
                getCommand(locale),
                new CommandPermission(MemberRank.MEMBER, guildID),
                getCommand(locale) + ' ' + TranslationKey.ACTION_LIST.getTranslation(locale),
                new CommandPermission(MemberRank.USER, guildID)
        );
    }

    @Override
    public void respond(CommandMatcher message, GuildDataStore guildData) {
        final TextChannel textChannel = message.getTextChannel();
        final ConfigManager guildConf = guildData.getConfigManager();
        final Locale locale = guildConf.getLocale();

        //Get arguments and parse accordingly
        final String[] arguments = message.getArguments(2);
        if (arguments.length == 0) {
            textChannel.sendMessage(TranslationKey.ERROR_MISSING_OPERATION.getTranslation(locale)).queue();
            return;
        }

        final String action = arguments[0];
        final ActionKey key = ActionKey.getAction(action, guildConf);
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
                textChannel.sendMessage(TranslationKey.ERROR_UNKNOWN_OPERATION.getTranslation(locale) + arguments[0]).queue();
            }
        }
    }

    private void loadAndPlay(final TextChannel channel, final String trackUrl, final Locale locale) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                play(channel.getGuild(), musicManager, track);
                final String template = TranslationKey.MUSIC_ADDED_SONG.getTranslation(locale);
                channel.sendMessageFormat(template, track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                //Playlist without track selected
                if (playlist.getSelectedTrack() == null) {
                    for (final AudioTrack tr : playlist.getTracks()) {
                        play(channel.getGuild(), musicManager, tr);
                    }
                    final String template = TranslationKey.MUSIC_ADDED_PLAYLIST.getTranslation(locale);
                    channel.sendMessageFormat(template, playlist.getName()).queue();
                    return;
                }
                //Single track from playlist
                trackLoaded(playlist.getSelectedTrack());
            }

            @Override
            public void noMatches() {
                final String template = TranslationKey.MUSIC_NOT_FOUND.getTranslation(locale);
                channel.sendMessageFormat(template, trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                final String template = TranslationKey.MUSIC_LOAD_FAILED.getTranslation(locale);
                channel.sendMessageFormat(template, exception.getMessage()).queue();
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

    /**
     * Skip tracks in playlist
     *
     * @param channel Channel to send messages on
     * @param trackUrl URL of the track or playlist to skip, null to skip
     * current track
     * @param locale Locale to send replies in
     */
    private void skipTrack(final TextChannel channel, final String trackUrl, final Locale locale) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        //Check if the player is playing
        if (musicManager.player.getPlayingTrack() == null) {
            channel.sendMessage(TranslationKey.MUSIC_SKIP_NO_TRACK_TO_SKIP.getTranslation(locale)).queue();
            return;
        }

        //No url, skip current track
        if (trackUrl == null) {
            musicManager.scheduler.nextTrack();
            channel.sendMessage(TranslationKey.MUSIC_TRACK_SKIPPED.getTranslation(locale)).queue();
            return;
        }

        //Skip tracks from url
        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (musicManager.scheduler.skipTrack(track)) {
                    final String template = TranslationKey.MUSIC_TRACK_IN_QUEUE_SKIPPED.getTranslation(locale);
                    channel.sendMessageFormat(template, track.getInfo().title).queue();
                } else {
                    channel.sendMessage(TranslationKey.MUSIC_SKIP_TRACK_NOT_IN_PLAYLIST.getTranslation(locale)).queue();
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
                        final String template = TranslationKey.MUSIC_SKIPPED_PLAYLIST.getTranslation(locale);
                        channel.sendMessageFormat(template, playlist.getName()).queue();
                    } else {
                        channel.sendMessage(TranslationKey.MUSIC_SKIP_SONGS_NOT_FOUND.getTranslation(locale)).queue();
                    }
                    return;
                }

                //Single track from playlist
                final AudioTrack selectedTrack = playlist.getSelectedTrack();
                final boolean skipped = musicManager.scheduler.skipTrack(selectedTrack);
                if (skipped) {
                    final String template = TranslationKey.MUSIC_SKIP_SONG.getTranslation(locale);
                    channel.sendMessageFormat(template, playlist.getName()).queue();
                } else {
                    channel.sendMessage(TranslationKey.MUSIC_SKIP_TRACK_NOT_IN_PLAYLIST.getTranslation(locale)).queue();
                }
            }

            @Override
            public void noMatches() {
                final String template = TranslationKey.MUSIC_NOT_FOUND.getTranslation(locale);
                channel.sendMessageFormat(template, trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                final String template = TranslationKey.MUSIC_SKIP_FAILED.getTranslation(locale);
                channel.sendMessageFormat(template, exception.getMessage()).queue();
            }
        });
    }

    /**
     * Pause music plaback
     *
     * @param textChannel textchannel for reques
     */
    private void pauseTrack(final TextChannel textChannel, final Locale locale) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.setPaused(true);
        textChannel.sendMessage(TranslationKey.MUSIC_PLAYBACK_PAUSED.getTranslation(locale)).queue();
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
    private void resumeTrack(final TextChannel textChannel, final Locale locale) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.setPaused(false);
        textChannel.sendMessage(TranslationKey.MUSIC_PLAYBACK_RESUMED.getTranslation(locale)).queue();
    }

    /**
     * Stop audio playback and clear queue
     *
     * @param textChannel channel for request
     */
    private void stopTrack(final TextChannel textChannel, final Locale locale) {
        GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        musicManager.player.stopTrack();
        musicManager.scheduler.clearPlaylist();
        textChannel.sendMessage(TranslationKey.MUSIC_PLAYBACK_STOPPED.getTranslation(locale)).queue();
    }

    /**
     * Print the upcoming songs
     *
     * @param textChannel channel to respond on
     */
    private void showPlaylist(final TextChannel textChannel, final Locale locale) {
        final GuildMusicManager musicManager = getGuildAudioPlayer(textChannel.getGuild());
        final List<AudioTrack> playlist = musicManager.scheduler.getPlaylist();
        final EmbedBuilder eb = new EmbedBuilder();

        final AudioTrack currentTrack = musicManager.player.getPlayingTrack();
        if (currentTrack == null) {
            eb.setTitle(TranslationKey.MUSIC_PLAYLIST_EMPTY.getTranslation(locale));
            eb.setDescription(TranslationKey.MUSIC_HELP_ADD_MUSIC.getTranslation(locale));
            textChannel.sendMessage(eb.build()).queue();
            return;
        }

        eb.setTitle(TranslationKey.MUSIC_CURRENTLY_PLAYING.getTranslation(locale));
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
            sb.append(TranslationKey.MUSIC_END_OF_PLAYLIST.getTranslation(locale));
        }

        final String fieldName = TranslationKey.MUSIC_UPCOMING_SONGS.getTranslation(locale);
        final MessageEmbed.Field upcomingSongsField = new MessageEmbed.Field(fieldName, sb.toString(), false);
        eb.addField(upcomingSongsField);

        long playlistLengthMS = 0;
        for (final AudioTrack t : playlist) {
            playlistLengthMS += t.getDuration();
        }
        Duration playlistDuration = Duration.ofMillis(playlistLengthMS);
        final long hoursRemaining = playlistDuration.toHours();
        final int minutesPart = playlistDuration.toMinutesPart();
        final int secondsPart = playlistDuration.toSecondsPart();
        final String durationTemplate = TranslationKey.MUSIC_DURATION_TEMPLATE.getTranslation(locale);
        final String durationString = String.format(durationTemplate, hoursRemaining, minutesPart, secondsPart);
        final String playlistLenth = TranslationKey.MUSIC_PLAYLIST_LENGTH.getTranslation(locale);
        MessageEmbed.Field playlistLenghtField = new MessageEmbed.Field(playlistLenth, durationString, false);
        eb.addField(playlistLenghtField);

        //Send the message
        textChannel.sendMessage(eb.build()).queue();
    }

}
