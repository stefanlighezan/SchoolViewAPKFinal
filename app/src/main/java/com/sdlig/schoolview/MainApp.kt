package com.sdlig.schoolview

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class MainApp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var takePhotoBtn: FloatingActionButton
    private lateinit var redoPhotoBtn: FloatingActionButton
    private lateinit var logOutBtn: FloatingActionButton
    private lateinit var ivDisplayImage: ImageView
    private var bitmap: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_app)

        //initialize views
        takePhotoBtn = findViewById(R.id.takePhotoBtn)
        redoPhotoBtn = findViewById(R.id.redoPhotoBtn)
        logOutBtn = findViewById(R.id.logOut)
        ivDisplayImage = findViewById(R.id.ivDisplayImage)
        // end

        //Initialize and make sure the client is authorized with Firebase
        auth = Firebase.auth
        db = Firebase.firestore
        val user: FirebaseUser? = auth.currentUser
        // end

        takePhotoBtn.setOnClickListener {
            if (bitmap == null) {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(takePictureIntent, 1)
            } else {
                // Send to cloud storage and firestore
                val storageRef = Firebase.storage.reference
                val imagesRef = storageRef.child("images/${user?.uid}/${System.currentTimeMillis()}.jpg")

                val baos = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)

                val data = baos.toByteArray()

                val uploadTask = imagesRef.putBytes(data)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Successfully uploaded image, get download URL
                    imagesRef.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()

                        // Query Firestore to find document with matching UID
                        db.collection("users")
                            .whereEqualTo("uid", user!!.uid)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    val document = querySnapshot.documents[0]
                                    val drafts = document.get("drafts") as? ArrayList<String> ?: ArrayList()

                                    // Add download URL to "drafts" array
                                    drafts.add(downloadUrl)

                                    // Update "drafts" array in Firestore using merge operation
                                    val updates = hashMapOf<String, Any>(
                                        "drafts" to drafts
                                    )
                                    document.reference.set(updates, SetOptions.merge())
                                        .addOnSuccessListener {
                                            Log.d("MainApp", "Drafts updated successfully")
                                            Toast.makeText(this, "Drafts updated successfully", Toast.LENGTH_SHORT).show()
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




        redoPhotoBtn.setOnClickListener {
            bitmap = null
            ivDisplayImage.setImageBitmap(null)
        }


        logOutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, Auth::class.java)
            startActivity(intent)
        }



    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            bitmap = imageBitmap
            ivDisplayImage.setImageBitmap(bitmap)
        }
    }

}