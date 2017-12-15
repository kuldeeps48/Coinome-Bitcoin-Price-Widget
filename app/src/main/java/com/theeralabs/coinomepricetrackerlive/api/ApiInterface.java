package com.theeralabs.coinomepricetrackerlive.api;

import com.theeralabs.coinomepricetrackerlive.model.PriceData;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiInterface {

    @GET("ticker.json")
    Call<PriceData> getPriceData();


}