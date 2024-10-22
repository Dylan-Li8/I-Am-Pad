package com.houvven.impad

import android.content.Context
import android.os.Process
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.android.ApplicationClass
import com.highcapable.yukihookapi.hook.type.android.BuildClass
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import java.io.File

@InjectYukiHookWithXposed
object HookEntrance : IYukiHookXposedInit {

    override fun onInit() = YukiHookAPI.configs {
        isDebug = BuildConfig.DEBUG
        debugLog {
            tag = BuildConfig.APPLICATION_ID
        }
    }

    override fun onHook() = YukiHookAPI.encase {
        processQQ()
        processWeChat()
        processWeWork()
    }

    private fun PackageParam.processQQ() = loadApp(QQ_PACKAGE_NAME) {
        simulateTabletModel("Xiaomi", "23046RP50C")
        simulateTabletProperties()

        withProcess(mainProcessName) {
            dataChannel.wait(ClearCacheKey) {
                YLog.info("Clear QQ cache")
                File("${appInfo.dataDir}/files/mmkv/Pandora").deleteRecursively()
                File("${appInfo.dataDir}/files/mmkv/Pandora.crc").deleteRecursively()
                Process.killProcess(Process.myPid())
            }
        }
    }

    private fun PackageParam.processWeChat() {
        loadApp { packageName ->
            if (packageName.startsWith(WECHAT_PACKAGE_NAME)) {
                simulateTabletModel("samsung", "SM-F9560")
    
                withProcess(mainProcessName) {
                    dataChannel.wait(ClearCacheKey) {
                        YLog.info("Clear WeChat cache")
                        File(appInfo.dataDir, ".auth_cache").deleteRecursively()
                        Process.killProcess(Process.myPid())
                    }
                }
            }
        }
    }

    private fun PackageParam.processWeWork() = loadApp(WEWORK_PACKAGE_NAME) {
        val targetClassName = "com.tencent.wework.foundation.impl.WeworkServiceImpl"
        val targetMethodName = "isAndroidPad"
        ApplicationClass.method {
            name("attach")
        }.hook().after {
            val context = args[0] as Context
            val classLoader = context.classLoader
            val clazz = targetClassName.toClass(classLoader)
            clazz.method { name(targetMethodName) }.hook().replaceToTrue()
        }
    }

    private fun simulateTabletModel(brand: String, model: String, manufacturer: String = brand) {
        BuildClass.run {
            field { name("MANUFACTURER") }.get(null).set(manufacturer)
            field { name("BRAND") }.get(null).set(brand)
            field { name("MODEL") }.get(null).set(model)
        }
    }

    private fun PackageParam.simulateTabletProperties() {
        SystemPropertiesClass.method {
            name("get")
            returnType(StringClass)
        }.hook().before {
            if (args[0] == "ro.build.characteristics") {
                result = "tablet"
            }
        }
    }
}
