package com.ziegler.kighelper.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.google.gson.Gson
import com.ziegler.kighelper.widget.SocialCardWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 个人社交卡片仓库。负责持久化 [SocialCardProfile] JSON 数据，
 * 并把头像、背景、二维码等图片复制到应用内部目录。
 *
 * 设计要点：
 * - 文本字段直接通过 [SocialCardProfile] 携带；
 * - 图片字段在 [commitProfile] 时统一由 URI 复制到内部存储并更新路径；
 * - UI 仅感知 [SocialCardProfile] 与 Repository 接口，不接触 SharedPreferences/文件系统。
 */
@Singleton
class SocialCardRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val gson: Gson = Gson()
    private val ioDispatcher = Dispatchers.IO

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfile())
    val profile: Flow<SocialCardProfile> = _profile.asStateFlow()

    val current: SocialCardProfile get() = _profile.value

    /**
     * 提交一份新的卡片资料。
     *
     * @param profile 用户编辑后的资料（图片字段中的路径可以是旧路径或为 null）
     * @param avatarUri 若非空，表示本次新选了头像，将被复制并覆盖旧文件
     * @param backgroundUri 若非空，表示本次新选了自定义背景
     * @param qrCodeUris contactId -> Uri，本次新上传二维码的对应关系
     * @param iconUris contactId -> Uri，本次新上传自定义图标的对应关系
     * @return 落库后的最终资料
     */
    suspend fun commitProfile(
        profile: SocialCardProfile,
        avatarUri: Uri?,
        backgroundUri: Uri?,
        qrCodeUris: Map<String, Uri>,
        iconUris: Map<String, Uri> = emptyMap()
    ): SocialCardProfile = withContext(ioDispatcher) {
        val dir = storageDir().apply { mkdirs() }

        // 头像：固定文件名，覆盖写入
        var newAvatarPath = profile.avatarPath
        if (avatarUri != null) {
            val ext = guessExtension(avatarUri) ?: DEFAULT_IMG_EXT
            val dest = File(dir, "$FILE_AVATAR.$ext")
            copyUriTo(avatarUri, dest)
            newAvatarPath = dest.absolutePath
        } else if (profile.avatarPath == null) {
            // 用户主动移除头像，清理磁盘残留
            deleteFilesWithPrefix(dir, FILE_AVATAR)
        }

        // 背景：固定文件名，覆盖写入
        var newBgPath = profile.customBackgroundPath
        if (backgroundUri != null) {
            val ext = guessExtension(backgroundUri) ?: DEFAULT_IMG_EXT
            val dest = File(dir, "$FILE_BACKGROUND.$ext")
            copyUriTo(backgroundUri, dest)
            newBgPath = dest.absolutePath
        } else if (profile.customBackgroundPath == null) {
            deleteFilesWithPrefix(dir, FILE_BACKGROUND)
        }

        // 联系方式：保留传入顺序，按需复制新二维码与新图标
        val remainingContactIds = profile.contacts.map { it.id }.toSet()
        val updatedContacts = profile.contacts.map { contact ->
            var updated = contact

            // 二维码
            val newQrUri = qrCodeUris[contact.id]
            if (newQrUri != null) {
                val ext = guessExtension(newQrUri) ?: DEFAULT_QR_EXT
                val dest = File(dir, "${contact.id}_qr.$ext")
                copyUriTo(newQrUri, dest)
                updated = updated.copy(qrCodePath = dest.absolutePath)
            }

            // 自定义图标
            val newIconUri = iconUris[contact.id]
            if (newIconUri != null) {
                val ext = guessExtension(newIconUri) ?: DEFAULT_IMG_EXT
                val dest = File(dir, "${contact.id}_icon.$ext")
                copyUriTo(newIconUri, dest)
                updated = updated.copy(customIconPath = dest.absolutePath)
            } else if (contact.customIconPath == null) {
                // 用户主动移除自定义图标，清理磁盘残留
                deleteFilesWithPrefix(dir, "${contact.id}_icon")
            }

            updated
        }

        // 清理已删除联系人的二维码与图标文件
        dir.listFiles { f ->
            val name = f.name
            name.endsWith("_qr.png") || name.endsWith("_qr.jpg") || name.endsWith("_qr.webp") || name.endsWith(
                "_icon.jpg"
            ) || name.endsWith("_icon.png") || name.endsWith("_icon.webp")
        }?.forEach { f ->
            // 文件名格式为 "{contactId}_qr.png" / "{contactId}_icon.jpg"
            // 取最后一个下划线之前的部分作为 owner；UUID 与预设 id 都不含下划线，
            // 即使含下划线，此方法也能正确保留 owner 全名
            val ownerId = f.name.substringBeforeLast('_')
            if (ownerId !in remainingContactIds) f.delete()
        }

        val final = profile.copy(
            avatarPath = newAvatarPath, customBackgroundPath = newBgPath, contacts = updatedContacts
        )

        prefs.edit(commit = true) {
            putString(KEY_PROFILE, gson.toJson(final))
        }
        _profile.value = final

        // 通知桌面小组件刷新
        SocialCardWidgetReceiver.updateAllWidgets(context)

        final
    }

    private fun loadProfile(): SocialCardProfile {
        val json = prefs.getString(KEY_PROFILE, null) ?: return SocialCardProfile.DEFAULT
        return runCatching {
            gson.fromJson(json, SocialCardProfile::class.java) ?: SocialCardProfile.DEFAULT
        }.getOrDefault(SocialCardProfile.DEFAULT)
    }

    private fun storageDir(): File = File(context.filesDir, STORAGE_DIR)

    private fun copyUriTo(uri: Uri, dest: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
    }

    private fun guessExtension(uri: Uri): String? {
        val mime = context.contentResolver.getType(uri) ?: return null
        return when (mime) {
            "image/png" -> "png"
            "image/jpeg", "image/jpg" -> "jpg"
            "image/webp" -> "webp"
            else -> null
        }
    }

    private fun deleteFilesWithPrefix(dir: File, prefix: String) {
        dir.listFiles { f -> f.name.startsWith(prefix) }?.forEach { it.delete() }
    }

    private companion object {
        const val PREFS_NAME = "social_card_prefs"
        const val KEY_PROFILE = "profile_json"

        const val STORAGE_DIR = "social_card"
        const val FILE_AVATAR = "avatar"
        const val FILE_BACKGROUND = "background"
        const val DEFAULT_IMG_EXT = "jpg"
        const val DEFAULT_QR_EXT = "png"
    }
}
