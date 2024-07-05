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
import com.google.firebase.firestore.FirebaseFirestore
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
            if(bitmap == null) {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(takePictureIntent, 1)
            } else {
                //send to cloud storage and firestore
                val storageRef = Firebase.storage.reference
                val imagesRef = storageRef.child("images/${user?.uid}/${System.currentTimeMillis()}.jpg")

                val baos = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()

                val uploadTask = imagesRef.putBytes(data)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    Log.d("MainApp", "Image uploaded successfully: ${taskSnapshot.metadata?.path}")

                    // Get download URL
                    imagesRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()

                        // Update Firestore
                        val userDocRef = db.collection("users").document(user?.uid ?: "")
                        userDocRef.get().addOnSuccessListener { documentSnapshot ->
                            val draftsList = documentSnapshot.get("drafts") as? MutableList<String> ?: mutableListOf()
                            draftsList.add(imageUrl)

                            // Update 'drafts' field in Firestore
                            userDocRef.update("drafts", draftsList)
                                .addOnSuccessListener {
                                    Log.d("MainApp", "Drafts list updated successfully")
                                    // Clear bitmap and ImageView after update
                                    bitmap = null
                                    ivDisplayImage.setImageBitmap(null)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("MainApp", "Error updating drafts list", e)
                                }
                        }.addOnFailureListener { e ->
                            Log.e("MainApp", "Error fetching user document", e)
                            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
                        }
                    }.addOnFailureListener { e ->
                        Log.e("MainApp", "Error getting download URL", e)
                        Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
                    }
                }.addOnFailureListener { e ->
                    Log.e("MainApp", "Error uploading image", e)
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
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