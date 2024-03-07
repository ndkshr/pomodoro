package me.ndkshr.pomodoro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import me.ndkshr.pomodoro.CountdownTimerClass.Companion.ONE_MIN
import me.ndkshr.pomodoro.databinding.ActivityMainBinding

class TimerActivity : AppCompatActivity() {

    private var minutesWeHave = DEFAULT_TIMER

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seekNotificationPermissions()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

//        TODO: Make Shared Prefs work
//        minutesWeHave = PreferenceUtils(applicationContext).getLong(KEY_PREF_DEFAULT_MIN).toLong()
//        if (minutesWeHave == 0L) {
//            PreferenceUtils(applicationContext).writeLong(KEY_PREF_DEFAULT_MIN, DEFAULT_TIMER)
//            minutesWeHave = DEFAULT_TIMER
//        }

        setUIListeners()
        setServiceLiveDataListeners()

        if (LiveDataFactory.timerState.value == TimerState.BASE) {
            LiveDataFactory.timeRemainingLiveData.postValue(SECONDS * minutesWeHave * ONE_MILLI_SEC)
            startTimerService()
        }
    }

    private fun setUIListeners() {
        binding.add5.setOnClickListener {
            val rem = LiveDataFactory.timeRemainingLiveData.value ?: return@setOnClickListener
            val newRem = rem + (5 * ONE_MIN)

            if (newRem > (60 * ONE_MIN)) return@setOnClickListener

            if (LiveDataFactory.timerState.value == TimerState.BASE) {
//                TODO: Make Shared Prefs work
//                val min = PreferenceUtils(applicationContext).getLong(KEY_PREF_DEFAULT_MIN)
//                PreferenceUtils(applicationContext).writeLong(KEY_PREF_DEFAULT_MIN, (min + 5))
                LiveDataFactory.timeRemainingLiveData.postValue(newRem)
            } else {
                LiveDataFactory.timerCommand.postValue(TimerServiceCommands.ADD5)
            }
        }

        binding.minus5.setOnClickListener {
            val rem = LiveDataFactory.timeRemainingLiveData.value ?: return@setOnClickListener
            val newRem = rem - (5 * ONE_MIN)

            if (newRem <= 0L) return@setOnClickListener

            if (LiveDataFactory.timerState.value == TimerState.BASE) {
//                TODO: Make Shared Prefs work
//                val min = PreferenceUtils(applicationContext).getLong(KEY_PREF_DEFAULT_MIN)
//                PreferenceUtils(applicationContext).writeLong(KEY_PREF_DEFAULT_MIN, (min - 5))
                LiveDataFactory.timeRemainingLiveData.postValue(newRem)
            } else {
                LiveDataFactory.timerCommand.postValue(TimerServiceCommands.MIN5)
            }
        }

        binding.pausePlayBtn.setOnClickListener {
            managePauseOrResume()
        }

        binding.resetBtn.setOnClickListener {
            LiveDataFactory.timerCommand.postValue(TimerServiceCommands.RESET)
        }

        binding.timerTv.setOnClickListener {
            managePauseOrResume()
        }
    }

    private fun managePauseOrResume() {
        val timerState = LiveDataFactory.timerState.value

        when (timerState) {
            TimerState.BASE -> {
                LiveDataFactory.timerCommand.postValue(TimerServiceCommands.START)
            }

            TimerState.PAUSED -> {
                LiveDataFactory.timerCommand.postValue(TimerServiceCommands.START)
            }

            TimerState.RUNNING -> {
                LiveDataFactory.timerCommand.postValue(TimerServiceCommands.PAUSE)
            }

            else -> {}
        }
    }

    private fun seekNotificationPermissions() {
        with(NotificationManagerCompat.from(this)) {
            if (ContextCompat.checkSelfPermission(
                    this@TimerActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (isGranted) {
                        Toast.makeText(
                            this@TimerActivity,
                            "You will be able to see timer notifications!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@TimerActivity,
                            "You will not be able to see timer notifications!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
                } else {
                    ActivityCompat.requestPermissions(
                        this@TimerActivity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        100
                    )
                }
                return@with
            }
        }
    }

    private fun startTimerService() {
        val intent = Intent(this, TimerService::class.java)
//        ContextCompat.startForegroundService(this, intent)
        startForegroundService(intent)
    }

    private fun setServiceLiveDataListeners() {
        LiveDataFactory.timeRemainingLiveData.observe(this) {
            updateTimerUI()
        }

        LiveDataFactory.timerState.observe(this) { state ->
            when (state) {
                TimerState.RUNNING -> {
                    startTimerUI()
                }

                TimerState.PAUSED -> {
                    pauseTimerUI()
                }

                TimerState.BASE -> {
                    resetTimerUI()
                }

                else -> {}
            }
        }

        LiveDataFactory.timerMode.observe(this) { mode ->
            when (mode) {
                TimerMode.BREAK -> {
                    setParentBG(
                        arrayOf(
                            ColorDrawable(resources.getColor(R.color.eerie_black, null)),
                            ColorDrawable(resources.getColor(R.color.honolulu_blue, null))
                        )
                    )
                }

                TimerMode.WORK -> {
                    findViewById<TextView>(R.id.message).visibility = View.INVISIBLE
                    setParentBG(
                        arrayOf(
                            ColorDrawable(resources.getColor(R.color.honolulu_blue, null)),
                            ColorDrawable(resources.getColor(R.color.eerie_black, null))
                        )
                    )
                }

                else -> {}
            }
        }
    }

    private fun setParentBG(colors: Array<ColorDrawable>) {
        val transition = TransitionDrawable(colors)
        findViewById<ConstraintLayout>(R.id.timer_parent).background = transition
        transition.startTransition(ONE_MILLI_SEC.toInt())
    }

    private fun startTimerUI() {
        val timerText: TextView = findViewById(R.id.timer_tv)
        timerText.setTextColor(resources.getColor(R.color.ivory, null))
        findViewById<TextView>(R.id.message).visibility = View.INVISIBLE
        binding.pausePlayBtn.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.pause,
                null
            )
        )
    }

    private fun updateTimerUI() {
        val timerText: TextView = findViewById(R.id.timer_tv)
        val totalTimeLeft = LiveDataFactory.timeRemainingLiveData.value!!
        timerText.text = TimerUtils.timeString(totalTimeLeft)
    }

    private fun resetTimerUI() {
//        minutesWeHave = PreferenceUtils(applicationContext).getLong(KEY_PREF_DEFAULT_MIN)
        LiveDataFactory.timeRemainingLiveData.postValue(minutesWeHave * SECONDS * ONE_MILLI_SEC)
        findViewById<TextView>(R.id.timer_tv).setTextColor(resources.getColor(R.color.ivory, null))
        findViewById<TextView>(R.id.message).visibility = View.INVISIBLE
        binding.pausePlayBtn.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.play,
                null
            )
        )
        setParentBG(
            arrayOf(
                ColorDrawable(resources.getColor(R.color.honolulu_blue, null)),
                ColorDrawable(resources.getColor(R.color.eerie_black, null))
            )
        )
    }

    private fun pauseTimerUI() {
        findViewById<TextView>(R.id.message).visibility = View.VISIBLE
        binding.pausePlayBtn.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.play,
                null
            )
        )
    }

    override fun onStop() {
        if (LiveDataFactory.timerState.value == TimerState.BASE) {
            stopService(Intent(this, TimerService::class.java))
        }
        super.onStop()
    }
}