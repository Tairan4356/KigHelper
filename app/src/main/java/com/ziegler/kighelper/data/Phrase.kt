package com.ziegler.kighelper.data

data class Phrase(
    val id: String = java.util.UUID.randomUUID().toString(), val label: String, val speech: String
)