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
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

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

        // Check if user is authenticated
        val user: FirebaseUser? = auth.currentUser

        // Set click listeners
        takePhotoBtn.setOnClickListener {
            if(currentPhotoPath == null) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                } else {
                    checkCameraPermission()
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
            val intent = Intent(this, Auth::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: Exception) {
                    // Error occurred while creating the File
                    Log.e("MainApp", "Error creating image file", ex)
                    null
                }
                // Continue only if the File was successfully created
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
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
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
            // Image captured and saved to fileUri specified in the Intent
            currentPhotoPath?.let { path ->
                // Update ImageView with the captured image
                ivDisplayImage.setImageURI(Uri.fromFile(File(path)))

                // Upload image to Firebase Storage and save URL to Firestore
            }
        }
    }

    private fun uploadImageToFirebaseStorage(imagePath: String) {
        val file = Uri.fromFile(File(imagePath))
        val storageRef = Firebase.storage.reference
        val imagesRef = storageRef.child("images/${auth.currentUser?.uid}/${file.lastPathSegment}")

        val uploadTask = imagesRef.putFile(file)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Image uploaded successfully, get download URL
            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()

                db.collection("users")
                    .whereEqualTo("uid", auth.currentUser!!.uid)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val document = querySnapshot.documents[0]
                            val drafts = document.get("drafts") as? ArrayList<Any> ?: ArrayList()

                            var text = etNotesTitle.text.toString()

                            val today = Calendar.getInstance()
                            val sendDateUAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(today.time)

                            etNotesTitle.setText(sendDateUAT.toString())

                            // Add download URL to "drafts" array
                            val hashMap = hashMapOf<String, Any>(
                                "url" to downloadUrl,
                                "title" to text
                            )
                            drafts.add(hashMap)

                            // Update "drafts" array in Firestore using merge operation
                            val updates = hashMapOf<String, Any>(
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
