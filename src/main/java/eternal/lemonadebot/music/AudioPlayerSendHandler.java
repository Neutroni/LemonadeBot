/*
 * Apache-2-0 Licenced from https://github.com/sedmelluq/lavaplayer
 */
package eternal.lemonadebot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import java.nio.ByteBuffer;
import net.dv8tion.jda.api.audio.AudioSendHandler;

/**
 *
 * @author Neutroni
 */
class AudioPlayerSendHandler implements AudioSendHandler {

    private final AudioPlayer audioPlayer;
    private final ByteBuffer buffer;
    private final MutableAudioFrame frame;

    /**
     * Constructor
     *
     * @param audioPlayer Audio player to wrap.
     */
    AudioPlayerSendHandler(final AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        this.buffer = ByteBuffer.allocate(1024);
        this.frame = new MutableAudioFrame();
        this.frame.setBuffer(this.buffer);
    }

    /**
     * Check if audio was provided
     *
     * @return True if audio was provided
     */
    @Override
    public boolean canProvide() {
        return this.audioPlayer.provide(this.frame);
    }

    /**
     * Buffer management
     *
     * @return Audio Buffer
     */
    @Override
    public ByteBuffer provide20MsAudio() {
        // flip to make it a read buffer
        this.buffer.flip();
        return this.buffer;
    }

    /**
     * Check if codec is Opus
     *
     * @return True if codec is Opus
     */
    @Override
    public boolean isOpus() {
        return true;
    }
}
