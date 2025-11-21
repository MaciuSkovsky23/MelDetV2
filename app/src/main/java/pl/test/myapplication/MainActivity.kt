package pl.test.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.Image
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.renderscript.Element
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.scale
//import pl.test.myapplication.ml.SecondTestModel
import pl.test.myapplication.ml.NewTestV1
import pl.test.myapplication.ui.theme.MyApplicationTheme
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.jar.Manifest
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.DataType

private const val MODEL_NAME = "NewTestV1"

class MainActivity : AppCompatActivity() {
    private lateinit var camera: Button
    private lateinit var gallery: Button
    private lateinit var imageView: ImageView
    private lateinit var result: TextView

    private val imageSize = 224
    private val classes = arrayOf("Malignant", "Benign")

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val image = data?.extras?.get("data") as? Bitmap
                image?.let { processImage(it) }
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        camera = findViewById(R.id.button)
        gallery = findViewById(R.id.button2)
        result = findViewById(R.id.result)
        imageView = findViewById(R.id.imageView)
    }

    private fun processImage(image: Bitmap) {
        val dimension = minOf(image.width, image.height)
        val thumbnail = ThumbnailUtils.extractThumbnail(image, dimension, dimension)
        imageView.setImageBitmap(thumbnail)

//        val scaledImage = Bitmap.createScaledBitmap(thumbnail, imageSize, imageSize, false)
        val scaledImage = thumbnail.scale(imageSize, imageSize)
        classifyImage(scaledImage)

    }

    private fun processGalleryImage(uri: Uri) {
        try {
            val image = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            imageView.setImageBitmap(image)

//            val scaledImage = Bitmap.createScaledBitmap(image, imageSize, imageSize, false)
            val scaledImage = image.scale(imageSize,imageSize)
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
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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
    }

    private fun classifyImage(image: Bitmap) {
        try {
//            val model = SecondTestModel.newInstance(applicationContext)
            val model = NewTestV1.newInstance(applicationContext)

// Creates inputs for reference.
            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3).apply {
                order(ByteOrder.nativeOrder())
            }

            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)

            //iterate over each pixel and extract r,g,b values
            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val value = intValues[pixel++]
                    byteBuffer.putFloat(((value shr 16) and 0xFF) * (1f / 1))   //red
                    byteBuffer.putFloat(((value shr 8) and 0xFF) * (1f / 1))    //green
                    byteBuffer.putFloat(((value) and 0xFF) * (1f / 1))          //blue
                }
            }

            inputFeature0.loadBuffer(byteBuffer)

// Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            val confidences = outputFeature0.floatArray
            val maxPos = confidences.indices.maxByOrNull { confidences[it] } ?: 0

            result.text = classes[maxPos]
// Releases model resources if no longer used.
            model.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


//    private fun classifyImage2(image: Bitmap) {
//        try {
//            Model.newInstance(applicationContext).use { model ->
//                // Creates inputs for reference
//                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 32, 32, 3), DataType.FLOAT32)
//                val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3).apply {
//                    order(ByteOrder.nativeOrder())
//                }
//
//                val intValues = IntArray(imageSize * imageSize)
//                image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)
//
//                // Iterate over each pixel and extract R, G, and B values
//                var pixel = 0
//                for (i in 0 until imageSize) {
//                    for (j in 0 until imageSize) {
//                        val value = intValues[pixel++]
//                        byteBuffer.putFloat(((value shr 16) and 0xFF) * (1f / 1))
//                        byteBuffer.putFloat(((value shr 8) and 0xFF) * (1f / 1))
//                        byteBuffer.putFloat((value and 0xFF) * (1f / 1))
//                    }
//                }
//
//                inputFeature0.loadBuffer(byteBuffer)
//
//                // Runs model inference and gets result
//                val outputs = model.process(inputFeature0)
//                val outputFeature0 = outputs.outputFeature0AsTensorBuffer
//
//                val confidences = outputFeature0.floatArray
//                // Find the index of the class with the biggest confidence
//                val maxPos = confidences.indices.maxByOrNull { confidences[it] } ?: 0
//
//                result.text = classes[maxPos]
//            }
//        } catch (e: IOException) {
//            // TODO Handle the exception
//            e.printStackTrace()
//        }
//    }
//}
