package com.theeralabs.coinomepricetrackerrebuilt.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PriceData {

    @SerializedName("BTC-INR")
    @Expose
    private BTCINR bTCINR;
    @SerializedName("BCH-INR")
    @Expose
    private BCHINR bCHINR;
    @SerializedName("LTC-INR")
    @Expose
    private LTCINR lTCINR;
    @SerializedName("DASH-INR")
    @Expose
    private DASHINR dASHINR;

    public BTCINR getBTCINR() {
        return bTCINR;
    }

    public void setBTCINR(BTCINR bTCINR) {
        this.bTCINR = bTCINR;
    }

    public BCHINR getBCHINR() {
        return bCHINR;
    }

    public void setBCHINR(BCHINR bCHINR) {
        this.bCHINR = bCHINR;
    }

    public LTCINR getLTCINR() {
        return lTCINR;
    }

    public void setLTCINR(LTCINR lTCINR) {
        this.lTCINR = lTCINR;
    }

    public DASHINR getDASHINR() {
        return dASHINR;
    }

    public void setDASHINR(DASHINR dASHINR) {
        this.dASHINR = dASHINR;
    }


}