package pl.test.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "results")
data class ResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val timestamp:Long,
    val imagePath: String?,
    val pMelanoma: Float,
    val label:String,
    val uncertain: Boolean
)