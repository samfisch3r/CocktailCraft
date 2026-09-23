package com.cocktailcraft.android.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.cocktailcraft.android.domain.model.BarBackup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilePersistenceHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

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

    fun exportBackupZip(backup: BarBackup, outputStream: OutputStream) {
        val jsonString = json.encodeToString(backup)
        val zipOut = ZipOutputStream(BufferedOutputStream(outputStream))

        // 1. Write backup.json entry
        val jsonEntry = ZipEntry("backup.json")
        zipOut.putNextEntry(jsonEntry)
        zipOut.write(jsonString.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        // 2. Collect local image files from app images directory and referenced URIs
        val imagesDir = File(context.filesDir, "images")
        val localImageFiles = mutableSetOf<File>()

        if (imagesDir.exists() && imagesDir.isDirectory) {
            imagesDir.listFiles()?.filter { it.isFile }?.let {
                localImageFiles.addAll(it)
            }
        }

        val referencedUris = (backup.recipes.mapNotNull { it.imageUri } + backup.bottles.mapNotNull { it.imageUri })
        for (uriStr in referencedUris) {
            if (uriStr.startsWith("file:") || uriStr.startsWith("/")) {
                val path = try {
                    uriStr.toUri().path ?: uriStr
                } catch (_: Exception) {
                    uriStr
                }
                val f = File(path)
                if (f.exists() && f.isFile) {
                    localImageFiles.add(f)
                }
            }
        }

        // 3. Write each image file into images/ entry in the ZIP
        for (file in localImageFiles) {
            try {
                val entry = ZipEntry("images/${file.name}")
                zipOut.putNextEntry(entry)
                file.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            } catch (_: Exception) {
                // Ignore individual image read failures
            }
        }

        zipOut.finish()
    }

    fun importBackupZip(inputStream: InputStream): BarBackup {
        val bytes = inputStream.readBytes()
        val isZip = bytes.size >= 4 &&
                (bytes[0] == 0x50.toByte()) && // 'P'
                (bytes[1] == 0x4B.toByte()) && // 'K'
                (bytes[2] == 0x03.toByte()) &&
                (bytes[3] == 0x04.toByte())

        if (!isZip) {
            // Fallback for legacy plain JSON backups
            val jsonString = String(bytes, Charsets.UTF_8)
            return json.decodeFromString<BarBackup>(jsonString)
        }

        val imagesDir = File(context.filesDir, "images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        var backupJsonString: String? = null
        val zipIn = ZipInputStream(ByteArrayInputStream(bytes))
        var entry: ZipEntry? = zipIn.nextEntry

        while (entry != null) {
            val name = entry.name
            if (name == "backup.json" || name.endsWith("/backup.json")) {
                backupJsonString = zipIn.bufferedReader(Charsets.UTF_8).readText()
            } else if (name.startsWith("images/") && !entry.isDirectory) {
                val fileName = File(name).name
                if (fileName.isNotBlank()) {
                    val targetFile = File(imagesDir, fileName)
                    FileOutputStream(targetFile).use { out ->
                        zipIn.copyTo(out)
                    }
                }
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        if (backupJsonString == null) {
            error("Invalid backup archive: backup.json missing.")
        }

        val rawBackup = json.decodeFromString<BarBackup>(backupJsonString)

        // Update local image URIs in recipes and bottles to point to extracted files in filesDir/images
        val updatedRecipes = rawBackup.recipes.map { recipe ->
            recipe.imageUri?.let { uriStr ->
                if (uriStr.startsWith("file:") || uriStr.startsWith("/") || uriStr.startsWith("images/")) {
                    val fileName = File(try { uriStr.toUri().path ?: uriStr } catch (_: Exception) { uriStr }).name
                    val extractedFile = File(imagesDir, fileName)
                    if (extractedFile.exists()) {
                        return@map recipe.copy(imageUri = Uri.fromFile(extractedFile).toString())
                    }
                }
            }
            recipe
        }

        val updatedBottles = rawBackup.bottles.map { bottle ->
            bottle.imageUri?.let { uriStr ->
                if (uriStr.startsWith("file:") || uriStr.startsWith("/") || uriStr.startsWith("images/")) {
                    val fileName = File(try { uriStr.toUri().path ?: uriStr } catch (_: Exception) { uriStr }).name
                    val extractedFile = File(imagesDir, fileName)
                    if (extractedFile.exists()) {
                        return@map bottle.copy(imageUri = Uri.fromFile(extractedFile).toString())
                    }
                }
            }
            bottle
        }

        return rawBackup.copy(
            recipes = updatedRecipes,
            bottles = updatedBottles
        )
    }
}
