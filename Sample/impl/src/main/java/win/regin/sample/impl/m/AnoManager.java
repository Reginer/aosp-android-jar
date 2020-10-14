package win.regin.sample.impl.m;

import android.util.Singleton;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import win.regin.sample.impl.ano.FuncMethod;
import win.regin.sample.impl.ano.FuncParam;
import win.regin.sample.impl.e.MethodEntity;
import win.regin.sample.impl.e.ParamEntity;

/**
 * @author :Reginer on  2020/10/14 11:38.
 * 联系方式:QQ:282921012
 * 功能描述:方法列表管理类
 */
public class AnoManager {

    private static final Singleton<AnoManager> INSTANCE = new Singleton<AnoManager>() {
        @Override
        protected AnoManager create() {
            return new AnoManager();
        }
    };

    public static AnoManager getInstance() {
        return INSTANCE.get();
    }

    private final List<MethodEntity> methodEntityList = new ArrayList<>();

    public void initFunction() {
        final Method[] methods = FuncManager.class.getDeclaredMethods();
        for (Method method : methods) {
            List<ParamEntity> params = null;
            Class<?>[] parameterTypes = method.getParameterTypes();
            Type[] arrayOfType = method.getGenericParameterTypes();
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < arrayOfType.length; i++) {
                for (Annotation parameterAnnotation : parameterAnnotations[i]) {
                    if (parameterAnnotation instanceof FuncParam) {
                        FuncParam ano = (FuncParam) parameterAnnotation;
                        params = new ArrayList<>();
                        params.add(new ParamEntity(ano.defaultValue(), ano.describe(), parameterTypes[i], arrayOfType[i]));
                    }
                }
            }
            FuncMethod methodAnnotation = method.getAnnotation(FuncMethod.class);
            if (methodAnnotation != null) {
                methodEntityList.add(new MethodEntity(method, methodAnnotation.name(), params));
            }
        }
    }

    public void reset() {
        methodEntityList.clear();
    }

    public List<MethodEntity> getMethodEntityList() {
        return methodEntityList;
    }


}
