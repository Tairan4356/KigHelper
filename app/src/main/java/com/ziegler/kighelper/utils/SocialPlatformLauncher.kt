package com.ziegler.kighelper.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import com.ziegler.kighelper.data.SocialContact
import com.ziegler.kighelper.data.SocialPlatformIcons
import com.ziegler.kighelper.utils.SocialPlatformLauncher.launchMain
import com.ziegler.kighelper.utils.SocialPlatformLauncher.launchScan
import com.ziegler.kighelper.utils.SocialPlatformLauncher.platforms
import com.ziegler.kighelper.utils.SocialPlatformLauncher.supportsMain

/**
 * 社交平台外部跳转工具。
 *
 * 把「平台图标键 -> 包名 / URL scheme」的映射集中维护，UI 层只负责调用
 * [launchMain] / [launchScan]，不再散落地硬编码第三方包名与 scheme，
 * 实现 UI 与平台知识的解耦。
 *
 * 新增平台只需在 [platforms] 中补充一行 [PlatformTarget]，
 * 并同步在 manifest `<queries>` 中追加对应 `<package>` 即可。
 */
object SocialPlatformLauncher {

    /**
     * 单个平台的跳转信息。
     *
     * @param packageName 目标 app 包名，用于 `setPackage` 限定
     * @param scanScheme  扫码 URL scheme；null 表示该平台暂无已知稳定扫码端点
     */
    private data class PlatformTarget(
        val packageName: String, val scanScheme: String? = null
    )

    /**
     * 平台图标键 -> 跳转信息。
     * 自定义平台（`KEY_DEFAULT`）不在表中，调用 [supportsMain] 返回 false，
     * UI 应禁用对应按钮。
     */
    private val platforms: Map<String, PlatformTarget> = mapOf(
        SocialPlatformIcons.KEY_WECHAT to PlatformTarget(
            packageName = "com.tencent.mm"
            // 微信不使用 Scheme，通过特殊显式 Intent 处理
        ), SocialPlatformIcons.KEY_QQ to PlatformTarget(
            packageName = "com.tencent.mobileqq"
        ), SocialPlatformIcons.KEY_BILIBILI to PlatformTarget(
            packageName = "tv.danmaku.bili", scanScheme = "bilibili://qrcode"
        ), SocialPlatformIcons.KEY_DOUYIN to PlatformTarget(
            packageName = "com.ss.android.ugc.aweme", scanScheme = "snssdk1128://scan"
        ), SocialPlatformIcons.KEY_WEIBO to PlatformTarget(
            packageName = "com.sina.weibo", scanScheme = "sinaweibo://qrcode"
        ), SocialPlatformIcons.KEY_X to PlatformTarget(
            packageName = "com.twitter.android"
        ), SocialPlatformIcons.KEY_FACEBOOK to PlatformTarget(
            packageName = "com.facebook.katana"
        ), SocialPlatformIcons.KEY_INSTAGRAM to PlatformTarget(
            packageName = "com.instagram.android"
        ), SocialPlatformIcons.KEY_TELEGRAM to PlatformTarget(
            packageName = "org.telegram.messenger.web"
        )
    )

    /** 该平台是否支持跳转主界面。 */
    fun supportsMain(contact: SocialContact): Boolean = contact.iconKey in platforms

    /**
     * 该平台是否支持跳转扫码。
     * 微信虽然没有 scanScheme，但在逻辑上我们支持微信扫码，因此特殊处理返回 true。
     */
    fun supportsScan(contact: SocialContact): Boolean {
        if (contact.iconKey == SocialPlatformIcons.KEY_WECHAT) return true
        return platforms[contact.iconKey]?.scanScheme != null
    }

    /**
     * 拉起 [contact] 对应平台的主界面。
     * @return true 表示成功发起启动；false 表示不支持或目标应用未安装。
     */
    fun launchMain(context: Context, contact: SocialContact): Boolean {
        val target = platforms[contact.iconKey] ?: return false
        return try {
            val launchIntent =
                context.packageManager.getLaunchIntentForPackage(target.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 拉起 [contact] 对应平台的扫码界面。
     * @return true 表示成功发起启动；false 表示不支持、未安装或对应端点失效。
     */
    fun launchScan(context: Context, contact: SocialContact): Boolean {
        val target = platforms[contact.iconKey] ?: return false

        // 尝试直接拉起该应用的扫码界面
        val success = if (contact.iconKey == SocialPlatformIcons.KEY_WECHAT) {
            launchWeChatScanSpecial(context)
        } else {
            val scanScheme = target.scanScheme
            if (scanScheme != null) {
                startSafely(context, target.packageName, scanScheme)
            } else {
                false
            }
        }

        // 兜底机制：如果拉起直接扫码页被限制/失败，退而求其次拉起应用的主界面
        if (!success) {
            return launchMain(context, contact)
        }
        return true
    }

    /**
     * 针对微信扫码的特殊通道（绕过已封锁的 weixin:// Scheme）
     */
    private fun launchWeChatScanSpecial(context: Context): Boolean = try {
        val intent = Intent().apply {
            component = ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
            putExtra("LauncherUI.From.Scaner.Shortcut", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            action = Intent.ACTION_VIEW
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }

    /**
     * 动态解析并安全拉起其他 Scheme
     */
    private fun startSafely(
        context: Context, packageName: String, scheme: String
    ): Boolean = try {
        val uri = scheme.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // 动态查询系统里有哪些 Activity 注册并支持响应该 Scheme
        val resolveInfos = context.packageManager.queryIntentActivities(
            intent, PackageManager.MATCH_DEFAULT_ONLY
        )

        // 过滤出属于目标应用的 Activity 节点
        val targetActivity = resolveInfos.firstOrNull {
            it.activityInfo.packageName.equals(packageName, ignoreCase = true)
        }

        if (targetActivity != null) {
            // 查到了确切的导出接收 Activity，升级为显式 Component 跳转，避免隐式 Scheme 被拦截
            intent.component = ComponentName(packageName, targetActivity.activityInfo.name)
            context.startActivity(intent)
            true
        } else {
            // 兜底方案：如果因为动态解析限制无法查到（但应用可能存在），盲发 setPackage
            intent.setPackage(packageName)
            context.startActivity(intent)
            true
        }
    } catch (_: Exception) {
        false
    }
}