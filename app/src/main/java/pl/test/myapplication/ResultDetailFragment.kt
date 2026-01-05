package pl.test.myapplication

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import pl.test.myapplication.data.ResultRepository
import pl.test.myapplication.databinding.FragmentResultDetailBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultDetailFragment : Fragment() {

    private var _binding: FragmentResultDetailBinding? = null
    private val binding get() = _binding!!
    private val repo by lazy { ResultRepository(requireContext()) }
    private val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResultDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val resultId = requireArguments().getLong("resultId", -1L)
        if (resultId <= 0L) {
            Toast.makeText(requireContext(), "Brak ID wyniku", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // Wczytaj dane
        viewLifecycleOwner.lifecycleScope.launch {
            val entity = repo.getById(resultId)
            if (entity == null) {
                Toast.makeText(requireContext(), "Nie znaleziono wpisu", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return@launch
            }

            // Data
            binding.txtDetailDate.text = df.format(Date(entity.timestamp))

            // Obraz
            val path = entity.imagePath
            val ok = !path.isNullOrBlank() && File(path).exists()
            if (ok) {
                binding.imgDetail.setImageBitmap(BitmapFactory.decodeFile(path))
            } else {
                binding.imgDetail.setImageDrawable(null)
            }

            // Wynik + kolor (spójnie z AnalyzeFragment)
            val p = entity.pMelanoma.coerceIn(0f, 1f)
            val uncertain = entity.uncertain

            val labelText = when {
                uncertain -> "NIEPEWNY"
                p >= 0.5f -> "CZERNIAK"
                else -> "ZMIANA ŁAGODNA"
            }

            binding.txtDetailResult.text = "Wynik: $labelText"
            if(p >= 0.5) {
                binding.txtDetailProb.text = "Prawdopodobieństwo: ${((p * 100f).toInt())}%"
            } else {
                binding.txtDetailProb.text = "Prawdopodobieństwo: ${(100 - (p * 100f).toInt())}%"
            }

            binding.txtDetailResult.setTextColor(
                when {
                    uncertain -> Color.rgb(255, 193, 7)
                    p >= 0.5f -> Color.RED
                    else -> Color.rgb(0, 150, 0)
                }
            )

            // Usuń wpis (potwierdzenie)
            binding.btnDeleteOne.setOnClickListener {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Usunąć wpis?")
                    .setMessage("Ten wpis oraz zapisane zdjęcie zostaną usunięte lokalnie.")
                    .setPositiveButton("Usuń") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            repo.deleteById(resultId)
                            Toast.makeText(requireContext(), "Usunięto wpis", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                    }
                    .setNegativeButton("Anuluj") { _, _ -> }
                    .show()
            }

            binding.btnBackToHistory.setOnClickListener{
                findNavController().popBackStack(R.id.historyFragment, false)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
