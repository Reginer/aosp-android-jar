 /**
 * @author :Reginer in  2020/2/27 16:06.
 *         联系方式:QQ:282921012
 *         功能描述:Mdm环境变量
 */
object AppConfig {
    const val versionName = "1.0"
    const val versionCode = 1
    const val applicationId = "win.regin.sample"
    const val buildToolsVersion = "30.0.2"
    const val compileSdkVersion = 30
    const val targetSdkVersion = 30
    const val minSdkVersion = 26


     const val storePassword = "ly9999"
     const val keyAlias = "那时年少"
     const val storeFile = "../keystore/Young.jks"
}

object Version {
    const val kotlinVersion = "1.4.0"
    const val lifecycleVersion = "2.2.0"
    const val retrofitVersion = "2.9.0"
    const val objectboxVersion = "2.7.1"
    const val glideVersion = "4.11.0"
    const val workVersion = "2.3.4"
    const val refreshVersion = "2.0.0"
    const val cameraVersion = "1.0.0-alpha08"
    const val ariaVersion = "3.8.10"
    const val andServerVersion = "2.0.5"
}

object DependenciesExt {
    const val buildGradle = "com.android.tools.build:gradle:4.0.2"
    const val kotlinGradlePlugin =
            "org.jetbrains.kotlin:kotlin-gradle-plugin:${Version.kotlinVersion}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.kotlinVersion}"
    const val appcompat = "androidx.appcompat:appcompat:1.2.0"
    const val ktx = "androidx.core:core-ktx:1.3.2"
    const val constraintlayout = "androidx.constraintlayout:constraintlayout:2.0.2"
    const val junit = "junit:junit:4.12"
    const val extUnit = "androidx.test.ext:junit:1.1.2"
    const val espresso = "androidx.test.espresso:espresso-core:3.3.0"

    //https://developer.android.google.cn/jetpack/androidx/releases/lifecycle#declaring_dependencies
    const val lifecycle = "androidx.lifecycle:lifecycle-extensions:${Version.lifecycleVersion}"
    const val viewmodelKtx =
            "androidx.lifecycle:lifecycle-viewmodel-ktx:${Version.lifecycleVersion}"

    //https://github.com/square/retrofit
    private const val retrofitCore = "com.squareup.retrofit2:retrofit:${Version.retrofitVersion}"
    private const val converterGson =
            "com.squareup.retrofit2:converter-gson:${Version.retrofitVersion}"
    val retrofit = arrayOf(retrofitCore, converterGson)

    //https://github.com/google/gson
    const val gson = "com.google.code.gson:gson:2.8.6"

    //https://jcenter.bintray.com/com/squareup/okhttp3/logging-interceptor/
    const val okhttp3Log = "com.squareup.okhttp3:logging-interceptor:4.5.0"

    //https://github.com/Kotlin/kotlinx.coroutines
    //https://jcenter.bintray.com/org/jetbrains/kotlinx/kotlinx-coroutines-android/
    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9"
    const val material = "com.google.android.material:material:1.2.0"

    // https://github.com/objectbox/objectbox-java
    const val objectboxProcessor = "io.objectbox:objectbox-processor:${Version.objectboxVersion}"
    private const val objectboxKotlin = "io.objectbox:objectbox-kotlin:${Version.objectboxVersion}"
    private const val objectboxAndroid =
            "io.objectbox:objectbox-android:${Version.objectboxVersion}"
    val objectbox = arrayOf(objectboxKotlin, objectboxAndroid)
    const val objectboxGradlePlugin =
            "io.objectbox:objectbox-gradle-plugin:${Version.objectboxVersion}"

    //https://github.com/orhanobut/logger
    const val logger = "com.orhanobut:logger:2.2.0"

    //https://github.com/Reginer/MVVMHub.gitge
    const val mvvm = "com.github.Reginer:MVVMHub:2.0.3"

    //https://github.com/SpeedataG/Mvp
    const val mvp = "com.github.SpeedataG:Mvp:1.5"

    //https://github.com/Tencent/MMKV.git
    const val mmkv = "com.tencent:mmkv-static:1.2.1"

    //https://github.com/getActivity/ToastUtils
    const val toast = "com.hjq:toast:8.8"

