package com.theeralabs.coinomepricetrackerrebuilt.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DgbInr implements Coin{

@SerializedName("last")
@Expose
private String last;
@SerializedName("lowest_ask")
@Expose
private String lowestAsk;
@SerializedName("highest_bid")
@Expose
private String highestBid;
@SerializedName("24hr_volume")
@Expose
private Object _24hrVolume;

public String getLast() {
return last;
}

public void setLast(String last) {
this.last = last;
}

@Override
public String getLowestAsk() {
return lowestAsk;
}

public void setLowestAsk(String lowestAsk) {
this.lowestAsk = lowestAsk;
}

@Override
public String getHighestBid() {
return highestBid;
}

public void setHighestBid(String highestBid) {
this.highestBid = highestBid;
}

public Object get24hrVolume() {
return _24hrVolume;
}

public void set24hrVolume(Object _24hrVolume) {
this._24hrVolume = _24hrVolume;
}

}