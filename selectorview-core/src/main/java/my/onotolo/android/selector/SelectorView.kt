package my.onotolo.android.selector

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.*
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.core.view.children
import androidx.core.view.get
import my.onotolo.android.selector.utils.addSimpleOnLayoutChangeListener
import kotlin.math.abs
import kotlin.math.roundToInt

typealias Action<T> = (T) -> Unit

class SelectorView(context: Context, attrs: AttributeSet) : HorizontalScrollView(context, attrs) {

    private val evaluator = ArgbEvaluator()

    private var defaultColor: Int? = null
    private var selectedColor: Int? = null

    private var scrollChangeListener: ViewTreeObserver.OnScrollChangedListener? = null

    private var colorView: ((View, Int) -> Unit)? = null

    fun useColors(default: Int,
                  selected: Int,
                  colorView: (View, Int) -> Unit) {
        defaultColor = default
        selectedColor = selected
        this.colorView = colorView;

        scrollBy(1, 0)
        scrollBy(-1, 0)
    }

    fun <T>setData(data: Array<T>,
                   visibleItemsCount: Int = 3,
                   configureView: (T, View) -> Unit,
                   selectionListener: Action<T>? = null,
                   scrollListener: ((position: Int, positionOffset: Float) -> Unit)? = null,
                   @LayoutRes optionId: Int = R.layout.diff_chip,
                   selectedItem: T? = null) {

        addSimpleOnLayoutChangeListener {
            inflateOptionViews(
                    data,
                    optionId,
                    selectedItem,
                    visibleItemsCount,
                    configureView,
                    scrollListener,
                    selectionListener
            )
        }
    }

    private fun <T>inflateOptionViews(data: Array<T>,
                                      @LayoutRes optionRes: Int,
                                      selectedItem: T?,
                                      visibleItemsCount: Int,
                                      configureView: (T, View) -> Unit,
                                      scrollListener: ((position: Int, positionOffset: Float) -> Unit)?,
                                      selectionListener: Action<T>? = null) {
        removeAllViews()

        val innerLayout = LinearLayout(context)
        innerLayout.gravity = Gravity.BOTTOM
        innerLayout.layoutParams =
                FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        innerLayout.setBackgroundColor(Color.TRANSPARENT)
        addView(innerLayout)

        val inflater = LayoutInflater.from(context)

        val fakeCount = visibleItemsCount / 2
        for (i in 0 until fakeCount)
            innerLayout.addView(inflater.inflate(optionRes, innerLayout, false))

        data.forEach { element ->

            val option =
                    inflater.inflate(optionRes, innerLayout, false)
            configureView(element, option)

            innerLayout.addView(option)
        }

        for (i in 0 until fakeCount)
            innerLayout.addView(inflater.inflate(optionRes, innerLayout, false))

        val itemWidth = width / visibleItemsCount

        for (i in fakeCount until innerLayout.childCount - fakeCount)
            innerLayout[i].setOnClickListener {
                val offset = (i - fakeCount) * itemWidth

                smoothScrollTo(offset, 0)
                selectionListener?.invoke(data[i - fakeCount])
            }

        innerLayout.children.forEach {
            it.layoutParams.width = itemWidth
        }

        var prevScroll = 0

        scrollChangeListener = ViewTreeObserver.OnScrollChangedListener {
            val scroll = scrollX

            val position = (scroll / itemWidth)
            val itemPosition = position + fakeCount

            val positionOffset = scroll.rem(itemWidth) * 1f / itemWidth

            if (scrollListener != null && scroll != prevScroll) {
                scrollListener(
                        if (positionOffset < 0.5f) position else position + 1,
                        if (positionOffset < 0.5f) positionOffset * 2f else (1f - positionOffset) * 2)
            }

            prevScroll = scroll
            val colorView = colorView

            innerLayout.children.forEachIndexed { index, view ->

                val dx = (scroll) - ((index - fakeCount) * itemWidth)
                val diff = itemPosition - index
                val alpha =
                        if (diff <= visibleItemsCount / 2
                                && diff >= -visibleItemsCount / 2 - 1)
                            ((visibleItemsCount / 2 + 1) - (Math.abs(dx) * 1f / itemWidth)) / 2f
                        else
                            0f
                view.alpha = alpha

                val scale =
                        if (diff <= visibleItemsCount / 2
                                && diff >= -visibleItemsCount / 2 - 1)
                            Math.sqrt(alpha.toDouble()).toFloat()
                        else
                            0f
                view.scaleX = scale
                view.scaleY = scale

                val colorFraction = abs(dx * 1f / itemWidth)

                if (colorFraction <= 1f && colorView != null) {
                    val color = evaluator.evaluate(colorFraction, selectedColor, defaultColor) as Int
                    colorView(view, color)
                }
            }
        }

        viewTreeObserver.addOnScrollChangedListener(scrollChangeListener)

        setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP
                    || event?.action == MotionEvent.ACTION_CANCEL
                    || event?.action == MotionEvent.ACTION_OUTSIDE) {

                val scroll = scrollX
                val offset = (scroll * 1f / itemWidth)

                val diff = offset.rem(1)
                val value = if (diff < 0.5f) -diff else 1f - diff

                smoothScrollBy((value * itemWidth).toInt(), 0);
                selectionListener?.invoke(data[offset.roundToInt()])
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }

        val selectedIndex = data.indexOf(selectedItem)
        println(itemWidth)
        addSimpleOnLayoutChangeListener {
            scrollX =
                    if (selectedIndex > 0)
                        selectedIndex * itemWidth
                    else
                        1
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        viewTreeObserver?.removeOnScrollChangedListener(scrollChangeListener)
        scrollChangeListener = null
    }
}

