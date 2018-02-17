package com.theeralabs.coinomepricetrackerrebuilt.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PriceData {

@SerializedName("btc-inr")
@Expose
private BtcInr btcInr;
@SerializedName("bch-inr")
@Expose
private BchInr bchInr;
@SerializedName("ltc-inr")
@Expose
private LtcInr ltcInr;
@SerializedName("dash-inr")
@Expose
private DashInr dashInr;
@SerializedName("dgb-inr")
@Expose
private DgbInr dgbInr;

public BtcInr getBtcInr() {
return btcInr;
}

public void setBtcInr(BtcInr btcInr) {
this.btcInr = btcInr;
}

public BchInr getBchInr() {
return bchInr;
}

public void setBchInr(BchInr bchInr) {
this.bchInr = bchInr;
}

public LtcInr getLtcInr() {
return ltcInr;
}

public void setLtcInr(LtcInr ltcInr) {
this.ltcInr = ltcInr;
}

public DashInr getDashInr() {
return dashInr;
}

public void setDashInr(DashInr dashInr) {
this.dashInr = dashInr;
}

public DgbInr getDgbInr() {
return dgbInr;
}

public void setDgbInr(DgbInr dgbInr) {
this.dgbInr = dgbInr;
}

}