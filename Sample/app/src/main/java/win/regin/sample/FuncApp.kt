package win.regin.sample

import com.orhanobut.logger.Logger
import win.regin.common.AppCommon
import win.regin.common.logTagDebug
import win.regin.sample.constant.SampleConstant

/**
 * @author :Reginer in  2020/10/14 14:23.
 * 联系方式:QQ:282921012
 * 功能描述:
 */
class FuncApp : AppCommon() {
    override fun onCreate() {
        super.onCreate()
        Logger.addLogAdapter(SampleConstant.SAMPLE_LOG_TAG.logTagDebug(BuildConfig.DEBUG))
    }
}