package win.regin.sample.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import win.regin.sample.R
import win.regin.sample.impl.e.MethodEntity

/**
 * @author :Reginer in  2020/10/14 16:24.
 *         联系方式:QQ:282921012
 *         功能描述:功能列表Adapter
 */
class FuncAdapter(data: MutableList<MethodEntity>?) : BaseQuickAdapter
<MethodEntity, BaseViewHolder>(R.layout.view_item_single_text_view_layout, data) {


    override fun convert(holder: BaseViewHolder, item: MethodEntity) {
        holder.setText(R.id.itemMethod, item.name)
    }


}