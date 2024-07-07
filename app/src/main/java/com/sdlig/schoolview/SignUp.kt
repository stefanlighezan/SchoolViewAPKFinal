package com.sdlig.schoolview

import ApiService
import Course
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SignUp : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var apiService: ApiService

    private lateinit var etEmailSignUp: EditText
    private lateinit var etPasswordSignup: EditText
    private lateinit var etAccessToken: EditText
    private lateinit var signupBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etEmailSignUp = findViewById(R.id.etEmailSignupForApp)
        etPasswordSignup = findViewById(R.id.etPasswordSignUpForAccount)
        etAccessToken = findViewById(R.id.etAccessTokenText)
        signupBtn = findViewById(R.id.btnSignUpApp)

        signupBtn.setOnClickListener {
            val email = etEmailSignUp.text.toString().trim()
            val password = etPasswordSignup.text.toString().trim()
            val accessToken = etAccessToken.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || accessToken.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            } else {
                // Initialize Retrofit and ApiService here

                    // Proceed with Firebase Authentication and API request
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val firebaseUser = auth.currentUser

                                val retrofit: Retrofit by lazy {
                                    Retrofit.Builder()
                                        .baseUrl(accessToken)
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build()
                                }

                                val apiService: ApiService by lazy {
                                    retrofit.create(ApiService::class.java)
                                }



                                // Fetch all courses using Retrofit and your ApiService
                            } else {
                                Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }

