package com.example.okysunlock;

import android.app.Application;
import android.content.Context;

import java.io.File;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.layaboxhmhz.gamehmhz.okys";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;

        // Hook Application.attach 确保最早执行
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                if (context == null) return;

                // 写入解锁文件
                UnlockUtils.writeUnlockFiles(context);

                // 防止 App 删除 ra.txt
                hookFileDelete();
            }
        });
    }

    private void hookFileDelete() {
        try {
            Method deleteMethod = File.class.getDeclaredMethod("delete");
            Pine.hook(deleteMethod, new MethodHook() {
                @Override
                public void beforeCall(Pine.CallFrame callFrame) {
                    File file = (File) callFrame.thisObject;
                    if (file != null && "ra.txt".equals(file.getName())) {
                        callFrame.setResult(true); // 伪装删除成功，但实际不删
                    }
                }
            });

            // deleteOnExit
            Method deleteOnExit = File.class.getDeclaredMethod("deleteOnExit");
            Pine.hook(deleteOnExit, new MethodHook() {
                @Override
                public void beforeCall(Pine.CallFrame callFrame) {
                    File file = (File) callFrame.thisObject;
                    if (file != null && "ra.txt".equals(file.getName())) {
                        callFrame.setResult(null);
                    }
                }
            });
        } catch (Exception e) {
            // 忽略
        }
    }
}
