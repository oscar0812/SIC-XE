package com.bittle.SIC.utils;

public class Error {
    private String description;
    private int index;
    boolean isSet = false;

    public Error(String description, int index){
        this.description = description;
        this.index = index;
    }

    public String getDescription() {
        return description;
    }

    public int getIndex() {
        return index;
    }

    public boolean isSet() {
        return isSet;
    }

    public void setSet(boolean set) {
        isSet = set;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass()!=getClass())
            return false;
        Error e = ((Error)obj);
        return e.getDescription().equals(getDescription()) && e.getIndex() == getIndex();
    }
}
