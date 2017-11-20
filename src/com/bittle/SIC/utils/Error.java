package com.bittle.SIC.utils;

public class Error {
    private String description;
    private boolean isSet = false;

    public Error(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSet() {
        return isSet;
    }

    public void setFlag(boolean set) {
        isSet = set;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != getClass())
            return false;
        Error e = ((Error) obj);
        return e.getDescription().equals(getDescription());
    }
}
