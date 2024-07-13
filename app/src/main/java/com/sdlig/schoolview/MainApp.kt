package com.sdlig.schoolview

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainApp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var takePhotoBtn: FloatingActionButton
    private lateinit var redoPhotoBtn: FloatingActionButton
    private lateinit var logOutBtn: FloatingActionButton
    private lateinit var ivDisplayImage: ImageView
    private lateinit var etNotesTitle: AppCompatEditText

    private var currentPhotoPath: String? = null

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val REQUEST_IMAGE_CAPTURE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_app)

        // Initialize views
        takePhotoBtn = findViewById(R.id.takePhotoBtn)
        redoPhotoBtn = findViewById(R.id.redoPhotoBtn)
        logOutBtn = findViewById(R.id.logOut)
        ivDisplayImage = findViewById(R.id.ivDisplayImage)
        etNotesTitle = findViewById(R.id.etNotesTitle)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore

        // Set initial date in EditText
        etNotesTitle.setText(getCurrentDate())

        // Set click listeners
        takePhotoBtn.setOnClickListener {
            if (currentPhotoPath == null) {
                if (checkCameraPermission()) {
                    dispatchTakePictureIntent()
                }
            } else {
                uploadImageToFirebaseStorage(currentPhotoPath.toString())
            }
        }

        redoPhotoBtn.setOnClickListener {
            currentPhotoPath = null
            ivDisplayImage.setImageDrawable(null) // Clear the image view
        }

        logOutBtn.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, Auth::class.java))
            finish()
        }
    }

    private fun getCurrentDate(): String {
        val today = Calendar.getInstance()
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(today.time)
    }

    private fun checkCameraPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            false
        } else {
            true
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: Exception) {
                    Log.e("MainApp", "Error creating image file", ex)
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.provider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                ivDisplayImage.setImageURI(Uri.fromFile(File(path)))
            }
        }
    }

    private fun uploadImageToFirebaseStorage(imagePath: String) {
        val file = Uri.fromFile(File(imagePath))
        val storageRef = Firebase.storage.reference
        val imagesRef = storageRef.child("images/${auth.currentUser?.uid}/${file.lastPathSegment}")

        val uploadTask = imagesRef.putFile(file)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()

                db.collection("users")
                    .whereEqualTo("uid", auth.currentUser!!.uid)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val document = querySnapshot.documents[0]
                            val drafts = document.get("drafts") as? ArrayList<Any> ?: ArrayList()

                            val text = etNotesTitle.text.toString()

                            val hashMap = hashMapOf(
                                "url" to downloadUrl,
                                "title" to text
                            )
                            drafts.add(hashMap)

                            val updates = hashMapOf(
                                "drafts" to drafts
                            )
                            document.reference.set(updates, SetOptions.merge())
                                .addOnSuccessListener {
                                    Log.d("MainApp", "Drafts updated successfully")
                                    Toast.makeText(this, "Drafts updated successfully", Toast.LENGTH_SHORT).show()
                                    currentPhotoPath = null
                                    ivDisplayImage.setImageDrawable(null)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("MainApp", "Error updating drafts", e)
                                    Toast.makeText(this, "Error updating drafts: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Log.e("MainApp", "Document not found")
                            Toast.makeText(this, "Document not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainApp", "Error querying document", e)
                        Toast.makeText(this, "Error querying document: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }.addOnFailureListener { e ->
                Log.e("MainApp", "Error getting download URL", e)
                Toast.makeText(this, "Error getting download URL: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { e ->
            Log.e("MainApp", "Error uploading image", e)
            Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
