package com.pt29.amarts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.obsez.android.lib.filechooser.ChooserDialog
import com.pt29.amarts.classifier.*
import com.pt29.amarts.classifier.tensorflow.ImageClassifierFactory
import com.pt29.amarts.utils.getCroppedBitmap
import com.pt29.amarts.utils.getUriFromFilePath
import kotlinx.android.synthetic.main.activity_main.*
import me.grantland.widget.AutofitHelper
import java.io.File


private const val REQUEST_PERMISSIONS = 1
private const val REQUEST_TAKE_PICTURE = 2
val test = 0

class MainActivity : AppCompatActivity() {

    private val handler = Handler()
    private lateinit var classifier: Classifier
    private var photoFilePath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()
        AutofitHelper.create(textResult)
    }

    private fun checkPermissions() {
        if (arePermissionsAlreadyGranted()) {
            init()
        } else {
            Log.i("LOG28", "permission")
            requestPermissions()
        }
    }

    private fun arePermissionsAlreadyGranted() =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {

        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSIONS && arePermissionGranted(grantResults)) {
            init()
        } else {
            requestPermissions()
        }
    }

    private fun arePermissionGranted(grantResults: IntArray) =
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

    private fun init() {
        createClassifier()
//        takePhoto()
    }

    private fun createClassifier() {
        classifier = ImageClassifierFactory.create(
                assets,
                GRAPH_FILE_PATH,
                LABELS_FILE_PATH,
                IMAGE_SIZE,
                GRAPH_INPUT_NAME,
                GRAPH_OUTPUT_NAME
        )
    }

    private fun takePhoto() {
        photoFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/${System.currentTimeMillis()}.jpg"
        val currentPhotoUri = getUriFromFilePath(this, photoFilePath)

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
        takePictureIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PICTURE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {


        return when (item.itemId) {
            R.id.take_photo -> {
                takePhoto(); true
            }
            R.id.about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.take_file -> {
//                val otherStrings = arrayOf("0. ZERO", "1. ONE", "2. TWO", "3. THREE", "4. FOUR", "5. FIVE", "6. SIX", "7. SEVEN", "8. EIGHT", "9. NINE")
//
//                for (i in 0..100){
//                    val file = File("/storage/emulated/0/Download/numbers/${otherStrings[test]}/$test ($i).jpg")
//                    if (file.exists()){
//                        classifyPhoto(file)
//                    }
//
//                }


                ChooserDialog().with(this)
                        .withStartFile("/storage/emulated/0/Download/numbers/")
                        .withFilter(false, false, "jpg", "jpeg", "png")
                        .withChosenListener { path, pathFile ->
                            classifyPhoto(pathFile)
                        }
                        .build()
                        .show();

                true
            }


            else -> super.onOptionsItemSelected(item)

//                    Pix.start(this@MainActivity,
//                    REQUEST_TAKE_PICTURE,
//                    1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val file = File(photoFilePath)
        if (requestCode == REQUEST_TAKE_PICTURE && file.exists()) {
            classifyPhoto(file)
        }

//
//        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_TAKE_PICTURE) {
//            val returnValue = data!!.getStringArrayListExtra(Pix.IMAGE_RESULTS)
//            Log.i("LOG28","LOG $returnValue")
//        }
    }

    private fun classifyPhoto(file: File) {
        val photoBitmap = BitmapFactory.decodeFile(file.absolutePath)
        val croppedBitmap = getCroppedBitmap(photoBitmap)
        classifyAndShowResult(croppedBitmap)
        imagePhoto.setImageBitmap(photoBitmap)
    }

    private fun classifyAndShowResult(croppedBitmap: Bitmap) {
        runInBackground(
                Runnable {
                    val result = classifier.recognizeImage(croppedBitmap)
                    showResult(result)
                })
    }

    @Synchronized
    private fun runInBackground(runnable: Runnable) {
        handler.post(runnable)
    }

    private fun showResult(result: Result) {
//        val otherStrings = arrayOf("0 ZERO", "1 ONE", "2 TWO", "3 THREE", "4 FOUR", "5 FIVE", "6 SIX", "7 SEVEN", "8 EIGHT", "9 NINE")
//        if(!result.result.equals(otherStrings[test].toLowerCase()) || result.confidence < 0.6f)
//            Log.i("LOG29","$name - "+ result.result +" - "+result.confidence)
        if (result.confidence < 0.1f) {
            textResult.text = resources.getString(R.string.SORRY)
//            textResult.setTextColor( resources.getColor(R.color.BLACK))
//            textResult2.setTextColor( resources.getColor(R.color.BLACK))
//            layoutContainer.setBackgroundColor(resources.getColor(R.color.SORRY))

        } else {
            textResult.text = result.result.toUpperCase()
//            layoutContainer.setBackgroundColor(getColorFromResult(result.result))
        }
        textResult2.text = String.format(resources.getString(R.string.probability), result.confidence * 100)

        AutofitHelper.create(textResult2)
    }


}
