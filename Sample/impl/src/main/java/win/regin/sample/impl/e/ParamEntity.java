package win.regin.sample.impl.e;

import android.content.Context;


import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import win.regin.sample.impl.m.ContextManager;
import win.regin.sample.impl.v.AbstractInputView;
import win.regin.sample.impl.v.BooleanView;
import win.regin.sample.impl.v.StringView;

/**
 * @author :Reginer in  2020/10/12 13:43.
 * 联系方式:QQ:282921012
 * 功能描述:
 */
public class ParamEntity {
    private String defaultValue;
    private String describe;
    private Class<?> param;
    private Type type;

    private static final Map<Class<?>, Class<?>> VIEWS = new HashMap<>();
    private static final Class<?> DEFAULT_VIEW = StringView.class;
    private AbstractInputView view;

    static {
        VIEWS.put(Object.class, StringView.class);
        VIEWS.put(String.class, StringView.class);
        VIEWS.put(Boolean.class, BooleanView.class);
        VIEWS.put(Boolean.TYPE, BooleanView.class);
    }


    public ParamEntity(String defaultValue, String describe, Class<?> param, Type type) {
        this.defaultValue = defaultValue;
        this.describe = describe;
        this.param = param;
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public Class<?> getParam() {
        return param;
    }

    public void setParam(Class<?> param) {
        this.param = param;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public AbstractInputView setView() {
        final Class<?> cls;
        if (VIEWS.containsKey(this.param)) {
            cls = VIEWS.get(this.param);
        } else {
            cls = DEFAULT_VIEW;
        }
        try {
            if (cls != null) {
                this.view = (AbstractInputView) cls.getConstructor(new Class[]{Context.class, ParamEntity.class})
                        .newInstance(new Object[]{ContextManager.getInstance().getContext(), this});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.view;
    }

    public AbstractInputView getView() {
        return this.view;
    }

}
