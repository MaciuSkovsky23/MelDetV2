package pl.test.myapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ResultDao {

    @Insert
    suspend fun insert(result: ResultEntity)

    @Delete
    suspend fun deleteOne(result: ResultEntity)

    @Query("SELECT * FROM results WHERE id = :id LIMIT 1")
    suspend fun getByID(id: Long): ResultEntity?

    @Update
    fun update(result: ResultEntity)

    @Query("SELECT * FROM results ORDER BY timestamp DESC")
    suspend fun getAll(): List<ResultEntity>

    @Query("DELETE FROM results")
    suspend fun clearAll()
}