package com.dantariun.sat_demo_map.network

import com.dantariun.sat_demo_map.network.model.AddressModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("search")
    suspend fun searchAddress(@Query("q") word:String, @Query("format") format: String = "json"): Response<List<AddressModel>>
}