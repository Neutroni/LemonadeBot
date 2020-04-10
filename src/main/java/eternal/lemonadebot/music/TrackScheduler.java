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

/**
 *
 * @author Neutroni
 */
class TrackScheduler extends AudioEventAdapter {

    private final AudioPlayer player;
    private final AudioManager manager;
    private final BlockingQueue<AudioTrack> queue;

    /**
     * @param player The audio player this scheduler uses
     * @param manager Manager to stop at the end of playlist
     */
    TrackScheduler(AudioPlayer player, AudioManager manager) {
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
    public void queue(AudioTrack track) {
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    public void nextTrack() {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        final AudioTrack track = queue.poll();
        player.startTrack(track, false);
        if (track == null) {
            this.manager.getJDA().getPresence().setActivity(null);
            this.manager.closeAudioConnection();
        }
    }

    /**
     *
     * @param player
     * @param track
     */
    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
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
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
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
    boolean skipTrack(AudioTrack track) {
        return this.queue.removeIf(t -> t.getIdentifier().equals(track.getIdentifier()));
    }

    /**
     * Get the current playlist
     *
     * @return Collection of AudioTracks
     */
    List<AudioTrack> getPlaylist() {
        return new ArrayList<>(queue);
    }
}
