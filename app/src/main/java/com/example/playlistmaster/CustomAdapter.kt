
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.playlistmaster.PlaylistSettingsActivity
import com.example.playlistmaster.R
import java.io.File


class CustomAdapter(
    private val context: PlaylistSettingsActivity,
    private val audioFiles: MutableList<File>
) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    val viewHolders = mutableListOf<ViewHolder>()
    // ViewHolder class
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val audioTextView: TextView = view.findViewById(R.id.audioNameM)
        val deleteImageView: ImageView = view.findViewById(R.id.deleteAudioM)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.text_row_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = audioFiles.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = audioFiles[position]
        holder.audioTextView.text = file.name

        holder.deleteImageView.setOnClickListener {
            if (audioFiles.size > 1) {
                val index = audioFiles.indexOf(file)
                audioFiles.remove(file)
                context.deleteUnfinished(file.name)
                notifyItemRemoved(index)
            }
        }
    }

    fun addAudio(file: File, uri: Uri) {
        audioFiles.add(file)
        context.writeUnfinished(uri, file.name)
        notifyItemInserted(audioFiles.size)
    }
}