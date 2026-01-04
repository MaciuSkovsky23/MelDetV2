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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import pl.test.myapplication.data.ResultRepository
import pl.test.myapplication.databinding.FragmentAnalyzeBinding
import pl.test.myapplication.ml.BestByValAucGptest4
//import pl.test.myapplication.ml.NewTestV1
//import pl.test.myapplication.ml.NewTestV6
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AnalyzeFragment : Fragment() {

    private var _binding: FragmentAnalyzeBinding? = null
    private val binding get() = _binding!!

    private lateinit var camera: Button
    private lateinit var gallery: Button
    private lateinit var imageView: ImageView
    private lateinit var result: TextView
    private lateinit var probabilityText: TextView
    private lateinit var guideText: TextView
    private lateinit var info: Button
    private lateinit var classified: TextView
    private lateinit var saveResult: Button
    private lateinit var openHistory: Button
    private val imageSize = 224
    private val classes = arrayOf("Zmiana łagodna", "Czerniak")
    private val decimalFormat = DecimalFormat("#.##")

    private var lastPMelanoma: Float? = null
    private var lastPNevus: Float? = null
    private var lastLabel: String? = null
    private var lastIsUncertain: Boolean? = null
    private var lastImage: Bitmap? = null

    private val repo by lazy {ResultRepository(requireContext())}

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
        guideText = binding.guideText
        info = binding.btnInfo
        classified = binding.classified
        saveResult = binding.btnSaveResult
        openHistory = binding.btnHistory
    }

    private fun processImage(image: Bitmap) {
        val dimension = minOf(image.width, image.height)
        val thumbnail = ThumbnailUtils.extractThumbnail(image, dimension, dimension)

        lastImage = thumbnail

        imageView.setImageBitmap(thumbnail)

        val scaledImage = thumbnail.scale(imageSize, imageSize)
        classifyImage(scaledImage)
    }

    private fun processGalleryImage(uri: Uri) {
        try {
            val image = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)

            val dimension = minOf(image.width, image.height)
            val thumbnail = ThumbnailUtils.extractThumbnail(image, dimension, dimension)

            lastImage = thumbnail

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
        saveResult.setOnClickListener {
            val img = lastImage ?: return@setOnClickListener
            val percentage = lastPMelanoma ?: return@setOnClickListener

            viewLifecycleOwner.lifecycleScope.launch{
                repo.saveResult(img, percentage)
                Toast.makeText(requireContext(), "Zapisano", Toast.LENGTH_SHORT).show()
                saveResult.isEnabled = false
            }
        }
        openHistory.setOnClickListener {
            findNavController().navigate(R.id.action_analyzeFragment_to_historyFragment)
        }
    }

    private fun classifyImage(image: Bitmap) {

        guideText.visibility = View.INVISIBLE
        print(guideText.visibility)
        try {
//            val model = NewTestV6.newInstance(requireContext())
            val model = BestByValAucGptest4.newInstance(requireContext())

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
//                    byteBuffer.putFloat(((value shr 16) and 0xFF) * (1f / 255f))
//                    byteBuffer.putFloat(((value shr 8) and 0xFF) * (1f / 255f))
//                    byteBuffer.putFloat(((value) and 0xFF) * (1f / 255f))

                    byteBuffer.putFloat((((value shr 16) and 0xFF) / 127.5f) - 1f)
                    byteBuffer.putFloat((((value shr 8) and 0xFF) / 127.5f) - 1f)
                    byteBuffer.putFloat((((value) and 0xFF) / 127.5f) - 1f)
                }
            }

            inputFeature0.loadBuffer(byteBuffer)

            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

//            val confidences = outputFeature0.floatArray
//            val probabilities = softmax(confidences)
//
//            val maxPos = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
//
//            displayResults(maxPos, probabilities)

            val output = outputFeature0.floatArray
            val pMelanoma = output[0].coerceIn(0f, 1f)

// dla UI dwie wartości P(nevus), P(melanoma)
            val probabilities = floatArrayOf(
                1f - pMelanoma,  // klasa 0: nevus
                pMelanoma        // klasa 1: melanoma
            )

// standardowy próg 0.5 do klasy
            val predictedClass = if (pMelanoma >= 0.5f) 1 else 0

            displayResults(predictedClass, probabilities)

            lastPMelanoma = pMelanoma
            lastPNevus = 1f - pMelanoma
            lastIsUncertain = pMelanoma in 0.4f..0.6f
            lastLabel = when {
                lastIsUncertain == true -> "NIEPEWNY"
                pMelanoma >= 0.5f -> "CZERNIAK"
                else -> "ZMIANA ŁAGODNA"
            }

            saveResult.isEnabled = true

            model.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayResults(predictedClass: Int, probabilities: FloatArray) {
        val pMelanoma = probabilities[1]
        val melanomaPercentage = pMelanoma*100f

//      wynik między 40%-60% jako niepewny
        val notSure = pMelanoma in 0.4f..0.6f

        result.text = if (notSure){
            "Predykcja: NIEPEWNY"
        }else{
            "Predykcja: ${classes[predictedClass]}"
        }

//      wypisanie prawdopodobienstwa
        val probabilityString = buildString {
            append("Prawdopodobieństwo:\n")
            for(i in classes.indices){
                val percentage = probabilities[i] * 100
                append("${classes[i]}: ${decimalFormat.format(percentage)}%\n")
            }
        }
        probabilityText.text = probabilityString


        when {
            notSure -> {
                result.setTextColor(resources.getColor(android.R.color.holo_orange_light, null))
            }
            predictedClass == 0 -> {
                result.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
            predictedClass == 1 -> {
                result.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
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