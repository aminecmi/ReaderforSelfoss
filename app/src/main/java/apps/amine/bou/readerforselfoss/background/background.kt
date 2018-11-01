package apps.amine.bou.readerforselfoss.background

import android.content.Context
import android.preference.PreferenceManager
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.persistence.database.AppDatabase
import apps.amine.bou.readerforselfoss.persistence.migrations.MIGRATION_1_2
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.persistence.toEntity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class LoadingWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val settings = this.context.getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this.context)
        val shouldLogEverything = sharedPref.getBoolean("should_log_everything", false)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "selfoss-database"
        ).addMigrations(MIGRATION_1_2).build()

        val api = SelfossApi(
            this.context,
            null,
            settings.getBoolean("isSelfSignedCert", false),
            shouldLogEverything
        )
        api.allItems().enqueue(object : Callback<List<Item>> {
            override fun onFailure(call: Call<List<Item>>, t: Throwable) {
            }

            override fun onResponse(
                call: Call<List<Item>>,
                response: Response<List<Item>>
            ) {
                thread {
                    if (response.body() != null) {
                        val apiItems = (response.body() as ArrayList<Item>)
                        db.itemsDao().deleteAllItems()
                        db.itemsDao()
                            .insertAllItems(*(apiItems.map { it.toEntity() }).toTypedArray())
                    }
                }
            }
        })
        return Result.SUCCESS
    }
}