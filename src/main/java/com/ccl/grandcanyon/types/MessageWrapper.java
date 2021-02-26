package com.ccl.grandcanyon.types;

public class MessageWrapper {
    private Message message;
    private Integer targetDistrictId;

    public void setMessage(Message m){
        message = m;
    }

    public Message getMessage(){
        return message;
    }

    public void setTargetDistrictId(Integer i){
        targetDistrictId = i;
    }

    public Integer getTargetDistrictId(){
        return targetDistrictId;
    }
}