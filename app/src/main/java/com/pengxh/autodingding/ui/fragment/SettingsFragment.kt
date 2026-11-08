package com.pengxh.autodingding.ui.fragment

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cn.jpush.android.api.JPushInterface
import com.blankj.utilcode.util.CacheDiskUtils
import com.blankj.utilcode.util.IntentUtils
import com.blankj.utilcode.util.ToastUtils
import com.pengxh.autodingding.AndroidxBaseFragment
import com.pengxh.autodingding.BaseApplication.Companion.daoSession
import com.pengxh.autodingding.BuildConfig
import com.pengxh.autodingding.R
import com.pengxh.autodingding.actions.DingSignAction
import com.pengxh.autodingding.databinding.FragmentSettingsBinding
import com.pengxh.autodingding.service.NotificationMonitorService
import com.pengxh.autodingding.service.IncomingCallReceiver
import com.pengxh.autodingding.ui.HistoryRecordActivity
import com.pengxh.autodingding.ui.fragment.PushFragment.Companion.newInstance
import com.pengxh.autodingding.utils.Constant
import com.pengxh.autodingding.utils.PushDiagnostics
import com.pengxh.autodingding.utils.Utils
import com.pengxh.autodingding.utils.launchWithExpHandler
import com.pengxh.autodingding.utils.toast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job

class SettingsFragment : AndroidxBaseFragment<FragmentSettingsBinding?>(), View.OnClickListener {
    private var actionJob: Job? = null

    private val phonePermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
        requireContext().getSharedPreferences(IncomingCallReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(IncomingCallReceiver.KEY_ENABLED, granted).apply()
        viewBinding?.phoneTriggerCheckBox?.isChecked = granted
        ToastUtils.showShort(if (granted) "来电触发已开启" else "未获得电话状态权限，来电触发未开启")
    }

    override fun initData() {
        val historyBeanDao = daoSession!!.historyRecordBeanDao
        val emailAddress = Utils.readEmailAddress()
        if (emailAddress != "") {
            viewBinding!!.emailTextView.text = emailAddress
        }
        viewBinding!!.recordSize.text = historyBeanDao.loadAll().size.toString()
        viewBinding!!.appVersion.text = BuildConfig.VERSION_NAME
        val savedRegId = CacheDiskUtils.getInstance().getString("regId")
        if (savedRegId != null) viewBinding!!.pushTextView.text = savedRegId
    }

    private val settingsLauncher =
        registerForActivityResult(StartActivityForResult()) {
            if (isNotificationEnable) {
                startNotificationMonitorService()
            }
        }

    //检测通知监听服务是否被授权
    private val isNotificationEnable: Boolean
        get() {
            val packageNames =
                NotificationManagerCompat.getEnabledListenerPackages(requireContext())
            return packageNames.contains(context?.packageName)
        }

    override fun initEvent() {
        if (!isNotificationEnable) {
            try {
                //打开通知监听设置页面
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                settingsLauncher.launch(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            startNotificationMonitorService()
        }
        viewBinding!!.tvRegValue.text = JPushInterface.getRegistrationID(context)
        viewBinding!!.phoneTriggerCheckBox.setOnClickListener {
            val enabled = viewBinding!!.phoneTriggerCheckBox.isChecked
            if (enabled && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
                viewBinding!!.phoneTriggerCheckBox.isChecked = false
                phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            } else {
                requireContext().getSharedPreferences(IncomingCallReceiver.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putBoolean(IncomingCallReceiver.KEY_ENABLED, enabled).apply()
            }
        }
        viewBinding!!.tvPushDiagnostics.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("推送诊断")
                .setMessage(PushDiagnostics.report(requireContext()))
                .setPositiveButton("关闭", null)
                .setNeutralButton("重新启用推送") { _, _ ->
                    JPushInterface.resumePush(requireContext())
                    ToastUtils.showShort("已请求重新启用，请稍后重新发送指令并查看诊断")
                }
                .show()
        }
        viewBinding!!.btnCopy.setOnClickListener(this)
        viewBinding!!.btnTestLaunchDing.setOnClickListener(this)
        viewBinding!!.emailLayout.setOnClickListener(this)
        viewBinding!!.historyLayout.setOnClickListener(this)
        viewBinding!!.introduceLayout.setOnClickListener(this)
        viewBinding!!.pushLayout.setOnClickListener(this)
        viewBinding!!.testLayout.setOnClickListener(this)
        viewBinding!!.rlVersion.setOnClickListener(this)
        viewBinding!!.permissionLayout.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        viewBinding?.tvRegValue?.text = JPushInterface.getRegistrationID(context)
        val granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
        val enabled = requireContext().getSharedPreferences(IncomingCallReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(IncomingCallReceiver.KEY_ENABLED, false)
        viewBinding?.phoneTriggerCheckBox?.isChecked = granted && enabled
    }

    //切换通知监听器服务
    private fun startNotificationMonitorService() {
        //创建常住通知栏
        Utils.createNotification()
        val pm = requireContext().packageManager
        pm.setComponentEnabledSetting(
            ComponentName(requireContext(), NotificationMonitorService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            ComponentName(requireContext(), NotificationMonitorService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
        viewBinding!!.noticeCheckBox.isChecked = isNotificationEnable
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onClick(v: View) {
        when (v.id) {
            R.id.emailLayout -> {
                parentFragmentManager
                    .beginTransaction()
                    .add(R.id.fragmentContainer, MailConfFragment())
                    .commit()
            }
            R.id.historyLayout -> {
                startActivity(Intent(context, HistoryRecordActivity::class.java))
            }
            R.id.introduceLayout -> {
                Utils.showAlertDialog(
                    activity,
                    "功能介绍",
                    context?.getString(R.string.about),
                    "看完了",
                    true
                )
            }
            R.id.btnCopy -> {
                val clipboardManager = context
                    ?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        null,
                        JPushInterface.getRegistrationID(context)
                    )
                )
                ToastUtils.showShort("已复制")
            }
            R.id.btnTestLaunchDing -> {
                activity?.startActivity(IntentUtils.getLaunchAppIntent(Constant.DINGDING))
            }
            R.id.pushLayout -> {
                parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, newInstance())
                    .commit()
            }
            R.id.rlVersion -> {
                // undone
            }
            R.id.testLayout -> {
                val action = DingSignAction()
                if (actionJob?.isCompleted.let { it != null && !it }) {
                    toast("有正在运行的任务")
                    return
                }
                actionJob = launchWithExpHandler {
                    activity?.let { action.run(it) }
                }
                actionJob?.invokeOnCompletion {
                    Utils.showAlertDialog(activity, "执行日志", action.message.toString(),
                        "确定", true)
                }
            }
            R.id.permissionLayout -> {
                parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, PermissionFragment())
                    .commit()
            }
        }
    }

    override fun setupTopBarLayout() {
    }
}
