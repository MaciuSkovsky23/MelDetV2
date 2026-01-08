package pl.test.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/*
    główna baza room aplikacji
    singleton - jedna instancja na caly proces aplikacji
 */
@Database(entities = [ResultEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase(){

    abstract fun resultDao(): ResultDao

    companion object{
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase{
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "melanoma_results.db"
                ) .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}