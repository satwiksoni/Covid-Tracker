package com.example.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.AbsListView
import android.widget.Toast
import androidx.work.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    lateinit var adapter:StateListAdapter

    @InternalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        list.addHeaderView(LayoutInflater.from(this).inflate(R.layout.list_header, list, false))
        fetchResult()

        swipeToRefresh.setOnRefreshListener {
            fetchResult()
        }
        initWorker()
        list.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}
            override fun onScroll(
                    view: AbsListView,
                    firstVisibleItem: Int,
                    visibleItemCount: Int,
                    totalItemCount: Int
            )
            {
                if (list.getChildAt(0) != null) {
                    swipeToRefresh.isEnabled = list.firstVisiblePosition === 0 && list.getChildAt(
                            0
                    ).getTop() === 0
                }
            }
        })
    }

    private fun fetchResult() {
        GlobalScope.launch{
            val response = withContext(Dispatchers.IO)
            {
                Client.api.clone().execute()
            }
            if (response.isSuccessful) {

               swipeToRefresh.isRefreshing=false
                val data = Gson().fromJson(response.body?.string(), Response::class.java)
                launch(Dispatchers.Main) {
                    bindCombinedData(data.statewise[0])
                    bindStateWiseData(data.statewise.subList(0,data.statewise.size))

                }
            }

        }
    }

    private fun bindStateWiseData(subList: List<StatewiseItem>) {
        adapter= StateListAdapter(subList)
        list.adapter=adapter

    }

    private fun bindCombinedData(get: StatewiseItem) {
        val lastUpdatedTime=get.lastupdatedtime
        val simpleDateFormat=SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        lastUpdatedtv.text="Last Updated\n ${getTimeAgo(simpleDateFormat.parse(lastUpdatedTime))}"
        confitmedtv.text=get.confirmed
        deathtv.text=get.deaths
        Recoveredtv.text=get.recovered
        Activetv.text=get.active

    }

    @InternalCoroutinesApi
    private fun initWorker() {
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val notificationWorkRequest =
                PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "JOB_TAG",
                ExistingPeriodicWorkPolicy.KEEP,
                notificationWorkRequest
        )
    }

}
fun getTimeAgo(past: Date): String
{
    val now = Date()
    val seconds = TimeUnit.MILLISECONDS.toSeconds(now.time - past.time)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(now.time - past.time)
    val hours = TimeUnit.MILLISECONDS.toHours(now.time - past.time)

    return when {
        seconds < 60 -> {
            "Few seconds ago"
        }
        minutes < 60 -> {
            "$minutes minutes ago"
        }
        hours < 24 -> {
            "$hours hour ${minutes % 60} min ago"
        }
        else -> {
            SimpleDateFormat("dd/MM/yy, hh:mm a").format(past).toString()
        }
    }

}





