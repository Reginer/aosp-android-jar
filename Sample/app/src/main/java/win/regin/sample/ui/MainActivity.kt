package win.regin.sample.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemClickListener
import kotlinx.android.synthetic.main.activity_main.*
import win.regin.common.toJsonString
import win.regin.sample.R
import win.regin.sample.adapter.FuncAdapter
import win.regin.sample.impl.a.IFunctionInvoke
import win.regin.sample.impl.e.MethodEntity
import win.regin.sample.impl.m.AnoManager


/**
 * @author :Reginer in  2020/10/14 13:25.
 *         联系方式:QQ:282921012
 *         功能描述:demo
 */
class MainActivity : AppCompatActivity(), IFunctionInvoke, OnItemClickListener {
    private lateinit var mAdapter: FuncAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AnoManager.getInstance().initFunction()
        mAdapter = FuncAdapter(AnoManager.getInstance().methodEntityList);rvFunction.adapter = mAdapter
        rvFunction.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        mAdapter.setOnItemClickListener(this)
    }

    override fun showInvokeResult(any: Any?) {
        FuncResultFragment(any.toJsonString()).show(supportFragmentManager, this.javaClass.name)
    }

    override fun invokeParams(methodEntity: MethodEntity) {
        FuncParamsFragment(this, methodEntity).show()
    }

    override fun onItemClick(p0: BaseQuickAdapter<*, *>, p1: View, p2: Int) {
        AnoManager.getInstance().methodEntityList[p2].invoke(this)
    }

    override fun onDestroy() {
        AnoManager.getInstance().reset()
        super.onDestroy()
    }
}