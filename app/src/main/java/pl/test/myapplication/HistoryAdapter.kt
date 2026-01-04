package pl.test.myapplication

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.test.myapplication.data.ResultEntity
import pl.test.myapplication.databinding.ItemHistoryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<ResultEntity, HistoryAdapter.VH>(DIFF){
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ResultEntity>() {
            override fun areItemsTheSame(oldItem: ResultEntity, newItem: ResultEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ResultEntity, newItem: ResultEntity): Boolean =
                oldItem == newItem
        }
    }

    private val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root) {

        @SuppressLint("SetTextI18n")
        fun bind(e: ResultEntity) {
            // Data
            b.txtDate.text = df.format(Date(e.timestamp))

            // P(melanoma)
            val pMelanoma = e.pMelanoma.coerceIn(0f, 1f)
            if(pMelanoma >= 0.5) {
                b.txtProb.text = "Prawdopodobieństwo: ${((pMelanoma * 100f).toInt())}%"
            } else {
                b.txtProb.text = "Prawdopodobieństwo: ${(100 - (pMelanoma * 100f).toInt())}%"
            }
            // Etykieta + kolor
            val labelText = when {
                e.uncertain -> "NIEPEWNY"
                pMelanoma >= 0.5f -> "CZERNIAK"
                else -> "ZMIANA ŁAGODNA"
            }
            b.txtResult.text = "Wynik: $labelText"

            b.txtResult.setTextColor(
                when {
                    e.uncertain -> Color.rgb(255, 193, 7)
                    pMelanoma >= 0.5f -> Color.RED
                    else -> Color.rgb(0, 150, 0)
                }
            )

            // Miniatura z pliku imageUri trzyma ścieżke
            val path = e.imagePath
            val fileOk = !path.isNullOrBlank() && File(path).exists()

            if (fileOk) {
                // Dekodowanie "na lekko" – zdjęcia zapisujemy już w rozsądnym rozmiarze w ImageStore,
                // więc decodeFile zwykle wystarcza bez kombinowania.
                b.imgThumb.setImageBitmap(BitmapFactory.decodeFile(path))
            } else {
                b.imgThumb.setImageDrawable(null)
            }
        }
    }
}