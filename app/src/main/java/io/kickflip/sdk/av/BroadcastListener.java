package io.kickflip.sdk.av;

import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.exception.KickflipException;

/**
 * Provides callbacks for the major lifecycle benchmarks of a Broadcast.
 */
public interface BroadcastListener {
    /**
     * The broadcast has started, and is currently buffering.
     */
    public void onBroadcastStart();

    /**
     * The first video segment and m3u8 file was succesfully uploaded to the server, since now the watchers can watch the stream
     *
     * @param stream the {@link io.kickflip.sdk.api.json.Stream} representing this broadcast.
     */
    public void onBroadcastStreaming(Stream stream, int currentBitrate);

    /**
     * The broadcast thumbnail was succesfully uploaded to the server
     *
     * @param stream the {@link io.kickflip.sdk.api.json.Stream} representing this broadcast.
     */
    public void onBroadcastReady(Stream stream);

    /**
     * The broadcast has ended.
     */
    public void onBroadcastStop();

    /**
     * An error occurred.
     */
    public void onBroadcastError(KickflipException error);

    /**
     * Event when the upload of the stream to the server is failing, for any kind of reasons (e.g. no internet access)
     * @param networkIssuesPresent
     */
    public void onTryingToReconnect(final boolean networkIssuesPresent);

    /**
     * Event triggered when the network speed is poor, so the quality of streaming is questionable
     */
    public void onLowUploadBitrateDetected();

    public void onBitrateChange(final int newVideoBitrate, final int bandwidth, boolean bitrateDynamicallyUpdated, int pendingItemsCount);

}
