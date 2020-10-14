package win.regin.sample.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.view_item_single_text_view_params_layout.*
import win.regin.common.parseString
import win.regin.sample.R

/**
 * @author :Reginer in  2020/10/12 19:30.
 * 联系方式:QQ:282921012
 * 功能描述:
 */
class FuncResultFragment(private val result: String) : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return inflater.inflate(R.layout.view_item_single_text_view_params_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mdmFunctionContent.movementMethod = ScrollingMovementMethod.getInstance()
        mdmFunctionContent.text = result.parseString()
    }

}