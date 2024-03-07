package me.ndkshr.pomodoro

import androidx.lifecycle.MutableLiveData

object LiveDataFactory {
    val timeRemainingLiveData = MutableLiveData<Long>()
    val timerCommand = MutableLiveData<TimerServiceCommands>()
    val timerMode = MutableLiveData<TimerMode>(TimerMode.WORK)
    val timerState = MutableLiveData<TimerState>(TimerState.BASE)
}
