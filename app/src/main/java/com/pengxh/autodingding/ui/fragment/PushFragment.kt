package com.pengxh.autodingding.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.CacheDiskUtils
import com.blankj.utilcode.util.ToastUtils
import com.pengxh.autodingding.databinding.FragmentPushBinding
import com.pengxh.autodingding.utils.SendMailUtil
import com.pengxh.autodingding.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class PushFragment : Fragment() {
    private var fragmentProvider: ViewModelProvider? = null
    private var binding: FragmentPushBinding? = null
    private lateinit var pushVM: PushVM

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPushBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        pushVM = getFragmentViewModel(PushVM::class.java)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pushVM.errorLiveData.observe(viewLifecycleOwner) {
            if (it.errorMessage.isNotBlank()) ToastUtils.showLong(it.errorMessage)
        }
        pushVM.pushResult.observe(viewLifecycleOwner){
            if (it.errorCode == 0){
                ToastUtils.showLong("推送平台已接受指令，请确认目标手机是否执行")
                saveRegId()
            }else{
                ToastUtils.showShort(it.messageCn)
            }
        }
        val savedRegId = CacheDiskUtils.getInstance().getString("regId")
        if (savedRegId != null){
            binding!!.etTargetRegId.setText(savedRegId)
        }
        val savedMailTarget = CacheDiskUtils.getInstance().getString("runningMailTarget", "")
        binding!!.etMailTarget.setText(savedMailTarget.takeUnless { it.isNullOrBlank() }
            ?: Utils.readEmailAddress())
        binding!!.ivBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .hide(this)
                .commit()
        }
        binding!!.btnCheck.setOnClickListener {
            if (binding!!.etTargetRegId.text.toString() == "") {
                ToastUtils.showShort("注册id必须填写")
            }else {
                pushVM.pushCheck(binding!!.etTargetRegId.text.toString())
            }
        }
        binding!!.btnPush.setOnClickListener {
            if (binding!!.etTargetRegId.text.toString() == "") {
                ToastUtils.showShort("注册id必须填写")
            }else {
                pushVM.pushSign(binding!!.etTargetRegId.text.toString())
            }
        }
        binding!!.btnStatus.setOnClickListener {
            if (binding!!.etTargetRegId.text.toString() == "") {
                ToastUtils.showShort("注册id必须填写")
            } else {
                pushVM.pushStatusFetch(binding!!.etTargetRegId.text.toString())
            }
        }
        binding!!.btnScreenShot.setOnClickListener {
            if (binding!!.etTargetRegId.text.toString() == "") {
                ToastUtils.showShort("注册id必须填写")
            } else {
                pushVM.pushScreenShot(binding!!.etTargetRegId.text.toString())
            }
        }
        binding!!.btnMailRunning.setOnClickListener {
            val target = binding!!.etMailTarget.text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(target).matches()) {
                ToastUtils.showShort("请填写 B 手机接收邮件的邮箱地址")
                return@setOnClickListener
            }
            val cache = CacheDiskUtils.getInstance()
            if (cache.getString("senderEmail", "").isNullOrBlank() ||
                cache.getString("senderAuth", "").isNullOrBlank()) {
                ToastUtils.showLong("请先在 A 手机的邮箱设置中填写发件邮箱和 SMTP 授权码")
                return@setOnClickListener
            }
            CacheDiskUtils.getInstance().put("runningMailTarget", target)
            binding!!.btnMailRunning.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    SendMailUtil.sendCustomMail(
                        target,
                        "检测running",
                        "请回复小助手运行状态。"
                    )
                }
                binding?.btnMailRunning?.isEnabled = true
                if (result.isSuccess) {
                    ToastUtils.showLong("检测邮件已发送，等待 B 手机回复")
                } else {
                    ToastUtils.showLong("检测邮件发送失败，请检查邮箱配置和网络")
                }
            }
        }


    }

    private fun <T : ViewModel> getFragmentViewModel(modelClass: Class<T>): T {
        if (fragmentProvider == null) {
            fragmentProvider = ViewModelProvider(this)
        }
        return fragmentProvider!![modelClass]
    }

    private fun saveRegId(){
        val curRegId = binding!!.etTargetRegId.text.toString().trim()
        CacheDiskUtils.getInstance().put("regId", curRegId)
    }

    companion object {
        @JvmStatic
        fun newInstance(): PushFragment {
            return PushFragment()
        }
    }
}
