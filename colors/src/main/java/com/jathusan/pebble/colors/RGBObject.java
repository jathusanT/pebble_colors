package com.jathusan.pebble.colors;

public class RGBObject {
    private boolean isAbsolute;
    private int rValue;
    private int gValue;
    private int bValue;
    private boolean isSelected;

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public boolean isAbsolute() {
        return isAbsolute;
    }

    public void setAbsolute(boolean isAbsolute) {
        this.isAbsolute = isAbsolute;
    }

    public int getRValue() {
        return rValue;
    }

    public void setRValue(int rValue) {
        this.rValue = rValue;
    }

    public int getGValue() {
        return gValue;
    }

    public void setGValue(int gValue) {
        this.gValue = gValue;
    }

    public int getBValue() {
        return bValue;
    }

    public void setBValue(int bValue) {
        this.bValue = bValue;
    }
}
