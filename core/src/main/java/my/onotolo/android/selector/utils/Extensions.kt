package my.onotolo.android.selector.utils

import android.view.View

fun View.addSimpleOnLayoutChangeListener(listener: () -> Unit) {
    this.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
            listener()
            v?.removeOnLayoutChangeListener(this)
        }
    })
}