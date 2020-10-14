package win.regin.sample.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;


import win.regin.sample.R;
import win.regin.sample.impl.e.MethodEntity;
import win.regin.sample.impl.e.ParamEntity;

/**
 * @author :Reginer in  2020/10/14 16:19.
 * 联系方式:QQ:282921012
 * 功能描述:
 */
public class FuncParamsFragment extends Dialog {
    public LinearLayout content;
    public TextView hint;
    public MethodEntity methodEntity;
    private final int width;

    public FuncParamsFragment(Context context, MethodEntity methodEntity) {
        super(context);
        this.methodEntity = methodEntity;
        this.width = context.getResources().getDisplayMetrics().widthPixels;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_func_params_layout);
        this.content = (LinearLayout) findViewById(R.id.content);
        Button enter = (Button) findViewById(R.id.enter);
        this.hint = (TextView) findViewById(R.id.hint);
        for (ParamEntity paramBean : methodEntity.getParams()) {
            LayoutParams layoutParams = new LayoutParams(this.width - 40, -2);
            layoutParams.setMargins(0, 5, 0, 5);
            this.content.addView(paramBean.setView(), layoutParams);
        }
        enter.setOnClickListener(view -> {
            for (ParamEntity paramBean : methodEntity.getParams()) {
                if (!paramBean.getView().isValid()) {
                    String sb = "参数格式错误:" + paramBean.getDescribe();
                    hint.setText(sb);
                    return;
                }
            }
            Object[] objArr = new Object[methodEntity.getParams().size()];
            for (int i = 0; i < methodEntity.getParams().size(); i++) {
                objArr[i] = ((ParamEntity) methodEntity.getParams().get(i)).getView().getValue();
            }
            methodEntity.invoke(objArr);
            content.removeAllViews();
            dismiss();
        });

    }
}
