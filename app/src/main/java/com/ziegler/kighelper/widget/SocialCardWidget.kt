package com.ziegler.kighelper.widget

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.currentState
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ziegler.kighelper.data.CardTemplates
import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.SocialPlatformIcons
import com.google.gson.Gson
import com.ziegler.kighelper.data.SocialContact
import androidx.core.graphics.get
import androidx.core.graphics.toColorInt

class SocialCardWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val profile = loadProfile(context)

        provideContent {
            GlanceTheme {
                // Read from the widget's Glance state: update() does not re-run provideGlance
                // on a running session, it only reloads this state and recomposes. Values read
                // outside the composition (like profile above) would stay stale, so the
                // selected index must be read here via currentState().
                val selectedIndex = currentState(PlatformClickAction.SELECTED_PLATFORM_INDEX) ?: 0
                SocialCardContent(profile, selectedIndex, context)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun SocialCardContent(
        profile: SocialCardProfile, selectedIndex: Int, appContext: Context
    ) {
        val size = LocalSize.current
        val context = LocalContext.current
        val bgColor = resolveBackgroundColor(profile, appContext)
        val textColor = resolveTextColor(profile)

        val visibleContacts = profile.visibleContacts
        val safeIndex = selectedIndex.coerceIn(0, visibleContacts.lastIndex.coerceAtLeast(0))
        val selectedContact = visibleContacts.getOrNull(safeIndex)

        // 4x2: only name+avatar; 4x4+: show platform icons; larger: show QR
        val showPlatformIcons = size.height >= 180.dp
        val showQrCode = size.height >= 340.dp

        Box(
            modifier = GlanceModifier.fillMaxSize().background(bgColor).cornerRadius(24.dp)
                .clickable(
                    actionStartActivity(
                        ComponentName(context.packageName, "${context.packageName}.MainActivity")
                    )
                ).padding(24.dp), contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Row 1: Name column (left) + Avatar (right) ──
                // Matches SocialCard: Row > Column(weight=1, size=128dp) + Spacer(20dp) + Image(96dp)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Name column: matches SocialCard's Column(weight=1, size=128dp)
                    Column(
                        modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.Top
                    ) {
                        val hasOtherContent =
                            profile.signature.isNotBlank() || profile.avatarPath != null || visibleContacts.isNotEmpty()
                        val showPlaceholder = profile.nickname.isBlank() && !hasOtherContent
                        val nicknameText = if (showPlaceholder) "扩列卡片" else profile.nickname

                        if (nicknameText.isNotBlank()) {
                            // Exact font sizes from SocialCard
                            val nicknameFontSize = when {
                                nicknameText.length <= 3 -> 48.sp
                                profile.signature.isBlank() && nicknameText.length <= 8 -> 36.sp
                                nicknameText.length <= 6 -> 36.sp
                                nicknameText.length <= 8 -> 24.sp
                                profile.signature.isBlank() || nicknameText.length <= 10 -> 24.sp
                                else -> 12.sp
                            }
                            Text(
                                text = nicknameText, style = TextStyle(
                                    color = ColorProvider(textColor),
                                    fontSize = nicknameFontSize,
                                    fontWeight = FontWeight.Bold
                                ), maxLines = if (profile.signature.isBlank()) 2 else 1
                            )
                        }
                        // Spacer(6.dp) + signature
                        if (profile.signature.isNotBlank()) {
                            Spacer(modifier = GlanceModifier.height(6.dp))
                            Text(
                                text = profile.signature, style = TextStyle(
                                    color = ColorProvider(textColor.copyAlpha(0.9f)),
                                    fontSize = 16.sp
                                ), maxLines = 2
                            )
                        }
                    }

                    // Spacer(20.dp) + Avatar 96dp
                    Spacer(modifier = GlanceModifier.width(20.dp))
                    val avatarBitmap = profile.avatarPath?.let {
                        WidgetBitmapLoader.loadCircularBitmap(context, it, 256)
                    }
                    if (avatarBitmap != null) {
                        Image(
                            provider = ImageProvider(avatarBitmap),
                            contentDescription = "头像",
                            modifier = GlanceModifier.size(96.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // ── Row 2: Platform icons ──
                if (showPlatformIcons && visibleContacts.isNotEmpty()) {
                    // Spacer(20.dp) before platform icons
                    Spacer(modifier = GlanceModifier.height(20.dp))
                    PlatformIconSection(visibleContacts, safeIndex, textColor)
                }

                // ── Row 3: QR code fills remaining height ──
                if (showQrCode && selectedContact != null) {
                    // Spacer(32.dp) before QR code (matches SocialCard)
                    Spacer(modifier = GlanceModifier.height(20.dp))
                    QrCodeSection(selectedContact, textColor, context)
                }
            }
        }
    }

    /**
     * Platform icon row: each icon is a 52dp circle, icon inside is 32dp.
     * Matches SocialCard's ContactIcon exactly:
     * - selected: white border 2dp, background 0x99FFFFFF, icon original color
     * - unselected: no border, background 0x55FFFFFF, icon tint 0xFF404040
     * - fallback: first character text, fontSize 20sp, FontWeight.Bold
     */
    @SuppressLint("RestrictedApi")
    @Composable
    private fun PlatformIconSection(
        contacts: List<SocialContact>, selectedIndex: Int, textColor: ComposeColor
    ) {
        val context = LocalContext.current
        val displayContacts = contacts.take(4)

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            displayContacts.forEachIndexed { contactIndex, contact ->
                val indexInList = contacts.indexOf(contact)
                val isSelected = indexInList == selectedIndex
                val iconBitmap = resolveContactIcon(context, contact)

                // 56dp circle icon container
                Box(
                    modifier = GlanceModifier.size(64.dp).cornerRadius(32.dp).background(
                        if (isSelected) ComposeColor(0x99FFFFFF)
                        else ComposeColor(0x55FFFFFF)
                    ).clickable(
                        actionRunCallback<PlatformClickAction>(
                            actionParametersOf(
                                PlatformClickAction.PLATFORM_INDEX to indexInList
                            )
                        )
                    ), contentAlignment = Alignment.Center
                ) {
                    if (iconBitmap != null) {
                        // 32dp icon
                        Image(
                            provider = ImageProvider(iconBitmap),
                            contentDescription = contact.displayName,
                            modifier = GlanceModifier.size(48.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Fallback: first character, 32sp Bold
                        Text(
                            text = contact.displayName.firstOrNull()?.toString() ?: "?",
                            style = TextStyle(
                                color = ColorProvider(
                                    if (isSelected) textColor else ComposeColor(0xFF404040)
                                ), fontSize = 32.sp, fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (contactIndex < displayContacts.lastIndex) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                }
            }
            // Empty slot spacers with weight to match SpaceBetween distribution
            repeat(4 - displayContacts.size) {
                Spacer(modifier = GlanceModifier.defaultWeight())
            }
        }
    }

    /**
     * QR code section: square QR box fills remaining height, caption below.
     * QR box: rounded 8dp, background 0x33FFFFFF, image Crop fill.
     * Below: platform name (16sp Medium) + handle (14sp 0.85f alpha).
     * Spacer(16.dp) between QR and caption.
     */
    @SuppressLint("RestrictedApi")
    @Composable
    private fun QrCodeSection(
        contact: SocialContact, textColor: ComposeColor, context: Context
    ) {
        val size = LocalSize.current
        // content area = total - 48dp padding (24 each side)
        val contentWidth = size.width - 48.dp
        // Overhead: name row (96dp) + spacer (20dp) + icons (56dp) + spacer (32dp) = 204dp
        // Caption: spacer (16dp) + name line (20dp) + spacer (4dp) + handle line (18dp) ≈ 58dp
        val qrMaxHeight = size.height - 48.dp - 200.dp - 58.dp
        val qrSize = maxOf(0.dp, minOf(contentWidth, qrMaxHeight))

        Column(
            modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Top
        ) {
            val qrBitmap = contact.qrCodePath?.let {
                WidgetBitmapLoader.loadBitmap(context, it, 512)
            }

            // Square QR box
            Box(
                modifier = GlanceModifier.size(qrSize).cornerRadius(12.dp)
                    .background(ComposeColor(0x33FFFFFF)), contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        provider = ImageProvider(qrBitmap),
                        contentDescription = "${contact.displayName} 二维码",
                        modifier = GlanceModifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "无二维码", style = TextStyle(
                            color = ColorProvider(textColor.copyAlpha(0.7f)), fontSize = 12.sp
                        )
                    )
                }
            }

            // Spacer(16.dp) + caption
            Spacer(modifier = GlanceModifier.height(16.dp))
            Text(
                text = contact.displayName.ifBlank { "社交平台" }, style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ), maxLines = 1
            )
            if (contact.handle.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = contact.handle, style = TextStyle(
                        color = ColorProvider(textColor.copyAlpha(0.85f)), fontSize = 14.sp
                    ), maxLines = 1
                )
            }
        }
    }

    private fun resolveContactIcon(
        context: Context, contact: SocialContact
    ): Bitmap? {
        val customPath = contact.customIconPath
        if (!customPath.isNullOrBlank()) {
            return WidgetBitmapLoader.loadBitmap(context, customPath, 64)
        }
        val iconKey = contact.iconKey
        if (iconKey == SocialPlatformIcons.KEY_DEFAULT) return null
        val resId = SocialPlatformIcons.iconRes(iconKey)
        return WidgetBitmapLoader.getDrawableAsBitmap(context, resId, 64)
    }

    private fun resolveBackgroundColor(
        profile: SocialCardProfile, appContext: Context
    ): ComposeColor {
        val intColor = when {
            profile.templateIndex == SocialCardProfile.CUSTOM_TEMPLATE_INDEX && !profile.customBackgroundPath.isNullOrBlank() -> {
                val bg =
                    WidgetBitmapLoader.loadBitmap(appContext, profile.customBackgroundPath, 256)
                if (bg != null) {
                    val pixel = bg[bg.width / 2, bg.height / 2]
                    bg.recycle()
                    pixel
                } else {
                    "#6650A4".toColorInt()
                }
            }

            profile.templateIndex == SocialCardProfile.ADAPTIVE_TEMPLATE_INDEX -> {
                "#6750A4".toColorInt()
            }

            profile.templateIndex == SocialCardProfile.CUSTOM_COLOR_TEMPLATE_INDEX -> {
                profile.customColor.toInt()
            }

            else -> {
                val index = profile.templateIndex.coerceIn(0, CardTemplates.presets.lastIndex)
                val preset = CardTemplates.presets[index]
                val r = (preset.background.red * 255).toInt()
                val g = (preset.background.green * 255).toInt()
                val b = (preset.background.blue * 255).toInt()
                Color.rgb(r, g, b)
            }
        }
        return ComposeColor(intColor)
    }

    /**
     * Text color logic matching SocialCard's cardTextColor exactly:
     * 1. Custom background image → always White
     * 2. Adaptive template → luminance of primary (#6750A4) → dark or white
     * 3. Custom color → luminance of custom color → dark or white
     * 4. Preset template → use CardTemplates.presets.onBackground
     */
    private fun resolveTextColor(profile: SocialCardProfile): ComposeColor {
        return when {
            profile.templateIndex == SocialCardProfile.CUSTOM_TEMPLATE_INDEX && !profile.customBackgroundPath.isNullOrBlank() -> {
                ComposeColor.White
            }

            profile.templateIndex == SocialCardProfile.ADAPTIVE_TEMPLATE_INDEX -> {
                val primary = ComposeColor(0xFF6750A4)
                val luminance =
                    0.299f * primary.red + 0.587f * primary.green + 0.114f * primary.blue
                if (luminance > 0.5f) ComposeColor(0xFF1C1B1F) else ComposeColor.White
            }

            profile.templateIndex == SocialCardProfile.CUSTOM_COLOR_TEMPLATE_INDEX -> {
                val customColor = ComposeColor(profile.customColor)
                val luminance =
                    0.299f * customColor.red + 0.587f * customColor.green + 0.114f * customColor.blue
                if (luminance > 0.5f) ComposeColor(0xFF1C1B1F) else ComposeColor.White
            }

            else -> {
                val index = profile.templateIndex.coerceIn(0, CardTemplates.presets.lastIndex)
                val preset = CardTemplates.presets[index]
                val r = (preset.onBackground.red * 255).toInt()
                val g = (preset.onBackground.green * 255).toInt()
                val b = (preset.onBackground.blue * 255).toInt()
                ComposeColor(Color.rgb(r, g, b))
            }
        }
    }

    private fun loadProfile(context: Context): SocialCardProfile {
        return try {
            val prefs = context.getSharedPreferences("social_card_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("profile_json", null) ?: return SocialCardProfile.DEFAULT
            Gson().fromJson(json, SocialCardProfile::class.java) ?: SocialCardProfile.DEFAULT
        } catch (_: Exception) {
            SocialCardProfile.DEFAULT
        }
    }

    private fun ComposeColor.copyAlpha(alpha: Float): ComposeColor {
        return ComposeColor(red, green, blue, alpha)
    }
}
