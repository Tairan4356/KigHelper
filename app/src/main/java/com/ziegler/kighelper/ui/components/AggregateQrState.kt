package com.ziegler.kighelper.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.socialshare.SocialCardQrCodec
import com.ziegler.kighelper.data.socialshare.SocialCardShareCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AggregateQrState(
    val result: SocialCardShareCodec.ShareEncodeResult, val bitmap: Bitmap?
) {
    val available: Boolean get() = result.url != null
    val overflow: Boolean get() = result.overflow
}

/**
 * 计算当前卡片的集合码 URL 与位图。
 *
 * 卡片条目、编辑页容量条与分享面板共用同一份编码结果，避免口径不一致。
 */
@Composable
fun rememberAggregateQrState(
    profile: SocialCardProfile, maxUrlBytes: Int = SocialCardShareCodec.MAX_QR_URL_BYTES
): AggregateQrState {
    val result = remember(profile, maxUrlBytes) {
        SocialCardShareCodec.encodeQr(profile, maxUrlBytes)
    }
    val bitmap by produceState<Bitmap?>(initialValue = null, result.url) {
        value = result.url?.let { url ->
            withContext(Dispatchers.Default) {
                SocialCardQrCodec.generate(url)
            }
        }
    }
    return remember(result, bitmap) { AggregateQrState(result, bitmap) }
}
