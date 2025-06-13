至于为什么会有src这个目录，可以看这里

https://blog.csdn.net/qq_26413249/article/details/120696400

以下文件为参与编译而经过修改,修改内容只在jar包中存在，而源码中不存在，调用会崩溃:

##### 1
```
添加方法
frameworks/base/core/java/android/content/pm/PackageInstaller.java

 /** {@hide} */
    public PackageInstaller(IPackageInstaller installer, String installerPackageName, int userId) {
      this(installer,installerPackageName,null,userId);
    }

```

##### 2
`添加文件android.net.IConnectivityManager`


##### 3
`移除多余文件com.android.internal.app.procstats.ProcessState.class`
