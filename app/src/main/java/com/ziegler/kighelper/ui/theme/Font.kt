package com.ziegler.kighelper.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ziegler.kighelper.R

object CustomFonts {
    val LXGWWenKai = FontFamily(
        Font(R.font.lxgwwenkai_light, FontWeight.Light),
        Font(R.font.lxgwwenkai_regular, FontWeight.Normal),
        Font(R.font.lxgwwenkai_medium, FontWeight.Medium)
    )

    val MiSans = FontFamily(
        Font(R.font.misans_thin, FontWeight.Thin),
        Font(R.font.misans_extralight, FontWeight.ExtraLight),
        Font(R.font.misans_light, FontWeight.Light),
        Font(R.font.misans_normal, FontWeight.Normal),
        Font(R.font.misans_medium, FontWeight.Medium),
        Font(R.font.misans_semibold, FontWeight.SemiBold),
        Font(R.font.misans_bold, FontWeight.Bold),
        Font(R.font.misans_heavy, FontWeight.ExtraBold)
    )
}

enum class FontType(val displayName: String, val fontFamily: FontFamily, val availableWeights: List<Int>) {
    SYSTEM("系统默认", FontFamily.Default, listOf(400)),
    MI_SANS("Mi Sans", CustomFonts.MiSans, listOf(100, 200, 300, 400, 500, 600, 700, 800)),
    LXGW_WENKAI("霞鹜文楷", CustomFonts.LXGWWenKai, listOf(300, 400, 500))
}
