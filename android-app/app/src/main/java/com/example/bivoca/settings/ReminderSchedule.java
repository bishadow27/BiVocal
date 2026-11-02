package com.example.bivoca.settings;

/** Immutable local reminder configuration. Calendar.SUNDAY is bit zero. */
public final class ReminderSchedule {
    public final boolean enabled;
    public final int hour, minute, days;
    public ReminderSchedule(boolean enabled, int hour, int minute, int days) {
        this.enabled = enabled; this.hour = hour; this.minute = minute; this.days = days;
    }
    public boolean isValid() {
        return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59
                && days >= 0 && days <= 127 && (!enabled || days != 0);
    }
    public ReminderSchedule withEnabled(boolean value) { return new ReminderSchedule(value,hour,minute,days); }
    public ReminderSchedule withTime(int h,int m) { return new ReminderSchedule(enabled,h,m,days); }
    public ReminderSchedule withDays(int value) { return new ReminderSchedule(enabled,hour,minute,value); }
    @Override public boolean equals(Object other) {
        if (!(other instanceof ReminderSchedule)) return false;
        ReminderSchedule s=(ReminderSchedule)other;
        return enabled==s.enabled && hour==s.hour && minute==s.minute && days==s.days;
    }
    @Override public int hashCode() { return (((enabled?1:0)*31+hour)*31+minute)*31+days; }
}
