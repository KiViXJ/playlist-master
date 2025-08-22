package com.example.playlistmaster

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.util.ArrayList
import kotlin.collections.count
import kotlin.collections.get
import kotlin.collections.listOf
import kotlin.reflect.typeOf

class PlaylistActivity : AppCompatActivity() {

    val mediaPlayer = MediaPlayer()
    lateinit var currentPlaylist: File
    val audio = mutableListOf<File>()
    var currentAudioIndex: Int = 0

    fun playPlaylist() {

        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()

        try {

            val tryNextAudio = audio.getOrNull(1)
            val nextSong = tryNextAudio?.nameWithoutExtension ?: "None"
            // Set data source to the first song in the playlist
            mediaPlayer.setDataSource(audio[currentAudioIndex].absolutePath)

            mediaPlayer.setOnCompletionListener {
                val nextAudio = audio.getOrNull(currentAudioIndex+1)
                if (nextAudio != null) {
                    playNextSong()
                } else {
                    makeToast("Finished playlist")
                    findViewById<ImageView>(R.id.currentSongStop).callOnClick()
                }
            }

            mediaPlayer.prepare()

            val playbackParams = PlaybackParams()
            playbackParams.speed = 1f

            try {
                mediaPlayer.playbackParams = playbackParams
                mediaPlayer.setVolume(1f, 1f)
            }
            catch (e: Exception) { makeToast("${e.message.toString()}, $e") }

            mediaPlayer.start()

            editCurrentSongView(currentPlaylist, audio[0].name, nextSong)

        } catch (e: Exception) {
            makeToast("Error playing playlist: ${e.message}")
        }
    }
    fun playNextSong()  {
        makeToast("Playing next song (${audio[currentAudioIndex + 1].nameWithoutExtension})")
        currentAudioIndex += 1
        val tryNextAudio = audio.getOrNull(currentAudioIndex+1)
        val nextSong = if (tryNextAudio != null) tryNextAudio.name else "None"

        mediaPlayer.reset()
        mediaPlayer.setDataSource(audio[currentAudioIndex].absolutePath)
        mediaPlayer.prepare()
        mediaPlayer.start()
        editCurrentSongView(currentPlaylist, audio[currentAudioIndex].name, nextSong)
    }
    fun playPreviousSong()  {
        makeToast("Playing previous song (${audio[currentAudioIndex - 1].nameWithoutExtension})")
        val nextSong = audio[currentAudioIndex].name
        currentAudioIndex -= 1
        mediaPlayer.reset()
        mediaPlayer.setDataSource(audio[currentAudioIndex].absolutePath)
        mediaPlayer.prepare()
        mediaPlayer.start()
        editCurrentSongView(currentPlaylist, audio[currentAudioIndex].name, nextSong)
    }
    fun pauseSong() {
        makeToast("Playback paused")
        findViewById<ImageView>(R.id.currentSongPause).setImageDrawable(resources.getDrawable(R.drawable.play))
        mediaPlayer.pause()
    }
    fun unpauseSong() {
        findViewById<ImageView>(R.id.currentSongPause).setImageDrawable(resources.getDrawable(R.drawable.pause))
        mediaPlayer.start()
    }
    fun slowSongDown() {
        if (!mediaPlayer.isPlaying) {
            unpauseSong()
        }

        if (mediaPlayer.playbackParams.speed != 0.25f) {

            val params = mediaPlayer.playbackParams
            makeToast(params.speed.toString())
            params.speed = params.speed - 0.25f // limit max speed if desired
            mediaPlayer.playbackParams = params

            makeToast("Speed changed to ${mediaPlayer.playbackParams.speed}")
        }
    }
    fun speedSongUp() {
        if (!mediaPlayer.isPlaying) {
            unpauseSong()
        }

        if (mediaPlayer.playbackParams.speed != 1.25f) {

            val params = mediaPlayer.playbackParams
            makeToast(params.speed.toString())
            params.speed = params.speed + 0.25f // limit max speed if desired
            mediaPlayer.playbackParams = params

            makeToast("Speed changed to ${mediaPlayer.playbackParams.speed}")
        }
    }
    fun fastForward() {
        makeToast("Fast-forwarded 10 seconds")

        if (mediaPlayer.isPlaying) { unpauseSong() }

        val newPosition = mediaPlayer.currentPosition + 10000
        mediaPlayer.seekTo(newPosition)

    }
    fun rewind() {
        makeToast("Rewound 10 seconds")

        if (mediaPlayer.isPlaying) { unpauseSong() }

        val newPosition = mediaPlayer.currentPosition - 10000
        mediaPlayer.seekTo(newPosition)

    }
    fun editCurrentSongView(playlist: File, song: String, nextSong: String) {
        try {
            val containerLayout = findViewById<LinearLayout>(R.id.currentSongContainer)

            containerLayout.findViewById<TextView>(R.id.currentSong).text =
                if (song.length < 12) song else song.substring(0, 12) + "..."
            containerLayout.findViewById<TextView>(R.id.nextSong).text =
                if (nextSong.length < 12) nextSong else nextSong.substring(0, 12) + "..."
            containerLayout.findViewById<TextView>(R.id.currentPlaylist).text =
                if (playlist.name.length < 12) playlist.name else playlist.name.substring(0, 12) + "..."


        } catch (e: Exception) {
            makeToast(e.message.toString())
        }
    }
    fun clickEffect(image: ImageView) {
        image.setColorFilter("#0000FF".toColorInt(), PorterDuff.Mode.SRC_ATOP)

        val animator = ValueAnimator.ofFloat(1f, 0f)
        animator.duration = 1000 // 1 second

        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float

            val alpha = (progress * 128).toInt()
            val color = Color.argb(alpha, 255, 0, 0) // red with varying transparency
            image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }

