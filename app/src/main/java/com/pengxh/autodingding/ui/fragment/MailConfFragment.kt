package com.pengxh.autodingding.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.CacheDiskUtils
import com.blankj.utilcode.util.ToastUtils
import com.pengxh.autodingding.databinding.FragmentMailConfBinding
import com.pengxh.autodingding.utils.Utils

class MailConfFragment :Fragment() {
    private var fragmentProvider: ViewModelProvider? = null
    private lateinit var binding: FragmentMailConfBinding
    private lateinit var vm: MailConfVM

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMailConfBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        vm = getFragmentViewModel(MailConfVM::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val savedFromAddress = CacheDiskUtils.getInstance().getString("senderEmail", "")
        val savedFromAuth = CacheDiskUtils.getInstance().getString("senderAuth", "")
        val savedToAddress = Utils.readEmailAddress()
        binding.etSenderEmailAddress.setText(savedFromAddress)
        binding.etSenderEmailAuth.setText(savedFromAuth)
        binding.etReceiverEmailAddress.setText(savedToAddress)

        vm.errorLiveData.observe(viewLifecycleOwner) {
            ToastUtils.showLong(it.errorMessage)
        }
        vm.mailSent.observe(viewLifecycleOwner) {
            ToastUtils.showShort("测试邮件已发送，请检查收件箱")
        }
        binding.btnCheck.setOnClickListener {
            val fromAddress = binding.etSenderEmailAddress.text.toString().trim()
            val fromAuth = binding.etSenderEmailAuth.text.toString().trim()
            val toAddress = binding.etReceiverEmailAddress.text.toString().trim()
            if(fromAddress.isEmpty() || fromAuth.isEmpty() || toAddress.isEmpty()){
                ToastUtils.showShort("配置选项缺失！")
            }else{
                vm.sendMail(fromAddress, fromAuth, toAddress)
                ToastUtils.showShort("正在发送测试邮件…")
            }
        }
        binding.btnSave.setOnClickListener {
            val fromAddress = binding.etSenderEmailAddress.text.toString().trim()
            val fromAuth = binding.etSenderEmailAuth.text.toString().trim()
            val toAddress = binding.etReceiverEmailAddress.text.toString().trim()
            if(fromAddress.isNotEmpty() && fromAuth.isNotEmpty() && toAddress.isNotEmpty()){
                CacheDiskUtils.getInstance().put("senderEmail" , fromAddress)
                CacheDiskUtils.getInstance().put("senderAuth", fromAuth)
                Utils.saveEmailAddress(toAddress)
                ToastUtils.showShort("保存配置成功")
            }else{
                ToastUtils.showShort("请填写完整邮箱配置")
            }
        }
        binding.ivBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .hide(this)
                .commit()
        }
    }

    private fun <T : ViewModel> getFragmentViewModel(modelClass: Class<T>): T {
        if (fragmentProvider == null) {
            fragmentProvider = ViewModelProvider(this)
        }
        return fragmentProvider!![modelClass]
    }
}