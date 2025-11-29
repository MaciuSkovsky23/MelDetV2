package pl.test.myapplication
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import pl.test.myapplication.databinding.ActivityMainBinding

private const val MODEL_NAME = "NewTestV6"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
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
