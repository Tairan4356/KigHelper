package com.ziegler.kighelper.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ziegler.kighelper.data.CardTemplates
import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.SocialContact
import com.ziegler.kighelper.data.SocialPlatformIcons
import com.ziegler.kighelper.ui.components.SocialCard
import java.io.File
import java.util.UUID

/**
 * 编辑社交卡片信息页面。
 *
 * 解耦：本页面只接收初始资料和保存回调，不直接持有 ViewModel；
 * 所有持久化操作由父级在 [onSave] 中完成。
 *
 * @param initialProfile 进入页面时的初始资料
 * @param onBack 返回回调
 * @param onSave 保存回调，传入更新后的资料 + 待写入的图片 URI
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SocialCardEditScreen(
    initialProfile: SocialCardProfile, onBack: () -> Unit, onSave: (
        profile: SocialCardProfile, avatarUri: Uri?, backgroundUri: Uri?, qrCodeUris: Map<String, Uri>, iconUris: Map<String, Uri>
    ) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current

    // 文本字段：使用 rememberSaveable 以在进程被回收后仍能恢复
    var nickname by rememberSaveable { mutableStateOf(initialProfile.nickname) }
    var signature by rememberSaveable { mutableStateOf(initialProfile.signature) }

    // 图片相关：使用 remember，Uri 难以序列化
    var avatarPath by remember { mutableStateOf(initialProfile.avatarPath) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var templateIndex by rememberSaveable { mutableIntStateOf(initialProfile.templateIndex) }
    var customBackgroundPath by remember { mutableStateOf(initialProfile.customBackgroundPath) }
    var backgroundUri by remember { mutableStateOf<Uri?>(null) }

    val contacts = remember {
        mutableStateListOf<SocialContact>().apply { addAll(initialProfile.contacts) }
    }
    val qrCodeUris = remember { mutableStateMapOf<String, Uri>() }
    val iconUris = remember { mutableStateMapOf<String, Uri>() }
    // 临时移除自定义图标的 contactId 集合（保存时通知 Repository 清理）
    val removedIconIds = remember { mutableStateListOf<String>() }

    var pendingQrContactId by remember { mutableStateOf<String?>(null) }
    var pendingIconContactId by remember { mutableStateOf<String?>(null) }

    val pickAvatar =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                avatarUri = uri
                avatarPath = null
            }
        }
    val pickBackground =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                backgroundUri = uri
                customBackgroundPath = null
            }
        }
    val pickQrLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val contactId = pendingQrContactId
            if (uri != null && contactId != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                qrCodeUris[contactId] = uri
            }
            pendingQrContactId = null
        }
    val pickIconLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val contactId = pendingIconContactId
            if (uri != null && contactId != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                iconUris[contactId] = uri
                removedIconIds.remove(contactId)
                // 清掉旧的 customIconPath，避免预览时仍加载旧文件
                val idx = contacts.indexOfFirst { it.id == contactId }
                if (idx >= 0) contacts[idx] = contacts[idx].copy(customIconPath = null)
            }
            pendingIconContactId = null
        }

    var showAddContactDialog by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    fun handleSave() {
        // 把「移除自定义图标」的 contact 在 contacts 中清掉 customIconPath
        val finalContacts = contacts.map { c ->
            if (c.id in removedIconIds) c.copy(customIconPath = null) else c
        }
        val finalProfile = SocialCardProfile(
            nickname = nickname.trim(),
            signature = signature.trim(),
            avatarPath = avatarPath,
            templateIndex = templateIndex,
            customBackgroundPath = customBackgroundPath,
            contacts = finalContacts
        )
        onSave(finalProfile, avatarUri, backgroundUri, qrCodeUris.toMap(), iconUris.toMap())
        onBack()
    }

    /**
     * 检测当前编辑状态相对于 [initialProfile] 是否有任何未保存的修改。
     * 用于在返回时决定是否弹出二次确认弹窗。
     */
    fun hasUnsavedChanges(): Boolean {
        if (nickname.trim() != initialProfile.nickname) return true
        if (signature.trim() != initialProfile.signature) return true
        if (avatarPath != initialProfile.avatarPath) return true
        if (avatarUri != null) return true
        if (templateIndex != initialProfile.templateIndex) return true
        if (customBackgroundPath != initialProfile.customBackgroundPath) return true
        if (backgroundUri != null) return true

        // 联系人列表差异检测
        val current = contacts.toList()
        val initial = initialProfile.contacts
        if (current.size != initial.size) return true
        // 按 id 配对比较；顺序变化不算修改
        val initialById = initial.associateBy { it.id }
        current.forEach { c ->
            val other = initialById[c.id]
            if (other == null) return true
            if (c.displayName != other.displayName) return true
            if (c.iconKey != other.iconKey) return true
            if (c.handle != other.handle) return true
            // customIconPath 视为已修改：无论用户上传新图标还是主动移除
            if (c.customIconPath != other.customIconPath) return true
            // qrCodePath 若被新上传 URI 覆盖，视为已修改
            if (qrCodeUris[c.id] != null) return true
            if (c.qrCodePath != other.qrCodePath) return true
        }
        if (removedIconIds.isNotEmpty()) return true
        if (iconUris.isNotEmpty()) return true

        return false
    }

    fun handleBack() {
        if (hasUnsavedChanges()) showDiscardConfirm = true else onBack()
    }

    // 拦截系统返回键，与点击 TopAppBar 返回按钮行为一致
    BackHandler(enabled = true) { handleBack() }

    // 当前预览资料：把 Uri 转成字符串供 SocialCard 直接加载
    val previewProfile = SocialCardProfile(
        nickname = nickname,
        signature = signature,
        avatarPath = avatarPath ?: avatarUri?.toString(),
        templateIndex = templateIndex,
        customBackgroundPath = customBackgroundPath ?: backgroundUri?.toString(),
        contacts = contacts.map { c ->
            val pendingQr = qrCodeUris[c.id]
            val pendingIcon = iconUris[c.id]
            c.copy(
                qrCodePath = pendingQr?.toString() ?: c.qrCodePath,
                customIconPath = pendingIcon?.toString() ?: c.customIconPath
            )
        })

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(
                title = { Text("编辑卡片") }, navigationIcon = {
                IconButton(onClick = ::handleBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }, actions = {
                IconButton(onClick = ::handleSave) {
                    Icon(Icons.Filled.Check, "保存")
                }
            }, scrollBehavior = scrollBehavior
            )
        }) { padding ->
        val navigationBarPadding =
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp + navigationBarPadding)
        ) {
            // 实时预览
            SocialCard(profile = previewProfile, showEditHint = false)
            Spacer(Modifier.height(16.dp))

            // ===== 基本信息 =====
            SectionTitle("基本信息")
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("昵称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = signature,
                onValueChange = { signature = it },
                label = { Text("个性签名") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // 头像
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { pickAvatar.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    val avatarModel: Any? = avatarPath?.let { File(it) } ?: avatarUri
                    if (avatarModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(avatarModel).build(),
                            contentDescription = "头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "添加头像",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("头像", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "点击图片更换",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (avatarPath != null || avatarUri != null) {
                        TextButton(onClick = {
                            avatarPath = null
                            avatarUri = null
                        }) { Text("移除头像") }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ===== 模板 =====
            SectionTitle("卡片模板")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(CardTemplates.presets.size) { index ->
                    val template = CardTemplates.presets[index]
                    TemplateThumb(
                        name = template.name,
                        brush = template.brush,
                        selected = templateIndex == index,
                        onClick = {
                            templateIndex = index
                            customBackgroundPath = null
                            backgroundUri = null
                        })
                }
                item {
                    val hasCustomBg = customBackgroundPath != null || backgroundUri != null
                    TemplateThumb(
                        name = "自定义",
                        brush = null,
                        customImageModel = customBackgroundPath?.let { File(it) } ?: backgroundUri,
                        selected = templateIndex == SocialCardProfile.CUSTOM_TEMPLATE_INDEX,
                        onClick = {
                            templateIndex = SocialCardProfile.CUSTOM_TEMPLATE_INDEX
                            if (!hasCustomBg) pickBackground.launch(arrayOf("image/*"))
                        })
                }
            }
            if (templateIndex == SocialCardProfile.CUSTOM_TEMPLATE_INDEX && customBackgroundPath == null && backgroundUri == null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { pickBackground.launch(arrayOf("image/*")) }) {
                    Text("选择背景图")
                }
            }

            Spacer(Modifier.height(24.dp))

            // ===== 社交平台（列表方式） =====
            SectionTitle("社交平台")
            contacts.forEach { contact ->
                ContactEditor(
                    contact = contact,
                    qrPreviewModel = qrCodeUris[contact.id] ?: contact.qrCodePath?.let { File(it) },
                    iconPreviewModel = iconUris[contact.id]
                        ?: contact.customIconPath?.let { File(it) },
                    onRename = { newName ->
                        val idx = contacts.indexOfFirst { it.id == contact.id }
                        if (idx >= 0) contacts[idx] = contacts[idx].copy(displayName = newName)
                    },
                    onIconKeyChange = { newKey ->
                        val idx = contacts.indexOfFirst { it.id == contact.id }
                        if (idx >= 0) {
                            contacts[idx] = contacts[idx].copy(iconKey = newKey)
                            iconUris.remove(contact.id)
                            removedIconIds.remove(contact.id)
                            if (contacts[idx].customIconPath != null) {
                                contacts[idx] = contacts[idx].copy(customIconPath = null)
                                removedIconIds.add(contact.id)
                            }
                        }
                    },
                    onUploadIcon = {
                        pendingIconContactId = contact.id
                        pickIconLauncher.launch(arrayOf("image/*"))
                    },
                    onRemoveCustomIcon = {
                        iconUris.remove(contact.id)
                        val idx = contacts.indexOfFirst { it.id == contact.id }
                        if (idx >= 0) {
                            contacts[idx] = contacts[idx].copy(customIconPath = null)
                        }
                        removedIconIds.add(contact.id)
                    },
                    onHandleChange = { newHandle ->
                        val idx = contacts.indexOfFirst { it.id == contact.id }
                        if (idx >= 0) contacts[idx] = contacts[idx].copy(handle = newHandle)
                    },
                    onPickQr = {
                        pendingQrContactId = contact.id
                        pickQrLauncher.launch(arrayOf("image/*"))
                    },
                    onRemoveQr = {
                        qrCodeUris.remove(contact.id)
                        val idx = contacts.indexOfFirst { it.id == contact.id }
                        if (idx >= 0) contacts[idx] = contacts[idx].copy(qrCodePath = null)
                    },
                    onDelete = {
                        qrCodeUris.remove(contact.id)
                        iconUris.remove(contact.id)
                        removedIconIds.remove(contact.id)
                        contacts.removeAll { it.id == contact.id }
                    })
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showAddContactDialog = true }, modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("添加平台")
            }
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onConfirm = { name, iconKey ->
                contacts.add(
                    SocialContact(
                        id = UUID.randomUUID().toString(), displayName = name, iconKey = iconKey
                    )
                )
                showAddContactDialog = false
            })
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("放弃修改？") },
            text = { Text("你有未保存的修改，确定要离开吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onBack()
                }) { Text("放弃修改") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("继续编辑") }
            })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun TemplateThumb(
    name: String,
    brush: Brush?,
    selected: Boolean,
    onClick: () -> Unit,
    customImageModel: Any? = null
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .size(width = 80.dp, height = 120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 100.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
        ) {
            when {
                brush != null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush)
                )

                customImageModel != null -> AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(customImageModel)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                else -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ContactEditor(
    contact: SocialContact,
    qrPreviewModel: Any?,
    iconPreviewModel: Any?,
    onRename: (String) -> Unit,
    onIconKeyChange: (String) -> Unit,
    onUploadIcon: () -> Unit,
    onRemoveCustomIcon: () -> Unit,
    onHandleChange: (String) -> Unit,
    onPickQr: () -> Unit,
    onRemoveQr: () -> Unit,
    onDelete: () -> Unit
) {
    var showIconPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameValue by remember(contact.id) { mutableStateOf(contact.displayName) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 平台图标（点击切换图标）
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { showIconPicker = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when {
                            iconPreviewModel != null -> AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(iconPreviewModel).build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            contact.iconKey != SocialPlatformIcons.KEY_DEFAULT -> Icon(
                                painter = painterResource(SocialPlatformIcons.iconRes(contact.iconKey)),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )

                            else -> Text(
                                text = contact.displayName.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    contact.displayName.ifBlank { "(未命名)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "重命名")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = contact.handle,
                onValueChange = onHandleChange,
                label = { Text("账号/ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (qrPreviewModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(qrPreviewModel)
                            .build(),
                        contentDescription = "二维码",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = onPickQr) { Text("更换二维码") }
                    Spacer(Modifier.size(4.dp))
                    TextButton(onClick = onRemoveQr) { Text("移除") }
                } else {
                    OutlinedButton(onClick = onPickQr) {
                        Icon(
                            Icons.Filled.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("上传二维码")
                    }
                }
            }
        }
    }

    if (showIconPicker) {
        IconPickerSheet(
            currentKey = contact.iconKey,
            hasCustomIcon = iconPreviewModel != null,
            onDismiss = { showIconPicker = false },
            onPick = { key ->
                onIconKeyChange(key)
                showIconPicker = false
            },
            onUploadIcon = {
                showIconPicker = false
                onUploadIcon()
            },
            onRemoveCustomIcon = {
                showIconPicker = false
                onRemoveCustomIcon()
            })
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("修改平台名称") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    label = { Text("平台名称") })
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameValue.isNotBlank()) onRename(renameValue.trim())
                    showRenameDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("取消") }
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun IconPickerSheet(
    currentKey: String,
    hasCustomIcon: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onUploadIcon: () -> Unit,
    onRemoveCustomIcon: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "选择图标",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 上传自定义图标
                AssistChip(
                    onClick = onUploadIcon, label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("上传图标")
                        }
                    }, colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // 若已有自定义图标，提供移除按钮
                if (hasCustomIcon) {
                    AssistChip(
                        onClick = onRemoveCustomIcon, label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("移除自定义")
                            }
                        })
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "预设图标",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SocialPlatformIcons.selectableKeys.forEach { key ->
                    FilterChip(selected = key == currentKey, onClick = { onPick(key) }, label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(SocialPlatformIcons.iconRes(key)),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                when (key) {
                                    SocialPlatformIcons.KEY_QQ -> "QQ"
                                    SocialPlatformIcons.KEY_WECHAT -> "微信"
                                    SocialPlatformIcons.KEY_BILIBILI -> "B站"
                                    SocialPlatformIcons.KEY_DOUYIN -> "抖音"
                                    SocialPlatformIcons.KEY_WEIBO -> "微博"
                                    SocialPlatformIcons.KEY_X -> "X"
                                    SocialPlatformIcons.KEY_FACEBOOK -> "Facebook"
                                    SocialPlatformIcons.KEY_INSTAGRAM -> "Instagram"
                                    SocialPlatformIcons.KEY_TELEGRAM -> "Telegram"
                                    else -> "默认"
                                }
                            )
                        }
                    })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit, onConfirm: (name: String, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf(SocialPlatformIcons.KEY_DEFAULT) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("添加平台") }, text = {
        Column {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("平台名称") },
                placeholder = { Text("如：微博、小红书") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text("选择图标", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SocialPlatformIcons.selectableKeys.forEach { key ->
                    AssistChip(
                        onClick = { selectedIconKey = key },
                        label = {
                            Icon(
                                painter = painterResource(SocialPlatformIcons.iconRes(key)),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = if (selectedIconKey == key) AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) else AssistChipDefaults.assistChipColors()
                    )
                }
            }
        }
    }, confirmButton = {
        TextButton(
            onClick = {
                if (name.isNotBlank()) onConfirm(name.trim(), selectedIconKey)
            }) { Text("添加") }
    }, dismissButton = {
        TextButton(onClick = onDismiss) { Text("取消") }
    })
}
