package com.dantariun.sat_demo_map.network

import javax.inject.Inject

class MainRepository @Inject constructor(private val apiService :ApiService) {

    suspend fun searchAddress(word:String) =
        apiService.searchAddress(word)
}