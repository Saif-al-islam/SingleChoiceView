package com.saif.singlechoiceview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

private typealias OnSelectionChangeListener = (selectionIndex: Int, selectedId: Int?) -> Unit
// todo re-select item change the textColor.
class SingleChoiceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var indexOfSelectedChild = -1
    private var prevIndexOfSelectedChild = -1
    private var bckWidth: Int = 0
    private var bckTargetXPosition: Float = 0f
    private var bckCurrentXPosition: Float = 0f
    private var animationDurationInMillis = 400
    private var bckCurrentAnimateSpeed = 0f
    private var bckAnimateSpeed = 30f
        set(value) {
            bckCurrentAnimateSpeed = value
            field = value
        }
    private var bckColor = Color.RED
    private var selectedTextColor = Color.WHITE
    private var unSelectedTextColor = Color.BLACK
    private var bckCornerRadius = 12f
    private var bckPaint: Paint = Paint()
    var listener: OnSelectionChangeListener? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SingleChoiceView,
            0, 0
        ).apply {

            try {
                indexOfSelectedChild = getInteger(R.styleable.SingleChoiceView_selectedIndex, 0)
                bckCornerRadius = getDimension(R.styleable.SingleChoiceView_selectionBckCorner, 12f)
                bckColor = getColor(R.styleable.SingleChoiceView_selectionBckColor, Color.GRAY)
                selectedTextColor =
                    getColor(R.styleable.SingleChoiceView_SelectedTextColor, Color.WHITE)
                unSelectedTextColor =
                    getColor(R.styleable.SingleChoiceView_unSelectedTextColor, Color.BLACK)
                animationDurationInMillis =
                    getInteger(R.styleable.SingleChoiceView_animationDurationInMillis, 400)

                setBckPaint(bckPaint, bckColor)

            } finally {
                recycle()
            }
        }
    }


    override fun addView(child: View?, index: Int) {
        if (child !is TextView)
            throw UnsupportedOperationException("this class is not supported. child must be TextView")

        child.apply {
            setBackgroundResource(0)
            setTextColor(if (indexOfSelectedChild == index) selectedTextColor else unSelectedTextColor)
        }

        super.addView(child, index)
    }

    private fun drawInitSelectedChild() {
        if (bckWidth == 0) {
            getChildAt(indexOfSelectedChild)?.let { child ->
                onSelectionChange(child)
                Log.d(
                    "saif",
                    "drawInitSelectedChild: indexOfSelectedChild= $indexOfSelectedChild, child= ${child.x}"
                )
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        drawInitSelectedChild()

        Log.d(
            "saif",
            "onDraw: consts: bckWidth= $bckWidth,   bckTargetXPosition= $bckTargetXPosition"
        )
        Log.d(
            "saif",
            "onDraw: bckCurrentXPosition= $bckCurrentXPosition,   bckCurrentAnimateSpeed= $bckCurrentAnimateSpeed,   bckAnimateSpeed= $bckAnimateSpeed"
        )

        canvas?.drawBckRect()

        Log.d(
            "saif",
            "onDraw: diff between target,current= ${bckCurrentXPosition - bckTargetXPosition}"
        )
        // this is stop condition.
        if (stopBckAnimationCondition()) {
            Log.d("saif", "onDraw: stop drawing")
            super.dispatchDraw(canvas)
            return
        }

        if (abs(bckCurrentXPosition - bckTargetXPosition) < bckAnimateSpeed) {
            bckCurrentAnimateSpeed = abs(bckCurrentXPosition - bckTargetXPosition)
        }

        when {
            bckCurrentXPosition < bckTargetXPosition -> {
                bckCurrentXPosition += bckCurrentAnimateSpeed
                invalidate()
            }
            bckCurrentXPosition > bckTargetXPosition -> {
                bckCurrentXPosition -= bckCurrentAnimateSpeed
                invalidate()
            }
        }

        super.dispatchDraw(canvas)
    }

    private fun Canvas.drawBckRect() {
        drawRoundRect(
            bckCurrentXPosition,
            0f,
            bckCurrentXPosition + bckWidth,
            height.toFloat(),
            bckCornerRadius,
            bckCornerRadius,
            bckPaint
        )
    }

    private fun stopBckAnimationCondition() = (bckCurrentXPosition - bckTargetXPosition == 0f)

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_UP -> {
                Log.d(
                    "saif",
                    "onTouchEvent: ACTION_UP:  x= ${event?.x}, y= ${event?.y}, action= ${event?.action}"
                )
                getChildAtLocation(event.x, event.y)?.let { clickedChild ->
                    onSelectionChange(clickedChild)
                    Log.d(
                        "saif",
                        "onTouchEvent: bckWidth= $bckWidth,   bckTargetXPosition= $bckTargetXPosition"
                    )
                    invalidate()
                }
            }
        }

        return true
    }


    private fun getChildAtLocation(x: Float, y: Float): View? {
        for (i: Int in 0..childCount) {
            getChildAt(i)?.let { child ->
                val childX = child.x
                if (x in childX..(childX + child.width)) {
                    if (indexOfSelectedChild != i) {
                        prevIndexOfSelectedChild = indexOfSelectedChild
                        indexOfSelectedChild = i
                    }

                    Log.d("saif", "getChildAtLocation: indexOfSelectedChild= $indexOfSelectedChild")
                    return child
                }
            }

        }

        return null
    }

    private fun setBckPaint(paint: Paint, colorValue: Int) = with(paint) {
        color = colorValue
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private fun onSelectionChange(clickedChild: View) {
        bckWidth = clickedChild.width
        bckTargetXPosition = clickedChild.x
        bckCurrentAnimateSpeed = bckAnimateSpeed

        (clickedChild as TextView).setTextColor(selectedTextColor)

        (getChildAt(prevIndexOfSelectedChild) as? TextView)?.let {
            it.setTextColor(unSelectedTextColor)
        }

        listener?.invoke(indexOfSelectedChild, clickedChild.id)
    }

    private fun calcAnimationSpeed(timeInMillis: Int, distance: Float): Float =
        distance / timeInMillis

    fun indexOfSelectedItem() = indexOfSelectedChild

    fun getIdOfSelectedItem() = getChildAt(indexOfSelectedChild).id

}