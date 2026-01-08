package pl.test.myapplication

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.test.myapplication.data.ResultRepository
import java.io.File

@RunWith(AndroidJUnit4::class)
class ResultRepositoryInstrumentedTest {

    private lateinit var repo: ResultRepository

    @Before
    fun setup() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        repo = ResultRepository(ctx)
        repo.clearAll() // startujemy “na czysto”
    }

    @After
    fun teardown() = runBlocking {
        repo.clearAll()
    }

    @Test
    fun `saveResult zapisuje rekord i plik`() = runBlocking {
        val bmp = Bitmap.createBitmap(2000, 1000, Bitmap.Config.ARGB_8888)

        repo.saveResult(bmp, 0.70f)

        val all = repo.getAll()
        assertEquals(1, all.size)

        val e = all[0]
        assertEquals("CZERNIAK", e.label)
        assertFalse(e.uncertain)

        val path = e.imagePath
        assertNotNull(path)
        assertTrue(File(path!!).exists())
    }

    @Test
    fun `saveResult dla 50 procent daje niepewny`() = runBlocking {
        val bmp = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)

        repo.saveResult(bmp, 0.50f)

        val e = repo.getAll().first()
        assertEquals("NIEPEWNY", e.label)
        assertTrue(e.uncertain)
    }

    @Test
    fun `deleteById usuwa rekord i plik`() = runBlocking {
        val bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        repo.saveResult(bmp, 0.80f)

        val e = repo.getAll().first()
        val id = e.id
        val path = e.imagePath!!

        assertTrue(File(path).exists())

        repo.deleteById(id)

        assertTrue(repo.getAll().isEmpty())
        assertFalse(File(path).exists())
    }
}
