package com.size.tools;

import cn.hutool.crypto.digest.DigestUtil;

public class MemoryChatClientTool {

    /* 不能把客户端传入的 conversationId 直接当成唯一记忆 Key，因为用户 A 可以猜测用户 B 的 ID。 */
    public static String memoryKey(String userId, String conversationId) {
        return DigestUtil.sha256Hex(userId + "\0" + conversationId);
    }
}
