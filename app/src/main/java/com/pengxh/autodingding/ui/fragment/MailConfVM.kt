package com.pengxh.autodingding.ui.fragment


import androidx.lifecycle.MutableLiveData
import com.pengxh.autodingding.ApiException
import com.pengxh.autodingding.base.BaseViewModel
import kotlinx.coroutines.CancellationException
import javax.mail.AuthenticationFailedException
import javax.mail.MessagingException
import javax.mail.internet.AddressException
import com.pengxh.autodingding.utils.MailSender
import com.pengxh.autodingding.utils.SendMailUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MailConfVM : BaseViewModel() {
    val mailSent = MutableLiveData<Boolean>()

    fun sendMail(fromEmail: String, fromAuth: String, toEmail: String) = launch {
        withContext(Dispatchers.IO) {
            try {
                MailSender().sendTextMail(
                    SendMailUtil.createMail(toEmail, "测试邮件", fromEmail, fromAuth)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthenticationFailedException) {
                throw ApiException("163邮箱认证失败：请确认已开启SMTP服务，并重新填写该发件邮箱的SMTP授权码（不是登录密码）", -100)
            } catch (e: AddressException) {
                throw ApiException("邮箱地址格式错误，请检查发件和收件邮箱", -100)
            } catch (e: Exception) {
                val cause = if (e is MessagingException) e.nextException ?: e else e
                val rawDetail = cause.message ?: cause.javaClass.simpleName
                val detail = if (fromAuth.isNotEmpty()) rawDetail.replace(fromAuth, "[已隐藏]") else rawDetail
                throw ApiException("邮件发送失败：${detail.take(240)}", -100)
            }
        }
        mailSent.value = true
    }
}