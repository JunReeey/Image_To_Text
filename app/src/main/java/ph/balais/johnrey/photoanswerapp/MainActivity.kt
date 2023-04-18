package ph.balais.johnrey.photoanswerapp


import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ph.balais.johnrey.photoanswerapp.databinding.ActivityMainBinding
import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.view.Menu
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var inputImgBtn : MaterialButton
    private lateinit var recognizedTextBtn :MaterialButton
    private lateinit var imageIv : ImageView
    private lateinit var recognizedTextEt : EditText

    private lateinit var progressDialog : ProgressDialog

    private lateinit var textRecognizer : TextRecognizer


    private companion object{
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
    }

    //?Uri of the image that take from Camera/Gallery
    private var imageUri: Uri? = null

    //arrays of permission required to pick image from Camera/Gallery
    private lateinit var  cameraPermissions: Array<String>
    private lateinit var storagePermisions: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init UI views
        inputImgBtn = binding.inputImgBtn
        recognizedTextBtn = binding.textRecognitionBtn
        imageIv = binding.imgIv
        recognizedTextEt = binding.recognizedText

        //init arrays of permissions
        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermisions= arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setCanceledOnTouchOutside(false)

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        inputImgBtn.setOnClickListener {
            showInputImageDialog()
        }

        recognizedTextBtn.setOnClickListener{
            if(imageUri == null){
                showToast("Pick image First!")
            }
            else{
                recognizeTextFromImage()
            }
        }
    }

    private fun recognizeTextFromImage() {
        progressDialog.setMessage("Preparing Image...")
        progressDialog.show()

        try{
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            progressDialog.setMessage("Recognizing text...")

            val textTaskResult = textRecognizer.process(inputImage)
                .addOnSuccessListener {text ->
                    progressDialog.dismiss()
                    val recognizedText = text.text

                    recognizedTextEt.setText(recognizedText)

                }
                .addOnFailureListener {e ->

                    progressDialog.dismiss()
                    showToast("Failed to recognize text due to ${e.message}")

                }
        }
        catch (e:Exception){
            progressDialog.dismiss()
            showToast("Failed to prepare image due to ${e.message}")
        }
    }

    private fun showInputImageDialog() {
        val popupMenu = PopupMenu(this, inputImgBtn)

        popupMenu.menu.add(Menu.NONE, 1, 1, "CAMERA")
        popupMenu.menu.add(Menu.NONE, 2, 2, "GALLERY")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener {menuItem ->

            val id = menuItem.itemId
            if(id == 1){

                if(checkCameraPermissions()){
                    pickImageCamera()
                }
                else{
                    requestCameraPermission()
                }
            }
            else if(id == 2){
                if(checkStoragePermissions()){
                    pickImgGallery()
                }else{
                    requestStoragePermission()
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun pickImgGallery(){
        val intent= Intent(Intent.ACTION_PICK)
         intent.type ="image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if(result.resultCode == Activity.RESULT_OK){
                val data =result.data
                imageUri = data!!.data

                imageIv.setImageURI(imageUri)
            }else{
                showToast("cancelled...")
            }
        }

    private fun pickImageCamera(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "sample Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)

        val intent =Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)

    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->

            if(result.resultCode == Activity.RESULT_OK){

                imageIv.setImageURI(imageUri)

            }else{
                showToast("cancelled...")
            }
        }

    private fun checkStoragePermissions(): Boolean{

        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    }

    private fun checkCameraPermissions(): Boolean{

       val cameraResult =  ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
       val storageResult =  ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        return cameraResult && storageResult

    }

    private fun requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermisions, STORAGE_REQUEST_CODE)

    }

    private fun requestCameraPermission(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_REQUEST_CODE ->{
                if (grantResults.isNotEmpty()){
                    pickImageCamera()
                }else{
                    showToast("Camera and Storage permission is required!")
                }
            }
            STORAGE_REQUEST_CODE ->{
                if(grantResults.isNotEmpty()){
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (storageAccepted){
                        pickImgGallery()
                    }
                }else{
                    showToast("Storage Permission is required!")
                }
            }
        }
    }

    private fun showToast(message:String){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}