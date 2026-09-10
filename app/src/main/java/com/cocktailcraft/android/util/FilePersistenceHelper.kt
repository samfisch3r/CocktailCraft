package com.cocktailcraft.android.util

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilePersistenceHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Copies an image from a system Uri to the app's internal storage.
     * Returns the local Uri of the saved file.
     */
    fun saveImageToInternalStorage(uri: Uri): Uri? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "recipe_${UUID.randomUUID()}.jpg"
        val imagesDir = File(context.filesDir, "images")
        
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        
        val file = File(imagesDir, fileName)
        val outputStream = FileOutputStream(file)
        
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        
        return Uri.fromFile(file)
    }

    fun deleteImage(path: String) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }
}
