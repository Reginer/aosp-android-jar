至于为什么会有src这个目录，可以看这里

https://blog.csdn.net/qq_26413249/article/details/120696400


[源码采用android-13.0.0_r3分支编译，也许是r4](https://cs.android.com/android/platform/superproject/+/android-13.0.0_r3:/)

以下文件有所变动，有些为参与编译而经过修改,修改内容只在jar包中存在，而源码中不存在，调用会崩溃:



##### 1
`添加文件com.android.internal.os.BatteryStatsHelper`

##### 2
`添加文件com.android.internal.os.BatterySipper`


##### 3

`添加文件android.net.IConnectivityManager`








