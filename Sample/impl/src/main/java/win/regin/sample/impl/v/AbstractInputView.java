package win.regin.sample.impl.v;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;

import win.regin.sample.impl.e.ParamEntity;


/**
 * @author :Reginer in  2020/10/14 16:01.
 *         联系方式:QQ:282921012
 *         功能描述:
 */
public abstract class AbstractInputView extends LinearLayout {
    ParamEntity param;

    public abstract void createView(ParamEntity paramsEntity);

    public abstract Object getValue();

    public abstract boolean isValid();

    public AbstractInputView(Context context, ParamEntity paramsEntity) {
        super(context);
        setGravity(Gravity.CENTER_VERTICAL);
        this.param = paramsEntity;
        createView(this.param);
    }
}
