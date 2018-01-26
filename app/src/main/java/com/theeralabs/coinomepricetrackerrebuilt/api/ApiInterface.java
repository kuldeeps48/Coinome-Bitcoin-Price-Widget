package com.theeralabs.coinomepricetrackerrebuilt.api;

import com.theeralabs.coinomepricetrackerrebuilt.model.PriceData;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiInterface {

    @GET("ticker.json")
    Call<PriceData> getPriceData();


}