package com.ziegler.kighelper.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ziegler.kighelper.data.FontCatalogItem
import java.io.File
import java.io.FileOutputStream

object FontManager {
    private const val TAG = "FontManager"
    private const val CUSTOM_FONTS_DIR = "custom_fonts"

    fun getCustomFontsDir(context: Context): File {
        val dir = File(context.filesDir, CUSTOM_FONTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveFontFile(
        context: Context, fileName: String, inputStream: java.io.InputStream
    ): Result<File> {
        return try {
            val fontDir = getCustomFontsDir(context)
            val file = File(fontDir, fileName)

            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }

            Log.d(TAG, "Font saved: ${file.absolutePath}")
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save font: $fileName", e)
            Result.failure(e)
        }
    }

    fun saveFontFileFromUri(context: Context, uri: Uri, fileName: String): Result<File> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return Result.failure(
                Exception("Cannot open URI")
            )

            val result = saveFontFile(context, fileName, inputStream)
            inputStream.close()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save font from URI: $uri", e)
            Result.failure(e)
        }
    }

    fun deleteFontFile(context: Context, fileName: String): Result<Unit> {
        return try {
            val fontDir = getCustomFontsDir(context)
            val file = File(fontDir, fileName)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Font deleted: $fileName")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete font: $fileName", e)
            Result.failure(e)
        }
    }

    fun deleteAllFontFiles(context: Context, fontBaseName: String): Result<Unit> {
        return try {
            val fontDir = getCustomFontsDir(context)
            val files = fontDir.listFiles { file ->
                file.name.startsWith(fontBaseName) && file.extension in listOf("ttf", "otf")
            }
            files?.forEach { it.delete() }
            Log.d(TAG, "All weight files deleted for: $fontBaseName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete font files for: $fontBaseName", e)
            Result.failure(e)
        }
    }

    fun getInstalledFonts(
        context: Context, catalog: List<FontCatalogItem>? = null
    ): List<InstalledFont> {
        val fontDir = getCustomFontsDir(context)
        val fontFiles = fontDir.listFiles { file ->
            file.extension in listOf("ttf", "otf")
        } ?: emptyArray()

        val grouped = mutableMapOf<String, MutableList<File>>()
        for (file in fontFiles) {
            val baseName = file.nameWithoutExtension
            val familyName = extractFamilyName(baseName)
            grouped.getOrPut(familyName) { mutableListOf() }.add(file)
        }

        return grouped.map { (familyName, files) ->
            val familyFileNames = files.map { it.name }.toSet()
            val matchedCatalog = catalog?.firstOrNull { fontItem ->
                fontItem.weights.any { weight ->
                    familyFileNames.contains(weight.fileName)
                }
            }

            val weights = files.map { file ->
                val baseName = file.nameWithoutExtension
                val weight = extractWeightFromFileName(baseName)
                WeightInfo(weight, file.name)
            }.sortedBy { it.weight }

            InstalledFont(
                baseName = familyName,
                displayName = matchedCatalog?.displayName ?: formatDisplayName(familyName),
                files = files.map { it.name },
                weights = weights,
                catalogFontId = matchedCatalog?.id
            )
        }.sortedBy { it.displayName }
    }

    private fun extractFamilyName(baseName: String): String {
        val lower = baseName.lowercase()
        val weightPatterns = listOf(
            "thin",
            "extralight",
            "light",
            "regular",
            "normal",
            "medium",
            "semibold",
            "bold",
            "extrabold",
            "heavy",
            "black"
        )
        for (pattern in weightPatterns) {
            if (lower.endsWith("-$pattern") || lower.endsWith("_$pattern")) {
                return baseName.substring(0, baseName.length - pattern.length - 1)
            }
        }
        return baseName
    }

    fun fontFileExists(context: Context, fileName: String): Boolean {
        val fontDir = getCustomFontsDir(context)
        return File(fontDir, fileName).exists()
    }

    fun getFontFileSize(context: Context, fileName: String): Long {
        val fontDir = getCustomFontsDir(context)
        val file = File(fontDir, fileName)
        return if (file.exists()) file.length() else 0L
    }

    private fun extractWeightFromFileName(fileName: String): Int {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.contains("thin") -> 100
            lowerName.contains("extralight") -> 200
            lowerName.contains("light") -> 300
            lowerName.contains("regular") || lowerName.contains("normal") -> 400
            lowerName.contains("medium") -> 500
            lowerName.contains("semibold") -> 600
            lowerName.contains("bold") && !lowerName.contains("extrabold") -> 700
            lowerName.contains("extrabold") -> 800
            lowerName.contains("heavy") -> 900
            lowerName.contains("black") -> 900
            else -> -1
        }
    }

    private fun formatDisplayName(baseName: String): String {
        return baseName.replace("_", " ").replace("-", " ").split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }
}

data class InstalledFont(
    val baseName: String,
    val displayName: String,
    val files: List<String>,
    val weights: List<WeightInfo>,
    val catalogFontId: String? = null
)

data class WeightInfo(
    val weight: Int, val fileName: String
) {
    val label: String
        get() = when (weight) {
            100 -> "Thin"
            200 -> "ExtraLight"
            300 -> "Light"
            400 -> "Regular"
            500 -> "Medium"
            600 -> "SemiBold"
            700 -> "Bold"
            800 -> "ExtraBold"
            900 -> "Black"
            else -> "Regular"
        }
}
