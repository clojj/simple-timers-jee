package cdiextension;

import de.clojj.simpletimers.TimerObject;

public class TimerFiredEvent {
    TimerObject timerObject;
    long now;

    public TimerFiredEvent(TimerObject timerObject, long now) {
        this.timerObject = timerObject;
        this.now = now;
    }

    public TimerObject getTimerObject() {
        return timerObject;
    }

    public long getNow() {
        return now;
    }

    @Override
    public String toString() {
        return "TimerFiredEvent{" +
                "timerObject=" + timerObject +
                ", now=" + now +
                '}';
    }
}
