package com.example.registrationapi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.util.Size
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.Executors
private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.GET_ACCOUNTS,
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.INTERNET
)

class MainActivity : AppCompatActivity(), LifecycleOwner {
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    lateinit var shareImage : Uri
    lateinit var file_uri : Uri
    var camera = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewFinder = findViewById(R.id.view_finder)
        if (allPermissionsGranted()) {
            viewFinder.post {
                startCamera()
            }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
        location_btn.setOnClickListener {
            if (allPermissionsGranted()){
                 mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                val locAPI : LocationAPI = LocationAPI(mFusedLocationClient, this, location_et)
                locAPI.getLastLocation()
            }
        }
        account_btn.setOnClickListener {
            if (allPermissionsGranted()){
                val am: AccountManager = getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
                val accounts: Array<out Account> = am.getAccountsByType("com.google")
                var account_list = mutableListOf<String>()
                for (ac in accounts){
                    account_list.add(ac.name)
                }
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Choose an email")
                val emails =  account_list.toTypedArray()
                builder.setItems(emails) { dialog, which ->
                    email_et.setText(account_list[which])
                }
                val dialog = builder.create()
                dialog.show()
            }else{
                Toast.makeText(this, "Allow access to accounts.", Toast.LENGTH_SHORT).show()
            }
        }

        gallery_btn.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 114)
        }

        register_btn.setOnClickListener {
            val param  = "uname=${username_et.text}&" +
                    "fname=${fname_et.text}&" +
                    "lname=${lname_et.text}&" +
                    "pnumber=${pnumber_et.text}&" +
                    "location=${location_et.text}&" +
                    "email=${email_et.text}"

            val httpTask = HTTPTask(this)
            httpTask.execute("post", param)
        }

        upload_btn.setOnClickListener {
            FileUpload(this).execute(shareImage)
        }
        file_btn.setOnClickListener {
            val READ_REQUEST_CODE: Int = 42
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
            }

            startActivityForResult(intent, READ_REQUEST_CODE)
        }

        upload_file_btn.setOnClickListener {
            PDFUpload(this).execute(file_uri)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 114 && resultCode == RESULT_OK) {
            if (data != null && data.data != null) {
                val ur: Uri = data.data
                shareImage  = data.data
                camera = false
               img_iv.setImageURI(ur)
            }
        }
        if (requestCode == 42 && resultCode == RESULT_OK) {
            if (data != null && data.data != null) {
                val ur: Uri = data.data
                file_uri = data.data
                dumpImageMetaData(file_uri)

            }
        }
    }
    private fun dumpImageMetaData(uri: Uri) {

        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        val cursor: Cursor? = contentResolver.query( uri, null, null, null, null, null)

        cursor?.use {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (it.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                val displayName: String =
                    it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                Toast.makeText(this, displayName, Toast.LENGTH_LONG).show()
                file_et.setText(displayName)

                val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                // If the size is unknown, the value stored is null.  But since an
                // int can't be null in Java, the behavior is implementation-specific,
                // which is just a fancy term for "unpredictable".  So as
                // a rule, check if it's null before assigning to an int.  This will
                // happen often:  The storage API allows for remote files, whose
                // size might not be locally known.
                val size: String = if (!it.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    it.getString(sizeIndex)
                } else {
                    "Unknown"
                }
            }
        }
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView

    private fun startCamera() {
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(200, 200))
        }.build()
        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)
            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)
        camera_btn.setOnClickListener {
            val file = File(externalMediaDirs.first(),
                "${System.currentTimeMillis()}.jpg")

            imageCapture.takePicture(file, executor,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(
                        imageCaptureError: ImageCapture.ImageCaptureError,
                        message: String,
                        exc: Throwable?
                    ) {
                        val msg = "Photo capture failed: $message"
                        Log.e("CameraXApp", msg, exc)
                        viewFinder.post {
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onImageSaved(file: File) {

                        val msg = "Photo capture succeeded: ${file.absolutePath}"
                        Log.d("CameraXApp", msg)
                        viewFinder.post {
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            img_iv.setImageURI(Uri.fromFile(file))
                            shareImage = Uri.fromFile(file)
                            img_iv.visibility = View.VISIBLE
                            viewFinder.visibility  = View.GONE
                            CameraX.unbindAll()
                        }
                    }
                })
        }
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }
    private fun updateTransform() {
        val matrix = Matrix()
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
        viewFinder.setTransform(matrix)
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


}
