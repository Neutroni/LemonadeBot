/*
 * Apache-2-0 Licenced from https://github.com/sedmelluq/lavaplayer
 */
package eternal.lemonadebot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.managers.AudioManager;

/**
 *
 * @author Neutroni
 */
class GuildMusicManager {

    /**
     * Audio player for the guild.
     */
    public final AudioPlayer player;
    /**
     * Track scheduler for the player.
     */
    public final TrackScheduler scheduler;

    /**
     * Creates a player and a track scheduler.
     *
     * @param manager Audio player manager to use for creating the player.
     * @param audioManager Audiomanager to close at the end of play
     */
    GuildMusicManager(AudioPlayerManager manager, AudioManager audioManager) {
        player = manager.createPlayer();
        scheduler = new TrackScheduler(player, audioManager);
        player.addListener(scheduler);
    }

    /**
     * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
     */
    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }
}
