package com.vigil.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vigil.app.cci.CCIEngine
import com.vigil.app.ui.EegWaveformView
import com.vigil.app.ui.HeadSilhouettesView
import com.vigil.app.ui.IndicatorBoxesView
import com.vigil.app.ui.PowerButtonView
import com.vigil.app.ui.RecIndicatorView
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * VIGIL — pre-clinical EEG coherence detector (UI shell).
 *
 * Operation:
 *   Press the power button → run 3 sequential CCI checks within 3–5 s total →
 *   show result on the head silhouettes → auto-save a JPEG screenshot to the
 *   device gallery (Pictures/VIGIL).
 *
 * Validation mode:
 *   Tap a head silhouette to enter validation. Double-tap the green head to
 *   force a positive (coherence) outcome, or the red head to force a negative.
 *   Then press Start; the run uses the same flow but with a forced outcome.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var heads: HeadSilhouettesView
    private lateinit var eeg: EegWaveformView
    private lateinit var indicators: IndicatorBoxesView
    private lateinit var rec: RecIndicatorView
    private lateinit var powerButton: PowerButtonView
    private lateinit var clock: TextView
    private lateinit var validationBadge: TextView
    private lateinit var root: View

    private val handler = Handler(Looper.getMainLooper())

    private var running = false

    /** Validation outcome to apply to the next run, or null for normal operation. */
    private var validationOutcome: Boolean? = null

    /** Tracks the head silhouette currently selected (for double-tap detection). */
    private var lastTapTime: Long = 0L
    private var lastTapSide: HeadSilhouettesView.Result = HeadSilhouettesView.Result.NONE
    private val DOUBLE_TAP_MS = 400L

    private val clockTicker = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.rootContainer)
        heads = findViewById(R.id.heads)
        eeg = findViewById(R.id.eeg)
        indicators = findViewById(R.id.indicators)
        rec = findViewById(R.id.rec)
        powerButton = findViewById(R.id.powerButton)
        clock = findViewById(R.id.clock)
        validationBadge = findViewById(R.id.validationBadge)

        powerButton.setOnClickListener {
            if (!running) startCheckSequence()
        }

        heads.onHeadTap = { side -> onHeadTapped(side) }

        rec.recording = true
        updateClock()
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockTicker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockTicker)
    }

    private fun updateClock() {
        clock.text = SimpleDateFormat("HH:mm", Locale.US).format(Date())
    }

    /**
     * Validation gesture handling.
     *
     * - First tap: enter validation mode and select a side (green or red).
     * - Double-tap on the same side within DOUBLE_TAP_MS: arm the forced outcome
     *   for the next run.
     * - Tap on the opposite side: switch the selection.
     */
    private fun onHeadTapped(side: HeadSilhouettesView.Result) {
        if (running) return
        val now = System.currentTimeMillis()
        if (side == lastTapSide && now - lastTapTime < DOUBLE_TAP_MS) {
            // Double-tap → arm the forced outcome.
            validationOutcome = (side == HeadSilhouettesView.Result.POSITIVE)
            heads.validationSelection = side
            validationBadge.visibility = View.VISIBLE
            val msg = if (validationOutcome == true)
                "Validation: forcing COHERENCE — press Start"
            else
                "Validation: forcing NO COHERENCE — press Start"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        } else {
            // First tap on a side — preview selection but do not arm yet.
            heads.validationSelection = side
            validationBadge.visibility = View.VISIBLE
            validationBadge.text = "VALIDATION • SELECT"
        }
        lastTapTime = now
        lastTapSide = side
    }

    private fun exitValidation() {
        validationOutcome = null
        heads.validationSelection = HeadSilhouettesView.Result.NONE
        validationBadge.visibility = View.GONE
        validationBadge.text = "VALIDATION"
        lastTapSide = HeadSilhouettesView.Result.NONE
    }

    private fun startCheckSequence() {
        running = true
        powerButton.active = true
        eeg.active = true
        heads.result = HeadSilhouettesView.Result.NONE
        indicators.litCount = 0
        indicators.litColors = arrayOf(null, null, null)

        // 3 checks within 3–5 s total. Pick total duration in [3000, 5000] ms.
        val total = 3000 + (Math.random() * 2000).toLong() // 3000..5000
        val perCheck = total / 3
        val outcomes = mutableListOf<Boolean>()

        val forced = validationOutcome
        for (i in 0 until 3) {
            handler.postDelayed({
                val res = CCIEngine.runCheck(forceCoherent = forced)
                outcomes += res.coherent
                indicators.litCount = i + 1
                Log.d(TAG, "Check ${i + 1}: coherent=${res.coherent} score=${"%.3f".format(res.score)}")
            }, perCheck * (i + 1))
        }

        handler.postDelayed({
            // Majority vote across the three checks.
            val positives = outcomes.count { it }
            val finalResult = if (positives >= 2)
                HeadSilhouettesView.Result.POSITIVE
            else
                HeadSilhouettesView.Result.NEGATIVE
            heads.result = finalResult

            // Capture screenshot AFTER UI has reflected the result.
            handler.postDelayed({
                captureScreenshotToGallery()
                running = false
                powerButton.active = false
                eeg.active = false
                exitValidation()
            }, 350)
        }, total + 50)
    }

    private fun captureScreenshotToGallery() {
        try {
            val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            root.draw(canvas)

            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val name = "VIGIL_$ts.jpg"

            val uri: Uri? = saveBitmapToMediaStore(bitmap, name)
            if (uri != null) {
                Toast.makeText(this, "Saved: $name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Screenshot save failed", Toast.LENGTH_SHORT).show()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "screenshot failed", t)
            Toast.makeText(this, "Screenshot error: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap, name: String): Uri? {
        val resolver = contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VIGIL")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val uri = resolver.insert(collection, values) ?: return null
        var stream: OutputStream? = null
        try {
            stream = resolver.openOutputStream(uri) ?: return null
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        } finally {
            stream?.close()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    companion object {
        private const val TAG = "VIGIL"
    }
}
