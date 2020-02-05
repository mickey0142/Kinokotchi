package com.kinokotchi.api

import android.content.SharedPreferences
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
    @GET("/")
    fun checkIsOnline() : Call<ConnectionResponse>

    @GET("/light/")
    fun getLightStatus() : Call<Light>

    @FormUrlEncoded
    @POST("/light/")
    fun setLightStatus(@Field("state") state: Int) : Call<Light>

    @GET("/moisture/")
    fun getMoisture() : Call<Moisture>

    @GET("/isFoodLow/")
    fun getIsFoodLow() : Call<IsFoodLow>

    @GET("/temperature/")
    fun getTemperature() : Call<Temperature>

    @GET("/growth/")
    fun getGrowth() : Call<Growth>

    @GET("/fan/")
    fun getFanStatus() : Call<Light> // change this in to fan class later or change class name to something else

    @FormUrlEncoded
    @POST("/fan/")
    fun setFanStatus(@Field("state") state: Int) : Call<Light>

    @FormUrlEncoded
    @POST("/water/")
    fun water(@Field("size") size: String) : Call<ConnectionResponse> // maybe change this to just receive that it is success

    @GET("/image/")
    fun getImage() : Call<EncodedImage> // maybe change this into object that have multiple type of image such as original image, filtered image

    @GET("/status/")
    fun getAllStatus() : Call<PiStatus>
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