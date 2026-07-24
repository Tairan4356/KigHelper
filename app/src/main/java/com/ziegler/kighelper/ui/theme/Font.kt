package com.ziegler.kighelper.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ziegler.kighelper.R
import com.ziegler.kighelper.utils.FontManager
import java.io.File

object CustomFonts {
    val MiSans = FontFamily(
        Font(R.font.misans_normal, FontWeight.Normal),
        Font(R.font.misans_bold, FontWeight.Bold),
        Font(R.font.misans_heavy, FontWeight.ExtraBold)
    )
}

enum class FontType(
    val displayName: String,
    val fontFamily: FontFamily,
    val availableWeights: List<Int>
) {
    SYSTEM("系统默认", FontFamily.Default, listOf(400)),
    MI_SANS("Mi Sans", CustomFonts.MiSans, listOf(400, 700, 800))
}

private fun extractWeightFromFile(fileName: String): FontWeight {
    val lowerName = fileName.lowercase()
    return when {
        lowerName.contains("thin") -> FontWeight.Thin
        lowerName.contains("extralight") -> FontWeight.ExtraLight
        lowerName.contains("light") -> FontWeight.Light
        lowerName.contains("regular") || lowerName.contains("normal") -> FontWeight.Normal
        lowerName.contains("medium") -> FontWeight.Medium
        lowerName.contains("semibold") -> FontWeight.SemiBold
        lowerName.contains("bold") && !lowerName.contains("extrabold") -> FontWeight.Bold
        lowerName.contains("extrabold") -> FontWeight.ExtraBold
        lowerName.contains("heavy") -> FontWeight.ExtraBold
        lowerName.contains("black") -> FontWeight.Black
        else -> FontWeight.Normal
    }
}

fun loadCustomFontFamily(context: Context, fontName: String): FontFamily? {
    val fontDir = FontManager.getCustomFontsDir(context)
    val allFiles = fontDir.listFiles { file ->
        file.extension in listOf("ttf", "otf")
    } ?: return null

    val familyFiles = allFiles.filter { file ->
        val baseName = file.nameWithoutExtension
        baseName == fontName || baseName.startsWith("$fontName-") || baseName.startsWith("${fontName}_")
    }

    if (familyFiles.isEmpty()) {
        val singleFile = File(fontDir, "$fontName.ttf").takeIf { it.exists() }
            ?: File(fontDir, "$fontName.otf").takeIf { it.exists() }
            ?: return null
        return FontFamily(Font(singleFile))
    }

    val fonts = familyFiles.map { file ->
        val fontWeight = extractWeightFromFile(file.name)
        Font(file, fontWeight)
    }

    return FontFamily(*fonts.toTypedArray())
}
