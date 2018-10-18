package one.mixin.android.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import one.mixin.android.R
import org.jetbrains.anko.dip

class CameraOpView : View {

    private enum class Mode {
        NONE,
        EXPAND,
        PROGRESS
    }

    private var ringColor = Color.WHITE
    private var circleColor = context.getColor(R.color.colorDarkBlue)
    private var ringStrokeWidth = dip(5).toFloat()
    private var circleWidth = -10f // initial value less than 0 for delay
    private var maxCircleWidth = 0f
    private var circleInterval = 3f
    private var progressStartAngle = -90f
    private var curSweepAngle = 0f
    private var progressRect: RectF? = null
    private var expand = 1.1f
    private var radius = 0f
    private var rawRadius = 0f
    private var midX = 0f
    private var midY = 0f
    private var progressInterval = 0f

    private var mode = Mode.NONE

    private var callback: CameraOpCallback? = null

    private val mHandler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == 1) {
                    curSweepAngle += progressInterval
                    if (curSweepAngle > 360) {
                        curSweepAngle = 360f
                        invalidate()
                        stop()
                        callback?.onProgressStop(curSweepAngle / progressInterval / 10)
                        clean()
                    } else {
                        invalidate()
                    }
                }
            }
        }
    }

    private val task by lazy {
        object : Runnable {
            override fun run() {
                mHandler.sendEmptyMessage(1)
                mHandler.postDelayed(this, 100)
            }
        }
    }

    private fun start() {
        task.run()
    }

    private fun stop() {
        mHandler.removeCallbacks(task)
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ringColor
        strokeWidth = ringStrokeWidth
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = circleColor
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private fun animateValue(form: Float, to: Float) {
        val anim = ValueAnimator.ofFloat(form, to)
        anim.addUpdateListener { valueAnimator ->
            radius = valueAnimator.animatedValue as Float * rawRadius
            invalidate()
        }
        anim.duration = 200
        anim.start()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (progressRect == null) {
            val size = width / 1.5f - ringStrokeWidth
            rawRadius = size / 2
            radius = rawRadius
            maxCircleWidth = radius - ringStrokeWidth
            midX = width / 2f
            midY = width / 2f
            val maxRadius = rawRadius * expand
            progressRect = RectF(midX - maxRadius, midY - maxRadius, midX + maxRadius, midY + maxRadius)
        }
    }

    override fun onDraw(canvas: Canvas) {
        ringPaint.color = ringColor
        if (mode == Mode.NONE) {
            canvas.drawCircle(midX, midY, radius, ringPaint)
        } else if (mode == Mode.EXPAND) {
            canvas.drawCircle(midX, midY, radius, ringPaint)
            canvas.drawCircle(midX, midY, circleWidth, circlePaint)
            circleWidth += circleInterval
            if (circleWidth <= maxCircleWidth) {
                invalidate()
            }
        } else {
            canvas.drawCircle(midX, midY, radius, ringPaint)
            ringPaint.color = circleColor
            canvas.drawArc(progressRect!!, progressStartAngle, curSweepAngle, false, ringPaint)
            canvas.drawCircle(midX, midY, maxCircleWidth, circlePaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animateValue(1f, expand)
                mode = Mode.EXPAND

                postDelayed(longPressRunnable, 500)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (mode == Mode.PROGRESS) {
                    stop()
                    callback?.onProgressStop(curSweepAngle / progressInterval / 10)
                    clean()
                } else if (mode == Mode.EXPAND) {
                    removeCallbacks(longPressRunnable)
                    if (!post(clickRunnable)) {
                        clickInternal()
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
                if (mode == Mode.PROGRESS) {
                    stop()
                    callback?.onProgressStop(curSweepAngle / progressInterval / 10)
                    clean()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private val longPressRunnable = Runnable {
        mode = Mode.PROGRESS
        start()
        callback?.onProgressStart()
    }

    private val clickRunnable = Runnable {
        clickInternal()
    }

    private fun clickInternal() {
        animateValue(expand, 1f)
        clean()
        callback?.onClick()
    }

    private fun clean() {
        mode = Mode.NONE
        curSweepAngle = 0f
        circleWidth = -10f
        radius = rawRadius
        invalidate()
    }

    fun setMaxDuration(duration: Int) {
        progressInterval = 360f / (duration + 0.5f) / 10
    }

    fun setCameraOpCallback(callback: CameraOpCallback) {
        this.callback = callback
    }

    interface CameraOpCallback {
        fun onClick()
        fun onProgressStart()
        fun onProgressStop(time: Float)
    }
}