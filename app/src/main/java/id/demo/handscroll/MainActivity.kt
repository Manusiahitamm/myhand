package id.demo.handscroll

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var preview: PreviewView
    private lateinit var status: TextView
    private var recognizer: GestureRecognizer? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    private var lastGesture = ""
    private var lastSwipeAt = 0L

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else status.text = "Izin kamera diperlukan."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        preview = PreviewView(this).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE; layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        status = TextView(this).apply { text = "Siap. Aktifkan layanan aksesibilitas terlebih dahulu." }
        layout.addView(preview); layout.addView(status)
        layout.addView(Button(this).apply { text = "1. Pengaturan aksesibilitas"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
        layout.addView(Button(this).apply { text = "2. Mulai kamera dan deteksi"; setOnClickListener { requestCamera() } })
        layout.addView(Button(this).apply { text = "Aktifkan kontrol scroll"; setOnClickListener { ScrollAccessibilityService.enabledByUser = true; status.text = "Scroll aktif: gesture Victory memicu swipe." } })
        layout.addView(Button(this).apply { text = "BERHENTI"; setOnClickListener { running = false; ScrollAccessibilityService.enabledByUser = false; status.text = "Kontrol berhenti." } })
        setContentView(layout)
        try {
            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("gesture_recognizer.task").build())
                .setRunningMode(RunningMode.IMAGE).setNumHands(1).build()
            recognizer = GestureRecognizer.createFromOptions(this, options)
        } catch (e: Exception) { status.text = "Model AI belum dimuat: ${e.message}" }
    }

    private fun requestCamera() {
        if (recognizer == null) { status.text = "Model gesture_recognizer.task tidak ditemukan di assets."; return }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val useCase = Preview.Builder().build().also { it.setSurfaceProvider(preview.surfaceProvider) }
                provider.unbindAll(); provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, useCase)
                running = true; status.text = "Kamera aktif. Gesture Victory ✌️ memicu swipe saat scroll aktif."
                handler.post(frameLoop)
            } catch (e: Exception) { status.text = "Kamera gagal: ${e.message}" }
        }, ContextCompat.getMainExecutor(this))
    }

    // Contoh sederhana. Untuk produksi gunakan ImageAnalysis agar lebih efisien.
    private val frameLoop = object : Runnable {
        override fun run() {
            if (!running || isFinishing) return
            val bitmap = preview.bitmap
            if (bitmap != null) executor.execute {
                try {
                    val result = recognizer?.recognize(BitmapImageBuilder(bitmap).build())
                    bitmap.recycle()
                    if (result != null) runOnUiThread { onResult(result) }
                } catch (_: Exception) { if (!bitmap.isRecycled) bitmap.recycle() }
            }
            handler.postDelayed(this, 400L)
        }
    }

    private fun onResult(result: GestureRecognizerResult) {
        val name = result.gestures().firstOrNull()?.firstOrNull()?.categoryName() ?: "None"
        status.text = "Gesture: $name | Scroll: ${if (ScrollAccessibilityService.enabledByUser) "AKTIF" else "mati"}"
        val now = System.currentTimeMillis()
        if (name == "Victory" && lastGesture != "Victory" && now - lastSwipeAt > 1600L) {
            if (ScrollAccessibilityService.enabledByUser) { ScrollAccessibilityService.requestSwipe(); lastSwipeAt = now }
        }
        lastGesture = name
    }

    override fun onDestroy() {
        running = false; handler.removeCallbacksAndMessages(null); recognizer?.close(); executor.shutdown(); super.onDestroy()
    }
}
