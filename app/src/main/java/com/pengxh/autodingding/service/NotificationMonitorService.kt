package com.pengxh.autodingding.service

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.drake.channel.sendEvent
import com.pengxh.autodingding.BaseApplication
import com.pengxh.autodingding.bean.HistoryRecordBean
import com.pengxh.autodingding.greendao.HistoryRecordBeanDao
import com.pengxh.autodingding.ui.MainActivity
import com.pengxh.autodingding.utils.PushDiagnostics
import com.pengxh.autodingding.utils.SendMailUtil
import com.pengxh.autodingding.utils.TimeOrDateUtil
import com.pengxh.autodingding.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

class NotificationMonitorService : NotificationListenerService() {
    private var recordBeanDao: HistoryRecordBeanDao? = null

    /**
     * 有可用的并且和通知管理器连接成功时回调
     */
    override fun onListenerConnected() {
        recordBeanDao = BaseApplication.daoSession?.historyRecordBeanDao
    }

    /**
     * 当有新通知到来时会回调
     */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == MAIL_MASTER_PACKAGE) {
            handleMailCommand(sbn)
            return
        }
        val extras = sbn.notification.extras
        // 获取接收消息APP的包名
        val packageName = sbn.packageName
        // 获取接收消息的内容
        val notificationText = extras.getString(Notification.EXTRA_TEXT)
        if (packageName == "com.alibaba.android.rimet") {
            if (notificationText == null || notificationText == "") {
                return
            }
            if (notificationText.contains("考勤打卡")) {
                //保存打卡记录
                val bean = HistoryRecordBean()
                bean.uuid = UUID.randomUUID().toString()
                bean.date = TimeOrDateUtil.timestampToDate(System.currentTimeMillis())
                bean.message = notificationText
                recordBeanDao!!.save(bean)
                //通知发送邮件和更新界面
                sendEvent(notificationText)
            }
        }
    }

    private fun handleMailCommand(sbn: StatusBarNotification) {
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = sbn.notification.extras
        // NetEase Mail Master displays the sender in EXTRA_TITLE and the subject in EXTRA_TEXT.
        // Match the subject only; do not use BIG_TEXT, which may contain the mail body.
        val subject = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        if (subject != "打卡" && subject != "检测running") return
        val prefs = getSharedPreferences("mail_command", MODE_PRIVATE)
        val marker = "${sbn.key}:${sbn.postTime}"
        if (marker == prefs.getString("last_notification", null)) return
        prefs.edit().putString("last_notification", marker).apply()
        if (subject == "检测running") {
            replyRunningCheck()
            return
        }
        PushDiagnostics.mailNotification(this, "匹配到“打卡”主题，正在打开钉钉")
        val power = getSystemService(POWER_SERVICE) as PowerManager
        power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "autoDing:mailCommand")
            .acquire(10_000L)
        try {
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_LAUNCH_DING)
            startActivity(intent)
        } catch (e: Exception) {
            PushDiagnostics.mailNotification(this, "邮件指令已匹配，但打开页面失败：${e.javaClass.simpleName}")
        }
    }

    private fun replyRunningCheck() {
        val toAddress = Utils.readEmailAddress().trim()
        if (toAddress.isEmpty()) {
            PushDiagnostics.mailNotification(this, "收到“检测running”，但未配置收件邮箱")
            return
        }
        PushDiagnostics.mailNotification(this, "收到“检测running”，正在发送运行确认邮件")
        Thread {
            val handledAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())
            val result = SendMailUtil.sendCustomMail(
                toAddress,
                "小助手运行确认",
                "B 手机的小助手在 $handledAt 收到并处理了“检测running”邮件通知。"
            )
            PushDiagnostics.mailNotification(this,
                if (result.isSuccess) "运行确认邮件已发送"
                else "运行确认邮件发送失败：${result.exceptionOrNull()?.javaClass?.simpleName}")
        }.start()
    }

    /**
     * 当有通知移除时会回调
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
    override fun onListenerDisconnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 通知侦听器断开连接 - 请求重新绑定
            requestRebind(ComponentName(this, NotificationMonitorService::class.java))
        }
    }

    companion object {
        private const val MAIL_MASTER_PACKAGE = "com.netease.mail"
    }
}
