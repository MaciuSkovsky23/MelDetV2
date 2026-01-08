package pl.test.myapplication.data

import android.content.Context
import android.graphics.Bitmap

/*
    repozytorium historii badan
    ma laczyc zapisywanie metadanych w room i zapisywanie zdjec w pamieci przez imagestore
 */
class ResultRepository(context: Context) {
    //applicationcontext aby nie trzymac referencji do fragment
    private val appContext = context.applicationContext
    private val dataBase = AppDatabase.get(appContext)
    private val dao = dataBase.resultDao()

    //zapisuje wynik analizy: kopie zdjecia(jpg) i rekord w bazie room
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

    //zwraca całą historie badan
    suspend fun getAll(): List<ResultEntity> = dao.getAll()

    //czysci całą historie badan
    suspend fun clearAll() {
        dao.clearAll()
        ImageStore.deleteAll(appContext)
    }

    //pobiera pojedynczy wynik po id
    suspend fun getById(id: Long): ResultEntity? = dao.getByID(id)

    //usuwa pojedynczy wpis w historii + usuwa plik zdjecia
    suspend fun deleteOne(entity: ResultEntity){
        ImageStore.delete(entity.imagePath)
        dao.deleteOne(entity)
    }

    //usuwa pojedynczy wpis po id
    suspend fun deleteById(id: Long){
        val entity = dao.getByID(id) ?: return
        deleteOne(entity)
    }
}