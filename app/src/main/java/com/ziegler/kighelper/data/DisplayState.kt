package com.ziegler.kighelper.data

data class DisplayState(
    val text: String = "",
    val isInitialHint: Boolean = true,
    val imagePath: String? = null,
    val videoPath: String? = null
)