package me.ndkshr.pomodoro

import android.os.CountDownTimer

class CountdownTimerClass(
    val timeRemaining: Long,
    val counterInterval: Long = 1000L
): CountDownTimer(timeRemaining, counterInterval) {

    companion object {
        const val ONE_SEC = 1000L
        const val ONE_MIN = 60 * ONE_SEC
        const val DELTA_MINUTES = 5 * ONE_MIN
    }

    override fun onTick(millisUntilFinished: Long) {
        LiveDataFactory.timeRemainingLiveData.postValue(
            (timeRemaining * ONE_MIN) - counterInterval
        )
    }

    override fun onFinish() {
        val mode = LiveDataFactory.timerMode.value ?: TimerMode.WORK
        when (mode) {
            TimerMode.BREAK -> {
                LiveDataFactory.timerCommand.postValue(TimerServiceCommands.RESET)
            }
            TimerMode.WORK -> {
                LiveDataFactory.timerMode.postValue(TimerMode.BREAK)
            }
        }
    }
}