        animator.start()
    }
    fun addClickListeners() {
        try {
            findViewById<ImageView>(R.id.currentSongStop).setOnClickListener {
                startActivity(Intent(this, MyPlaylistsActivity::class.java))
                mediaPlayer.release()
            }
            findViewById<ImageView>(R.id.currentSongPause).setOnClickListener {
                clickEffect(it as ImageView)
                if (mediaPlayer.isPlaying) {
                    makeToast("Playback paused")
                    pauseSong()
                } else {
                    makeToast("Playback resumed")
                    unpauseSong()
                }
            }
            findViewById<ImageView>(R.id.playNextSong).setOnClickListener {

                if (mediaPlayer.isPlaying) {
                    unpauseSong()
                }
                if (currentAudioIndex < (audio.count() - 1)) {
                    clickEffect(it as ImageView)
                    playNextSong()
                }
            }
            findViewById<ImageView>(R.id.playPrevSong).setOnClickListener {

                if (mediaPlayer.isPlaying) {
                    unpauseSong()
                }
                if (currentAudioIndex > 0) {
                    clickEffect(it as ImageView)
                    playPreviousSong()
                }
            }
            findViewById<ImageView>(R.id.speedUp).setOnClickListener {
                try {
                    clickEffect(it as ImageView)
                    speedSongUp()
                } catch (e: Exception) {
                    makeToast(e.message.toString())
                }
            }
            findViewById<ImageView>(R.id.slowDown).setOnClickListener {
                try {
                    clickEffect(it as ImageView)
                    slowSongDown()
                } catch (e: Exception) {
                    makeToast(e.message.toString())
                }
            }
            findViewById<ImageView>(R.id.fastForward).setOnClickListener {
                clickEffect(it as ImageView)
                fastForward()
            }
            findViewById<ImageView>(R.id.rewind).setOnClickListener {
                clickEffect(it as ImageView)
                rewind()
            }
        }
        catch (e: Exception) { makeToast(e.message.toString()) }
    }
    fun makeToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_playlist)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        currentPlaylist = intent.getSerializableExtra("playlist") as File
        val serializable = intent.getSerializableExtra("audio")!!
        if (serializable is ArrayList<*>) {
            serializable.forEach {
                audio.add(it as File)
            }
        }

        addClickListeners()
        playPlaylist()

    }
    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
    }
}