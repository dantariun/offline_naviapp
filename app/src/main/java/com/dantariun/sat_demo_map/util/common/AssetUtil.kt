package com.dantariun.sat_demo_map.util.common

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object AssetUtil {

    fun copyMapFromAssetsToStorage(context: Context, fileName: String): File? {
        val mapFile = File(context.filesDir, fileName)
        if (mapFile.exists()) {
            return mapFile
        }

        try {
            val inputStream = context.assets.open(fileName)
            val outputStream = FileOutputStream(mapFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.flush()
            outputStream.close()
            return mapFile
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}