package com.ccl.grandcanyon.types;

import com.ccl.grandcanyon.types.Caller;
import com.ccl.grandcanyon.types.Reminder;

public class CallerInfo {
    private Caller caller;
    private Reminder reminder;

    public CallerInfo(Caller c, Reminder r){
        caller = c;
        reminder = r;
    }

    public Caller getCaller()  {
        return caller;
    }

    public void setCaller(Caller c){
        caller = c;
    }

    public Reminder getReminder()  {
        return reminder;
    }
    
    public void setReminder (Reminder r){
        reminder = r;
    }
}