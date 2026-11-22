package io.github.lorber13.jellyfinmusic.core.playback

import android.app.PendingIntent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import io.github.lorber13.jellyfinmusic.core.data.stream.StreamRepository
import javax.inject.Inject

/**
 * Plays music in the background and publishes it as a media session, which gives the media
 * notification and the lock-screen controls and lets Bluetooth devices control playback.
 * Tapping the notification or the lock-screen controls opens the app. The app sends commands
 * through [MediaControllerMusicPlayer].
 *
 * Media3's defaults cover the rest: the player holds a wake lock while it plays with the screen
 * off; swiping the app away from Recents keeps the music playing, or stops the service if it is
 * paused; and the service leaves the foreground after 10 minutes paused.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject
    lateinit var streams: StreamRepository

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        // Hilt injects the fields here, so the player is built after it.
        super.onCreate()
        useAppNotificationIcon()
        val sessionBuilder = MediaSession.Builder(this, buildPlayer())
            .setCallback(TrackStreamCallback(streams))
        openAppIntent()?.let(sessionBuilder::setSessionActivity)
        mediaSession = sessionBuilder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /**
     * What tapping the notification or the lock-screen controls opens: the app's launch intent,
     * which brings the app back to the screen the user left and keeps this module independent of
     * the app's activity. Null only without a launcher activity, as in a library's test APK.
     */
    private fun openAppIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    // Opts in to Media3's unstable API for DefaultMediaNotificationProvider only; it is the only
    // way to replace Media3's generic icon in the status bar.
    @OptIn(UnstableApi::class)
    private fun useAppNotificationIcon() {
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this).build()
        notificationProvider.setSmallIcon(R.drawable.playback_ic_notification)
        setMediaNotificationProvider(notificationProvider)
    }

    // Opts in to Media3's unstable API for setUsePlatformDiagnostics only.
    @OptIn(UnstableApi::class)
    private fun buildPlayer(): ExoPlayer {
        val musicAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        return ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(authorizedDataSourceFactory()))
            // With audio focus handled (true), starting our music pauses other music apps, and
            // theirs pauses ours. The system lowers our volume during short sounds such as
            // navigation prompts.
            .setAudioAttributes(musicAttributes, true)
            // Otherwise playback events go to the platform's media metrics, which Google may
            // collect; the app has no telemetry.
            .setUsePlatformDiagnostics(false)
            .build()
    }

    /**
     * Adds the signed-in user's credentials to every stream request, read anew each time so a
     * new sign-in applies. Media3 only offers per-request headers through its unstable
     * ResolvingDataSource. DefaultDataSource keeps local files and content URIs working.
     */
    @OptIn(UnstableApi::class)
    private fun authorizedDataSourceFactory(): DataSource.Factory {
        val authorizedHttpFactory = ResolvingDataSource.Factory(DefaultHttpDataSource.Factory()) { dataSpec ->
            dataSpec.withAdditionalHeaders(streams.requestHeaders())
        }
        return DefaultDataSource.Factory(this, authorizedHttpFactory)
    }
}
