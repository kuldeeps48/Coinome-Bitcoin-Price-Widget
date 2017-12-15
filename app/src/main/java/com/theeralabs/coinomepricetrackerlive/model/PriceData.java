package com.theeralabs.coinomepricetrackerlive.model;

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

}