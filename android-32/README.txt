[源码采用android-12.1.0_r5分支编译](https://cs.android.com/android/platform/superproject/+/android-12.1.0_r5:/)

以下文件有所变动，有些为参与编译而经过修改,修改内容只在jar包中存在，而源码中不存在，调用会崩溃:


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

`移除android.content.Intent#getParcelableExtra(String name)过时提示`


