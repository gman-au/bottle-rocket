package au.com.gman.bottlerocket
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class PageCaptureOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint().apply {
        color = context.getColor(R.color.debug_text)
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private var pageBoundingBox: Path? = null
    private var qrCodeBoundingBox: Path? = null

    fun setPageOverlayPath(path: Path?) {
        pageBoundingBox = path
        // Invalidate the view to trigger a redraw
        postInvalidate()
    }

    fun setQrOverlayPath(path: Path?) {
        qrCodeBoundingBox = path
        // Invalidate the view to trigger a redraw
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pageBoundingBox?.let { canvas.drawPath(it, paint) }
        qrCodeBoundingBox?.let { canvas.drawPath(it, paint) }
    }
}
