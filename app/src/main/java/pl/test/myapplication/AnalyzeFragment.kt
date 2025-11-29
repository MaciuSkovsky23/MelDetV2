package pl.test.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.text.DecimalFormat
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import pl.test.myapplication.databinding.FragmentAnalyzeBinding
import pl.test.myapplication.ml.NewTestV1
import pl.test.myapplication.ml.NewTestV6
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

private const val MODEL_NAME = "NewTestV6"
class AnalyzeFragment : Fragment() {

    private var _binding: FragmentAnalyzeBinding? = null
    private val binding get() = _binding!!

    private lateinit var camera: Button
    private lateinit var gallery: Button
    private lateinit var imageView: ImageView
    private lateinit var result: TextView
    private lateinit var probabilityText: TextView
    private lateinit var info: Button
    private lateinit var classified: TextView

    private val imageSize = 224
    private val classes = arrayOf("Czerniak", "Zmiana łagodna")
    private val decimalFormat = DecimalFormat("#.##")

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data = result.data
                val image = data?.extras?.get("data") as? Bitmap
                image?.let { processImage(it) }
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data = result.data
                val uri = data?.data
                uri?.let { processGalleryImage(it) }
            }
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyzeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        camera = binding.btnCamera
        gallery = binding.btnGallery
        result = binding.result
        imageView = binding.imageView
        probabilityText = binding.probabilityText
        info = binding.btnInfo
        classified = binding.classified
    }

    private fun processImage(image: Bitmap) {
        val dimension = minOf(image.width, image.height)
        val thumbnail = ThumbnailUtils.extractThumbnail(image, dimension, dimension)
        imageView.setImageBitmap(thumbnail)

        val scaledImage = thumbnail.scale(imageSize, imageSize)
        classifyImage(scaledImage)
    }

    private fun processGalleryImage(uri: Uri) {
        try {
            val image = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            imageView.setImageBitmap(image)

            val scaledImage = image.scale(imageSize, imageSize)
            classifyImage(scaledImage)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    private fun setupClickListeners() {
        camera.setOnClickListener {
            if (requireContext().checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                permissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
        gallery.setOnClickListener {
            val galleryIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(galleryIntent)
        }
        info.setOnClickListener {
            showInfoDialog()
        }
    }

    private fun classifyImage(image: Bitmap) {
        try {
            val model = NewTestV6.newInstance(requireContext())

            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3).apply {
                order(ByteOrder.nativeOrder())
            }

            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)

            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val value = intValues[pixel++]
                    byteBuffer.putFloat(((value shr 16) and 0xFF) * (1f / 255f))
                    byteBuffer.putFloat(((value shr 8) and 0xFF) * (1f / 255f))
                    byteBuffer.putFloat(((value) and 0xFF) * (1f / 255f))
                }
            }

            inputFeature0.loadBuffer(byteBuffer)

            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            val confidences = outputFeature0.floatArray
            val probabilities = softmax(confidences)

            val maxPos = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0

            displayResults(maxPos, probabilities)

            model.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val expValues = FloatArray(logits.size)
        var sum = 0f

        for (i in logits.indices) {
            expValues[i] = exp(logits[i].toDouble()).toFloat()
            sum += expValues[i]
        }

        for (i in expValues.indices) {
            expValues[i] = expValues[i] / sum
        }

        return expValues
    }

    @SuppressLint("SetTextI18n")
    private fun displayResults(predictedClass: Int, probabilities: FloatArray) {
        classified.setTextColor(resources.getColor(android.R.color.black, null))
        result.text = "Predykcja: ${classes[predictedClass]}"

        val probabilityString = buildString {
            append("Prawdopodobieństwo:\n")
            for (i in classes.indices) {
                val percentage = probabilities[i] * 100
                append("${classes[i]}: ${decimalFormat.format(percentage)}%\n")
            }
        }

        probabilityText.text = probabilityString

        when (predictedClass) {
            0 -> {
                result.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            }
            1 -> {
                result.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
        }
    }

    private fun showInfoDialog() {
        val infoMessage = """
            Ta aplikacja używa sztucznej inteligencji do analizy zmian skórnych w celu wykrycia potencjalnego czerniaka.
            
            📋 Jak używać:
            1. Wybierz opcję "Zrób zdjęcie"
            2. Upewnij się, że masz dobre oświetlenie
            3. Trzymaj aparat nieruchomo
            4. Zrób wyraźne zdjęcie zmiany skórnej
            5. Odczytaj wynik analizy
            
            LUB
            
            1. Wybierz opcję "Wczytaj z galerii"
            2. Wybierz z galerii odpowiednie zdjęcie
            3. Odczytaj wynik analizy
            
            Dla dokładniejszych wyników:
            - Używaj zdjęć dobrej jakości
            - Uważaj na cień i/lub blask 
            - Upewnij się, że na zdjęciu zamieszczona jest cała zmiana
            
            ⚠️ Ważne:
            - To nie jest profesjonalna diagnoza medyczna
            - W przypadku wątpliwości skonsultuj się z dermatologiem
            - Regularnie obserwuj skórę i monitoruj wszelkie zmiany skórne
        """.trimIndent()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Informacje")
            .setMessage(infoMessage)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}