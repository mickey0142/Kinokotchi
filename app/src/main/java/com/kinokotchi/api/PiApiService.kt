package com.kinokotchi.api

import android.content.SharedPreferences
import android.util.Log
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Deferred
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

private const val baseURL = "https://unsurprised-hedgehog-6645.dataplicity.io"

//private const val baseURL = "https://api.github.com/users/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private var retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .baseUrl(baseURL)
    .build()

interface PiApiService {

    @GET("/led/green/")// use @Url url:string to change base url ugly but it should work
    fun getGreenStatus() : Call<PiStatus>

    @GET("/")
    fun checkIsOnline() : Call<ConnectionResponse>

    @FormUrlEncoded
    @POST("/led/green/")
    fun setGreenStatus(@Field("state") state:String) : Call<PiStatus>

    @GET("/light")
    fun getLightStatus() : Call<Light>

    @FormUrlEncoded
    @POST("/light")
    fun setLightStatus(@Field("state") state: String) : Call<Light>

    @GET("/moisture")
    fun getMoisture() : Call<Moisture>

    @GET("/temperature")
    fun getTemperature() : Call<Temperature>

    @GET("/growth")
    fun getGrowth() : Call<Growth>

    @GET("/fan")
    fun getFanStatus() : Call<Integer> // change this in to class later

    @FormUrlEncoded
    @POST("/fan")
    fun setFanStatus(@Field("state") state: String) : Call<Integer>

    @FormUrlEncoded
    @POST("/water")
    fun water(@Field("size") size: String) : Call<Moisture> // maybe change this to just receive that it is success

}

object PiApi {
      var retrofitService : PiApiService = retrofit.create(PiApiService::class.java)// by lazy {
//        retrofit.create(PiApiService::class.java)
//    }

    // call this function before using piapi should change url to url in sharedpreferences
    private fun createNewUrl(url:String) {
        retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .baseUrl(url)
            .build()
        retrofitService = retrofit.create(PiApiService::class.java)
    }

    fun setupURL(sharedPreferences: SharedPreferences?) {
        if (sharedPreferences != null) {
            val baseURL = sharedPreferences.getString("connectionURL", "")
            createNewUrl(baseURL)
        }
    }

    fun setupURL(url: String) {
        createNewUrl(url)
    }
}

// maybe create another helper class here and have a method to use valid url to create retrofit first
// and call this method before using anything related to PiApi to avoid creating PiApi object before
// retrofit have any valid url