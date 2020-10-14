package win.regin.sample.impl.v;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import win.regin.sample.impl.e.ParamEntity;


/**
 * @author :Reginer in  2020/10/14 16:02.
 * 联系方式:QQ:282921012
 * 功能描述:String类型参数
 */
@SuppressLint("ViewConstructor")
public class StringView extends AbstractInputView {
    private EditText input;

    public StringView(Context context, ParamEntity paramsEntity) {
        super(context, paramsEntity);
    }

    public void createView(ParamEntity paramsEntity) {
        TextView hint = new TextView(getContext());
        hint.setText(paramsEntity.getDescribe());
        this.input = new EditText(getContext());
        this.input.setText(paramsEntity.getDefaultValue());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2);
        layoutParams.weight = 1.0f;
        addView(hint, layoutParams);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, -2);
        layoutParams2.weight = 2.0f;
        addView(this.input, layoutParams2);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Object getValue() {
        return this.input.getText().toString();
    }
}
