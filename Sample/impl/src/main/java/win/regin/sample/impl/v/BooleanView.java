package win.regin.sample.impl.v;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import win.regin.sample.impl.e.ParamEntity;


/**
 * @author :Reginer in  2020/10/14 16:02.
 * 联系方式:QQ:282921012
 * 功能描述:Boolean类型参数
 */
@SuppressLint("ViewConstructor")
public class BooleanView extends AbstractInputView {
    private RadioGroup group;
    private RadioButton yes;

    public BooleanView(Context context, ParamEntity paramsEntity) {
        super(context, paramsEntity);
    }

    public void createView(ParamEntity paramsEntity) {
        TextView hint = new TextView(getContext());
        hint.setText(paramsEntity.getDescribe());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2);
        layoutParams.weight = 1.0f;
        addView(hint, layoutParams);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, -2);
        layoutParams2.weight = 2.0f;
        group = new RadioGroup(getContext());
        yes = new RadioButton(getContext());
        yes.setText("是");
        RadioButton no = new RadioButton(getContext());
        no.setText("否");
        addView(group, layoutParams2);
        group.addView(yes);
        group.addView(no);
        group.setOrientation(LinearLayout.HORIZONTAL);
        if ("true".equalsIgnoreCase(paramsEntity.getDefaultValue())) {
            yes.setChecked(true);
        } else {
            no.setChecked(true);
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Object getValue() {
        return group.getCheckedRadioButtonId() == yes.getId();
    }
}
