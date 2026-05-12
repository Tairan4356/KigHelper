# Live Updates 通知实现说明

## 概述
本应用已实现 Android Live Updates 通知功能，让通知显示在通知中心的 Live Updates 区域，提供更突出的可见性。
通知现在包含**交互式按钮**，允许用户快速重播短语或打开应用。

## 核心功能

### 🔊 动态内容显示
- **标题**: 显示最后使用的短语（例如："你好"、"谢谢"）
- **内容**: 固定显示 "点击返回主界面"
- **状态芯片**: 在通知栏顶部显示短语文本
- **图标**: 使用音频/扬声器图标 🔊

### 🎯 交互式按钮
通知包含两个操作按钮：

1. **🔊 重播** - 使用 TTS 重新朗读最后的短语
   - 无需打开应用即可再次听到短语
   - 通过 BroadcastReceiver 在后台处理
   
2. **📱 打开应用** - 快速返回应用主界面
   - 直接跳转到应用，无需通过启动器

### 💾 持久化存储
- 自动保存最后使用的短语
- 应用重启后恢复上次的短语显示
- 使用 SharedPreferences 存储

## 实现要求 ✅

根据 Android 官方文档，Live Update 通知必须满足以下所有要求：

### 1. ✅ 通知样式
- **要求**: 必须是 Standard Style, BigTextStyle, CallStyle, ProgressStyle 或 MetricStyle
- **实现**: 使用 Standard Style（默认样式）

### 2. ✅ 清单权限
- **要求**: 必须在 AndroidManifest.xml 中请求 `android.permission.POST_PROMOTED_NOTIFICATIONS` 权限
- **实现**: 已添加到 `app/src/main/AndroidManifest.xml` 第 13 行

```xml
<uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS" />
```

### 3. ✅ 请求提升
- **要求**: 必须使用 `setRequestPromotedOngoing(true)` 请求提升
- **实现**: 在 `NotificationHelper.kt` 的 `buildNotification()` 方法中设置

```kotlin
.apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        setRequestPromotedOngoing(true)
    }
}
```

### 4. ✅ 持续通知
- **要求**: 必须设置 `FLAG_ONGOING_EVENT` (使用 `.setOngoing(true)`)
- **实现**: 已设置

```kotlin
.setOngoing(true)
```

### 5. ✅ 内容标题
- **要求**: 必须设置 contentTitle
- **实现**: 已设置为 "KigHelper 正在后台运行"

```kotlin
.setContentTitle("KigHelper 正在后台运行")
```

### 6. ✅ 无自定义视图
- **要求**: 不能使用 RemoteViews (customContentView)
- **实现**: 未使用任何自定义视图

### 7. ✅ 非分组摘要
- **要求**: 不能是分组通知的摘要 (不使用 setGroupSummary)
- **实现**: 未使用分组功能

### 8. ✅ 非彩色化
- **要求**: 不能设置 `setColorized(true)`
- **实现**: 未使用彩色化

### 9. ✅ 通知渠道重要性
- **要求**: 通道不能设置为 `IMPORTANCE_MIN`
- **实现**: 使用 `IMPORTANCE_HIGH`

```kotlin
NotificationManager.IMPORTANCE_HIGH
```

## 关键配置

### 通知类别
使用 `CATEGORY_SERVICE` 而不是 `CATEGORY_ALARM`，更符合 Live Updates 的持续运行特性：

```kotlin
.setCategory(NotificationCompat.CATEGORY_SERVICE)
```

### 状态芯片支持
设置 `setWhen()` 和 `setShowWhen(true)` 以在状态栏芯片中显示时间信息：

```kotlin
.setWhen(System.currentTimeMillis())
.setShowWhen(true)
```

### 调试日志
添加了日志输出以验证 Live Updates 功能：

```kotlin
if (Build.VERSION.SDK_INT >= 36) {
    val canPost = notificationManager.canPostPromotedNotifications()
    val hasCharacteristics = notification.hasPromotableCharacteristics()
    android.util.Log.d("NotificationHelper", 
        "Live Updates - canPost: $canPost, hasPromotableCharacteristics: $hasCharacteristics")
}
```

## 辅助方法

### 检查 Live Updates 权限
```kotlin
NotificationHelper.canPostLiveUpdates(context)
```
返回用户是否在设置中启用了 Live Updates。

### 打开 Live Updates 设置
```kotlin
NotificationHelper.openLiveUpdatesSettings(context)
```
打开系统设置页面，允许用户管理 Live Updates 权限。

## 使用场景

根据 Android 文档，Live Updates 应该用于：

### ✅ 适合的场景
- 正在进行的活动（有明确的开始和结束）
- 用户主动触发的操作
- 需要实时监控的任务

### ❌ 不适合的场景
- 广告或促销
- 聊天消息
- 警报通知
- 即将到来的日历事件
- 应用功能的快速访问

## 外观特征

提升后的 Live Update 通知具有以下特征：

1. **默认展开**: 通知会自动展开显示完整内容
2. **不可折叠**: 用户无法折叠通知
3. **更高可见性**: 
   - 显示在通知抽屉顶部
   - 显示在锁屏顶部
   - 在状态栏显示为芯片

## API 版本要求

- **最低要求**: Android 14 (API 34 / UPSIDE_DOWN_CAKE) 用于基本 Live Updates
- **完整功能**: Android 15 (API 36+) 用于高级 API（canPostPromotedNotifications 等）
- **向后兼容**: 在旧版本 Android 上正常显示为标准持续通知

## 🔧 测试建议

1. 在 Android 14+ 设备上测试通知是否显示在 Live Updates 区域
2. 检查 Logcat 输出验证 `hasPromotableCharacteristics` 返回 true
3. 在设置中验证 Live Updates 权限可以正常开关
4. 测试通知在锁屏和主屏的显示效果
5. 验证状态栏芯片的显示
6. **测试"重播"按钮** - 点击后 TTS 是否正常朗读短语
7. **测试"打开应用"按钮** - 是否正确跳转到应用主界面
8. **测试持久化** - 关闭应用后重新打开，通知是否显示最后使用的短语

## 参考文档

- [Android Live Updates 官方文档](https://developer.android.com/develop/ui/views/notifications/live-updates)
- [NotificationCompat API](https://developer.android.com/reference/androidx/core/app/NotificationCompat)

## 更新日期
2026年5月12日

