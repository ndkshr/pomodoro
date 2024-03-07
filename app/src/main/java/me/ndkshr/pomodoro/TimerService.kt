package me.ndkshr.pomodoro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.view.View
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import me.ndkshr.pomodoro.CountdownTimerClass.Companion.DELTA_MINUTES
import me.ndkshr.pomodoro.CountdownTimerClass.Companion.ONE_MIN


const val CHANNEL_ID = "pomodoro_ndkshr"
const val NOTIFICATION_ID = 1000

class TimerService : Service() {

    private lateinit var notification: Notification
    private lateinit var notificationManager: NotificationManager
    private lateinit var countDownTimer: CountDownTimer
    private var timerActivityIntent:Intent? = null
    private var timerActivityPendingIntent: PendingIntent? = null

    override fun onCreate() {
        countDownTimer = CountdownTimerClass(DEFAULT_TIMER)
        getNotificationManager()
        createChannel()

        timerActivityIntent = Intent(this, TimerActivity::class.java)
        timerActivityPendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(timerActivityIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.pomodoro)
            .setContentTitle("Pomodoro: ${LiveDataFactory.timerMode.value}")
            .setContentText("Time Remaining: ${TimerUtils.timeString(DEFAULT_TIMER * ONE_MIN)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(timerActivityPendingIntent)

        notification = builder.build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LiveDataFactory.timerCommand.observeForever { command ->
            when (command) {
                TimerServiceCommands.START -> {
                    startTimer()
                }

                TimerServiceCommands.PAUSE -> {
                    pauseTimer()
                }

                TimerServiceCommands.RESET -> {
                    resetTimer()
                }

                TimerServiceCommands.ADD5 -> {
                    add5Minutes()
                }

                TimerServiceCommands.MIN5 -> {
                    minus5Minutes()
                }

                else -> {}
            }
        }

        LiveDataFactory.timerMode.observeForever { mode ->
            when (mode) {
                TimerMode.BREAK -> {
                    LiveDataFactory.timeRemainingLiveData.postValue(BREAK_TIME * ONE_MIN)
                    startTimer()
                }

                TimerMode.WORK -> {
                    resetTimer()
                }

                else -> {}
            }
        }

        LiveDataFactory.timeRemainingLiveData.observeForever {
            updateNotification()
        }

        return START_STICKY
    }

    private fun add5Minutes() {
        countDownTimer.cancel()
        val rem = LiveDataFactory.timeRemainingLiveData.value!! + DELTA_MINUTES
        LiveDataFactory.timeRemainingLiveData.postValue(rem)

        if (LiveDataFactory.timerState.value == TimerState.RUNNING)
            LiveDataFactory.timerCommand.postValue(TimerServiceCommands.START)
    }

    private fun minus5Minutes() {
        countDownTimer.cancel()
        val rem = LiveDataFactory.timeRemainingLiveData.value!! - DELTA_MINUTES
        LiveDataFactory.timeRemainingLiveData.postValue(rem)
        if (LiveDataFactory.timerState.value == TimerState.RUNNING)
            LiveDataFactory.timerCommand.postValue(TimerServiceCommands.START)
    }

    private fun startTimer() {
        if (LiveDataFactory.timerState.value != TimerState.BASE) {
            countDownTimer.cancel()
        }
        val totalTimeLeft = if (LiveDataFactory.timerMode.value == TimerMode.WORK) {
            LiveDataFactory.timeRemainingLiveData.value ?: DEFAULT_TIMER
        } else {
            BREAK_TIME * ONE_MIN
        }

        countDownTimer = object : CountDownTimer(totalTimeLeft, ONE_MILLI_SEC) {
            override fun onTick(millisUntilFinished: Long) {
                val command = LiveDataFactory.timerCommand.value
                if (command == TimerServiceCommands.ADD5 || command == TimerServiceCommands.MIN5) return
                LiveDataFactory.timeRemainingLiveData.postValue(millisUntilFinished)
            }

            override fun onFinish() {
                if (LiveDataFactory.timerMode.value == TimerMode.WORK) {
                    LiveDataFactory.timerMode.postValue(TimerMode.BREAK)
                } else {
                    LiveDataFactory.timerState.postValue(TimerState.BASE)
                    LiveDataFactory.timerMode.postValue(TimerMode.WORK)
                    notificationManager.cancel(NOTIFICATION_ID)
                }
            }

        }.start()
        LiveDataFactory.timerState.postValue(TimerState.RUNNING)
    }

    private fun pauseTimer() {
        countDownTimer.cancel()
        LiveDataFactory.timerState.postValue(TimerState.PAUSED)
    }

    private fun resetTimer() {
        countDownTimer.cancel()
        LiveDataFactory.timerState.postValue(TimerState.BASE)
    }

    private fun getNotificationManager() {
        notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager
    }

    private fun updateNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.pomodoro)
            .setContentTitle("Pomodoro: ${LiveDataFactory.timerMode.value}")
            .setContentText("Time Remaining: ${TimerUtils.timeString(LiveDataFactory.timeRemainingLiveData.value!!)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(timerActivityPendingIntent)

        notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            "Pomodoro",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.setSound(null, null)
        notificationChannel.setShowBadge(true)
        notificationManager.createNotificationChannel(notificationChannel)
    }
}