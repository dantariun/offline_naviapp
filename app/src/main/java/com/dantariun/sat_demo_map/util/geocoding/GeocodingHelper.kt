package com.dantariun.sat_demo_map.util.geocoding

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import java.io.IOException
import java.util.Locale

object GeocodingHelper {

    fun getLatLonFromAddressAsync(context: Context, strAddress: String, callback: (Pair<Double, Double>?) -> Unit) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val maxResults = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(strAddress, maxResults, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (addresses.isNotEmpty()) {
                        val lat = addresses[0].latitude
                        val lon = addresses[0].longitude
                        callback(Pair(lat, lon))
                    } else {
                        callback(null)
                    }
                }

                override fun onError(errorMessage: String?) {
                    Log.e("Geocoder", "Geocoding Error: $errorMessage")
                    callback(null)
                }
            })
        } else {
            var latLon: Pair<Double, Double>? = null

            try {
                @Suppress("DEPRECATION")
                val addressList: List<Address>? = geocoder.getFromLocationName(strAddress, 1)

                if (!addressList.isNullOrEmpty()) {
                    val address = addressList[0]
                    val latitude = address.latitude
                    val longitude = address.longitude
                    latLon = Pair(latitude, longitude)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }

            return callback(latLon)
        }
    }
}