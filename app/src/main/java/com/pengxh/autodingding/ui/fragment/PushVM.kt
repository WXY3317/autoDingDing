package com.pengxh.autodingding.ui.fragment

import androidx.lifecycle.MutableLiveData
import cn.jpush.android.api.JPushInterface
import com.blankj.utilcode.util.CacheDiskUtils
import com.pengxh.autodingding.ApiException
import org.json.JSONObject
import retrofit2.HttpException
import com.pengxh.autodingding.base.BaseViewModel
import com.pengxh.autodingding.bean.BodyMsg
import com.pengxh.autodingding.bean.PushAudience
import com.pengxh.autodingding.bean.PushMessage
import com.pengxh.autodingding.bean.PushResp
import com.pengxh.autodingding.net.RetrofitManager
import com.pengxh.autodingding.net.api.PushApi
import com.pengxh.autodingding.service.PushCoreService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PushVM: BaseViewModel() {
    val pushResult = MutableLiveData<PushResp>()

    fun pushCheck(regId:String) {
        pushCmd(regId, PushCoreService.MSG_MAIL_CHECK)
    }

    fun pushSign(regId:String) {
        pushCmd(regId, PushCoreService.MSG_SIGN)
    }

    fun pushStatusFetch(regId: String){
        pushCmd(regId, PushCoreService.MSG_STATUS_REPORT)
    }

    fun pushScreenShot(regId: String){
        pushCmd(regId, PushCoreService.MSG_SCREEN_SHOT)
    }

    fun pushManualSign(regId: String){
        pushCmd(regId, PushCoreService.MSG_MANUAL_SIGN)
    }

    private fun pushCmd(regId: String, cmd: String) = launch {
        val targetRegId = regId.trim()
        if (!targetRegId.matches(Regex("[A-Za-z0-9]+"))) {
            throw ApiException("目标注册 ID 格式错误：请清空输入框，只粘贴一次 B 手机的 ID，不能包含空格或换行", -100)
        }
        pushResult.value = withContext(Dispatchers.IO){
            val api = RetrofitManager.retrofitClient.create(PushApi::class.java)
            val pushMessage = PushMessage()
            pushMessage.audience = PushAudience().apply {
                registration_id = mutableListOf(targetRegId)
            }
            pushMessage.message = BodyMsg().apply {
                msg_content = cmd
                title = JPushInterface.getRegistrationID(com.pengxh.autodingding.BaseApplication.application)
                    .takeUnless { it.isNullOrBlank() } ?: "cmd"
            }
            try {
                api.pushMsg(pushMessage)
            } catch (e: HttpException) {
                val detail = runCatching {
                    JSONObject(e.response()?.errorBody()?.string().orEmpty())
                        .optJSONObject("error")?.let {
                            "${it.optInt("code")}: ${it.optString("message")}"
                        }
                }.getOrNull()
                throw ApiException("推送失败（HTTP ${e.code()}）${detail?.let { ": $it" }.orEmpty()}", e.code())
            }
        }
    }
}