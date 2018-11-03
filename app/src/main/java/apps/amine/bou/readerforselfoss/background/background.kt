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
import android.app.NotificationManager
import android.app.NotificationChannel
import android.util.Log
import androidx.core.app.NotificationCompat
import apps.amine.bou.readerforselfoss.R

class LoadingWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //If on Oreo then notification required a notification channel.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, "default")
            .setContentTitle("Loading")
            .setContentText("Loading new items")
            .setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)

        notificationManager.notify(1, notification.build())

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
                notificationManager.cancel(1)
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
                    notificationManager.cancel(1)
                }
            }
        })
        return Result.SUCCESS
    }
}