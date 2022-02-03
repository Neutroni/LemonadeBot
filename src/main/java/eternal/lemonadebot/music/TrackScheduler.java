/*
 * Apache-2-0 Licenced from https://github.com/sedmelluq/lavaplayer
 */
package eternal.lemonadebot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neutroni
 */
class TrackScheduler extends AudioEventAdapter {
    private static final Logger LOGGER = LogManager.getLogger();

    private final AudioPlayer player;
    private final AudioManager manager;
    private final BlockingQueue<AudioTrack> queue;

    /**
     * @param player The audio player this scheduler uses
     * @param manager Manager to stop at the end of playlist
     */
    TrackScheduler(final AudioPlayer player, final AudioManager manager) {
        this.player = player;
        this.manager = manager;
        this.queue = new LinkedBlockingQueue<>();
    }

    /**
     * Add the next track to queue or play right away if nothing is in the
     * queue.
     *
     * @param track The track to play or add to queue.
     */
    public boolean queue(final AudioTrack track) {
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        if (!this.player.startTrack(track, true)) {
            //Return if track can be queued
            return this.queue.offer(track);
        }
        //Track playback started successfully
        return true;
    }

    /**
     *
     * Start the next track, stopping the current one if it is playing.
     *
     * @return true if there is next track to skip to, false if end of playlist.
     */
    public boolean nextTrack() {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        final AudioTrack track = this.queue.poll();
        this.player.startTrack(track, false);
        if (track == null) {
            this.manager.getJDA().getPresence().setActivity(null);
            this.manager.closeAudioConnection();
        }
        return (track != null);
    }

    /**
     * Called when a track playback starts
     *
     * @param player player that started
     * @param track track that was started
     */
    @Override
    public void onTrackStart(final AudioPlayer player, final AudioTrack track) {
        final Activity status = Activity.listening(track.getInfo().title);
        this.manager.getJDA().getPresence().setActivity(status);
        super.onTrackStart(player, track);

    }

    /**
     * Called when track ends
     *
     * @param player player
     * @param track track that ended
     * @param endReason why end
     */
    @Override
    public void onTrackEnd(final AudioPlayer player, final AudioTrack track, final AudioTrackEndReason endReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (endReason.mayStartNext) {
            nextTrack();
        }
    }

    /**
     * Clear the playlist and disconnect from voice
     */
    void clearPlaylist() {
        this.queue.clear();
        this.manager.getJDA().getPresence().setActivity(null);
        this.manager.closeAudioConnection();
    }

    /**
     * Remove track from song queue
     *
     * @param track Track to remove
     * @return true if removed
     */
    boolean skipTrack(final AudioTrack track) {
        return this.queue.removeIf(t -> t.getIdentifier().equals(track.getIdentifier()));
    }

    /**
     * Get the current playlist
     *
     * @return Collection of AudioTracks
     */
    List<AudioTrack> getPlaylist() {
        return new ArrayList<>(this.queue);
    }
}
