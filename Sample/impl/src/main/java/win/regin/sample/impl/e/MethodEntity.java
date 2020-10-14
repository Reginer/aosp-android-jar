package win.regin.sample.impl.e;


import java.lang.reflect.Method;
import java.util.List;

import win.regin.sample.impl.a.IFunctionInvoke;
import win.regin.sample.impl.m.FuncManager;
import win.regin.sample.impl.u.ReflectUtils;

/**
 * @author :Reginer in  2020/10/12 13:43.
 * 联系方式:QQ:282921012
 * 功能描述:
 */
public class MethodEntity {

    private Method action;
    private String name;
    private List<ParamEntity> params;

    public MethodEntity(Method action, String name, List<ParamEntity> params) {
        this.action = action;
        this.name = name;
        this.params = params;
    }

    public Method getAction() {
        return action;
    }

    public void setAction(Method action) {
        this.action = action;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ParamEntity> getParams() {
        return params;
    }

    public void setParams(List<ParamEntity> params) {
        this.params = params;
    }

    private IFunctionInvoke iFunctionInvoke;

    public void invoke(IFunctionInvoke iFunctionInvoke) {
        this.iFunctionInvoke = iFunctionInvoke;
        if (params == null || params.size() == 0) {
            invoke();
        } else {
            iFunctionInvoke.invokeParams(this);
        }
    }

    public void invoke(Object... objects) {
        Object invokeResult;
        if (objects == null || objects.length == 0) {
            invokeResult = ReflectUtils.reflect(FuncManager.getInstance()).method(action).get();
        } else {
            invokeResult = ReflectUtils.reflect(FuncManager.getInstance()).method(action, objects).get();
        }
        if (iFunctionInvoke != null) {
            iFunctionInvoke.showInvokeResult(invokeResult);
        }
    }

}
