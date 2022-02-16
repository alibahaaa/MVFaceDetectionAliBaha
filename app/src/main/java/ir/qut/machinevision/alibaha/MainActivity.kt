package ir.qut.machinevision.alibaha

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.camerakit.CameraKitView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions


class MainActivity : AppCompatActivity() {

    private lateinit var btnDetectObject: Button
    private lateinit var cameraView: CameraKitView
    private lateinit var graphicOverlay: GraphicOverlay

    private lateinit var alertDialog: AlertDialog

    private var isCaptured = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraView = findViewById(R.id.camera_view)
        btnDetectObject = findViewById(R.id.btn_detect)
        graphicOverlay = findViewById(R.id.graphic_overlay)

        alertDialog = AlertDialog.Builder(this)
            .setMessage("Please wait...")
            .setCancelable(false)
            .create();

        btnDetectObject.setOnClickListener {
            if (!isCaptured) {
                alertDialog.show()
                cameraView.captureImage { cameraKitView, byteArray ->
                    cameraView.onStop()
                    var bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray?.size ?: 0)
                    bitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        cameraView?.width ?: 0,
                        cameraView?.height ?: 0,
                        false
                    )
                    runDetector(bitmap)
                }
                graphicOverlay.clear()
            } else {
                alertDialog.dismiss()
                if (Build.VERSION.SDK_INT >= 11) {
                    recreate()
                } else {
                    val intent = intent
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    finish()
                    overridePendingTransition(0, 0)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            }
            isCaptured = !isCaptured

            btnDetectObject.text = if (isCaptured) "Reset" else "Detect Faces"
        }
    }

    private fun runDetector(bitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .build()

        val detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(options)

        detector.detectInImage(image)
            .addOnSuccessListener { faces ->
                processFaceResult(faces)

            }.addOnFailureListener {
                it.printStackTrace()
            }
    }

    private fun processFaceResult(faces: MutableList<FirebaseVisionFace>) {
        faces.forEach {
            val bounds = it.boundingBox
            val rectOverLay = RectOverlay(graphicOverlay, bounds)
            graphicOverlay.add(rectOverLay)
        }
        alertDialog.dismiss()
    }

    override fun onResume() {
        super.onResume()
        cameraView.onStart()
    }

    override fun onPause() {
        cameraView.onPause()
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions!!, grantResults!!)
        cameraView.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}