    //https://github.com/xwc520/BusinessComponent/tree/master/view_module_swipeback
    const val swipeBack = "com.from.view.swipeback:view_module_swipeback:1.0.3"

    //https://github.com/bumptech/glide.git
    const val glide = "com.github.bumptech.glide:glide:${Version.glideVersion}"
    const val glideCompiler = "com.github.bumptech.glide:compiler:${Version.glideVersion}"

    //https://github.com/yanzhenjie/Album
    const val album = "com.github.Reginer:Album:2.1.4"

    //https://github.com/CymChad/BaseRecyclerViewAdapterHelper
    const val recyclerViewAdapter = "com.github.CymChad:BaseRecyclerViewAdapterHelper:3.0.4"

    //https://github.com/Curzibn/Luban 图片压缩
    const val imageCompress = "top.zibin:Luban:1.1.8"

    //https://github.com/bingoogolapple/BGAQRCode-Android 扫码
    const val scan = "cn.bingoogolapple:bga-qrcode-zbar:1.3.7"

    //http://jcenter.bintray.com/com/google/zxing/core/  二维码生成
    const val zxingCore = "com.google.zxing:core:3.4.0"


    //https://developer.android.google.cn/jetpack/androidx/releases/work#declaring_dependencies
    val workManager = arrayOf(
            "androidx.work:work-runtime:${Version.workVersion}",
            "androidx.work:work-runtime-ktx:${Version.workVersion}"
    )

    //https://github.com/scwang90/SmartRefreshLayout
    val refreshLayout = arrayOf(
            "com.scwang.smart:refresh-layout-kernel:${Version.refreshVersion}",
            "com.scwang.smart:refresh-header-classics:${Version.refreshVersion}",
            "com.scwang.smart:refresh-layout-kernel:${Version.refreshVersion}",
            "com.scwang.smart:refresh-footer-classics:${Version.refreshVersion}",
            "com.scwang.smart:refresh-footer-ball:${Version.refreshVersion}"
    )

    //https://codelabs.developers.google.com/codelabs/camerax-getting-started/#0
    val location = arrayOf(
            //https://jcenter.bintray.com/com/amap/api/location/
            "com.amap.api:location:4.8.0"
    )

    //https://github.com/Justson/AgentWeb
    const val webView = "com.just.agentweb:agentweb:4.1.2"

    //https://github.com/Karn/notify
    const val notify = "io.karn:notify:1.3.0"

    //https://codelabs.developers.google.com/codelabs/camerax-getting-started/#2
    private const val cameraCore = "androidx.camera:camera-core:${Version.cameraVersion}"
    private const val camera2 = "androidx.camera:camera-camera2:${Version.cameraVersion}"
    val camera = arrayOf(cameraCore, camera2)

    //https://github.com/AriaLyy/Aria
    const val ariaCore = "com.arialyy.aria:core:${Version.ariaVersion}"
    const val ariaCompiler = "com.arialyy.aria:compiler:${Version.ariaVersion}"

    private const val mqttClient = "org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.2"
    private const val mqttService = "org.eclipse.paho:org.eclipse.paho.android.service:1.1.1"
    val mqtt = arrayOf(mqttClient, mqttService)

    const val localBroadcastManager = "androidx.localbroadcastmanager:localbroadcastmanager:1.0.0"


    //https://github.com/anzaizai/EasySwipeMenuLayout 侧滑删除
    const val easySwipeMenuLayout = "com.github.anzaizai:EasySwipeMenuLayout:1.1.4"

    //https://github.com/gzu-liyujiang/AndroidPicker 文件浏览
    const val filePicker = "com.github.gzu-liyujiang.AndroidPicker:FilePicker:1.5.6"
    //https://github.com/yanzhenjie/AndServer
    private const val andServerApi = "com.yanzhenjie.andserver:api:${Version.andServerVersion}"
    private const val andServerAnnotation = "com.yanzhenjie.andserver:annotation:${Version.andServerVersion}"
    const val andServerProcessor = "com.yanzhenjie.andserver:processor:${Version.andServerVersion}"
    val andServer = arrayOf(andServerApi, andServerAnnotation)
    const val pickerView = "com.contrarywind:Android-PickerView:4.1.6"
    const val conscrypt = "org.conscrypt:conscrypt-android:2.4.0"
}