package pl.test.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
    pojedynczy zapis historii badania
    metadane wyniku i sciezka do pliku ze zdjeciem
 */
@Entity(tableName = "results")
data class ResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val timestamp:Long,
    val imagePath: String?,
    val pMelanoma: Float,
    val label:String,
    val uncertain: Boolean
)