至于为什么会有src这个目录，可以看这里

https://blog.csdn.net/qq_26413249/article/details/120696400


[源码采用android-15.0.0_r1分支编译](https://cs.android.com/android/platform/superproject/+/android-15.0.0_r1:/)


以下文件有所变动


##### 1

```
com.android.internal.app.procstats.ProcessStats

默认编译出来的文件会r8报错，直接使用了Android14的对应文件，没有经过比对内容是否一致，使用到该文件需谨慎

```







