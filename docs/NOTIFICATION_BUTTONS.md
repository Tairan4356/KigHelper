# 交互式通知按钮功能说明

## 📱 通知外观

```
┌─────────────────────────────────────────┐
│  🔊  谢谢你                             │  ← 标题（最后使用的短语）
│      点击返回主界面                     │  ← 内容文本（固定）
│                                         │
│  ┌─────────────┐  ┌─────────────┐     │
│  │  🔊  重播   │  │  📱 打开应用 │     │  ← 操作按钮
│  └─────────────┘  └─────────────┘     │
│                                         │
│  状态芯片: "谢谢你" 🔊                  │  ← 状态栏芯片
└─────────────────────────────────────────┘
```

## ✨ 功能说明

### 🔊 重播按钮
**功能**: 重新播放最后使用的短语
- **触发**: 点击"重播"按钮
- **处理**: `NotificationActionReceiver` 接收广播
- **执行**: 
  1. 创建临时 TTS 实例
  2. 设置中文语言
  3. 播放短语内容
  4. 5秒后自动关闭 TTS

**优势**:
- ✅ 无需打开应用
- ✅ 后台立即播放
- ✅ 适合需要反复听的场景

### 📱 打开应用按钮
**功能**: 快速返回应用主界面
- **触发**: 点击"打开应用"按钮
- **处理**: 使用 PendingIntent 启动 MainActivity
- **特性**: 
  - 复用已有 Activity（FLAG_ACTIVITY_SINGLE_TOP）
  - 无需从启动器重新打开
  - 保持应用状态

## 🔄 工作流程

### 用户操作短语
```
用户点击短语按钮（例如："谢谢"）
    ↓
TTS 朗读 "谢谢你"
    ↓
保存到 SharedPreferences（最后使用的短语）
    ↓
更新通知
    ├─ 标题: "谢谢"
    ├─ 芯片: "谢谢"
    ├─ 重播按钮: 启用（携带 "谢谢你" 文本）
    └─ 打开应用按钮: 可用
```

### 用户点击重播按钮
```
点击通知中的"重播"按钮
    ↓
发送广播: ACTION_REPLAY_PHRASE
    ↓
NotificationActionReceiver 接收
    ↓
创建 TTS 实例 → 设置语言 → 播放 "谢谢你"
    ↓
5秒后自动清理 TTS 资源
```

### 用户点击打开应用按钮
```
点击通知中的"打开应用"按钮
    ↓
PendingIntent 触发
    ↓
MainActivity 启动（单例模式）
    ↓
应用回到前台
```

## 💻 代码实现

### NotificationHelper.kt
```kotlin
// 添加操作按钮
if (!phraseText.isNullOrEmpty()) {
    val replayIntent = createReplayPendingIntent(context, phraseText)
    builder.addAction(
        R.drawable.ic_speaker,
        "重播",
        replayIntent
    )
}

builder.addAction(
    R.drawable.ic_launcher_foreground,
    "打开应用",
    pendingIntent
)
```

### NotificationActionReceiver.kt
```kotlin
class NotificationActionReceiver : BroadcastReceiver() {
    private var tts: TextToSpeech? = null
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REPLAY_PHRASE -> {
                val phraseText = intent.getStringExtra(EXTRA_PHRASE_TEXT)
                replayPhrase(context, phraseText)
            }
        }
    }
}
```

### AndroidManifest.xml
```xml
<receiver
    android:name=".utils.NotificationActionReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="com.ziegler.kighelper.ACTION_REPLAY_PHRASE" />
    </intent-filter>
</receiver>
```

## 🎯 使用场景

### 适合使用重播按钮的场景
- 🗣️ 需要反复练习发音
- 👂 在嘈杂环境没听清
- 📚 教学或学习场景
- 🔁 需要多次确认的信息

### 适合使用打开应用按钮的场景
- ✏️ 想要编辑短语
- 🔍 查看其他短语选项
- ⚙️ 调整应用设置
- 📱 快速返回应用主界面

## 🔐 权限要求

### 已配置权限
- ✅ `POST_NOTIFICATIONS` - 发送通知（Android 13+）
- ✅ `POST_PROMOTED_NOTIFICATIONS` - Live Updates 显示（Android 14+）
- ✅ `USE_FULL_SCREEN_INTENT` - 锁屏显示

### BroadcastReceiver 配置
- ✅ `android:exported="false"` - 仅应用内使用，提高安全性
- ✅ Intent Filter - 明确指定可处理的 Action

## 📊 性能考虑

### TTS 资源管理
- **创建**: 仅在需要时创建实例
- **使用**: 播放完成后自动清理
- **超时**: 5秒后强制关闭，避免资源泄漏

### 通知更新频率
- **触发时机**: 仅在用户使用短语时更新
- **内容变更**: 只更新标题和芯片文本
- **按钮状态**: 重播按钮仅在有短语时显示

## 🐛 疑难解答

### 重播按钮不响应
1. 检查 `NotificationActionReceiver` 是否在 Manifest 中注册
2. 确认 Intent Action 字符串匹配
3. 查看 Logcat 是否有 TTS 初始化错误

### 通知不显示按钮
1. 确认 Android 版本支持 Action 按钮
2. 检查图标资源是否存在
3. 验证 PendingIntent FLAG 设置正确

### TTS 播放无声音
1. 检查设备 TTS 引擎是否安装
2. 确认应用是否有音频权限
3. 测试设备音量设置

## 更新日期
2026年5月12日

