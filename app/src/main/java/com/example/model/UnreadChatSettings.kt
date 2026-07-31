package com.example.model

data class UnreadChatSettings(
    val minUnreadCount: Int = 1,
    val processOrder: String = ORDER_TOP_TO_BOTTOM, // "TOP_TO_BOTTOM" or "BOTTOM_TO_TOP"
    val maxChatsToOpen: Int = 0, // 0 = unlimited
    val skipPinnedChats: Boolean = false,
    val skipMutedChats: Boolean = false,
    val autoScroll: Boolean = true,
    val stopAtEnd: Boolean = true
) {
    companion object {
        const val ORDER_TOP_TO_BOTTOM = "TOP_TO_BOTTOM"
        const val ORDER_BOTTOM_TO_TOP = "BOTTOM_TO_TOP"
    }
}
