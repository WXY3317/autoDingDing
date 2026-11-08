package com.pengxh.autodingding.utils

import android.content.Context
import cn.jpush.android.api.JPushInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Local observations only; a historical connection event is not a live online check. */
object PushDiagnostics {
    private fun preferences(context: Context) =
        context.getSharedPreferences("push_diagnostics", Context.MODE_PRIVATE)

    private fun stamp() = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    fun connection(context: Context, connected: Boolean) {
        preferences(context).edit().putString("connection", "${stamp()} ${if (connected) "已连接" else "已断开"}").apply()
    }

    fun received(context: Context, command: String) {
        preferences(context).edit()
            .putString("received", "${stamp()} ${command.take(80)}")
            .putString("execution", "等待执行").apply()
    }

    fun execution(context: Context, state: String) {
        preferences(context).edit().putString("execution", "${stamp()} $state").apply()
    }

    fun mailNotification(context: Context, state: String) {
        preferences(context).edit().putString("mail_notification", "${stamp()} $state").apply()
    }

    fun phoneCall(context: Context, state: String) {
        preferences(context).edit().putString("phone_call", "${stamp()} $state").apply()
    }

    fun report(context: Context): String {
        val prefs = preferences(context)
        return """
            当前注册ID：${JPushInterface.getRegistrationID(context).takeUnless { it.isNullOrBlank() } ?: "注册中"}
            推送开关：${if (JPushInterface.isPushStopped(context)) "已停止" else "已启用"}
            最近连接回调：${prefs.getString("connection", "暂无记录")}
            最近收到指令：${prefs.getString("received", "暂无记录")}
            最近执行进度：${prefs.getString("execution", "暂无记录")}
            最近邮件通知：${prefs.getString("mail_notification", "暂无记录")}
            最近来电触发：${prefs.getString("phone_call", "暂无记录")}

            连接回调是历史记录，不代表此刻在线。收到指令的时间未更新，说明本机接收回调尚未触发；已更新则检查执行进度。
        """.trimIndent()
    }
}
