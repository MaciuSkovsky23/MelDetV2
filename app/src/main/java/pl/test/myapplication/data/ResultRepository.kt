package pl.test.myapplication.data

import android.content.Context
import android.graphics.Bitmap

/*
    klasa ktora ma spinac zapisywanie metadanych w room i zapisywanie zdjec przez imagestore
 */
class ResultRepository(context: Context) {
    //applicationcontext aby nie trzymac referencji do fragment
    private val appContext = context.applicationContext
    private val dataBase = AppDatabase.get(appContext)
    private val dao = dataBase.resultDao()

    suspend fun saveResult(bitmap: Bitmap, pMelanoma: Float){
        val p = pMelanoma.coerceIn(0f, 1f)
        val uncertain = p in 0.40f..0.60f

        val label = when{
            uncertain -> "NIEPEWNY"
            p >= 0.5f -> "CZERNIAK"
            else -> "ZMIANA ŁAGODNA"
        }

        //zapis pliku
        val imagePath = ImageStore.saveJpeg(appContext, bitmap)

        //zapis w bazie danych
        dao.insert(
            ResultEntity(
                timestamp = System.currentTimeMillis(),
                imagePath = imagePath,
                pMelanoma = p,
                label = label,
                uncertain = uncertain
            )
        )
    }

    //pobiera całą historie
    suspend fun getAll(): List<ResultEntity> = dao.getAll()

    //czysci całą historie
    suspend fun clearAll() {
        dao.clearAll()
        ImageStore.deleteAll(appContext)
    }
}