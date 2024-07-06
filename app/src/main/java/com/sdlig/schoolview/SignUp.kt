package com.sdlig.schoolview

import ApiService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SignUp : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var apiService: ApiService

    private lateinit var etEmailSignUp: EditText
    private lateinit var etPasswordSignup: EditText
    private lateinit var etAccessToken: EditText
    private lateinit var signupBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize Views
        etEmailSignUp = findViewById(R.id.etEmailSignUp)
        etPasswordSignup = findViewById(R.id.etPasswordSignup)
        signupBtn = findViewById(R.id.signupBtn)
        etAccessToken = findViewById(R.id.etAccessToken)

        // Set click listener for sign up button
        signupBtn.setOnClickListener {
            val email = etEmailSignUp.text.toString().trim()
            val password = etPasswordSignup.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            } else {
                // Call Firebase to create user with email and password
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign up success, update UI accordingly
                            val user = auth.currentUser
                            Toast.makeText(this, "Sign up successful", Toast.LENGTH_SHORT).show()

                            // Example access token URL
                            val accessTokenUrl = etAccessToken.text.toString().trim()

                            // Extract base URL
                            val baseUrl = extractBaseUrl(accessTokenUrl)

                            // Initialize Retrofit with dynamic base URL
                            val retrofit = Retrofit.Builder()
                                .baseUrl(baseUrl)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()

                            apiService = retrofit.create(ApiService::class.java)

                            val accessToken = extractAccessToken(accessTokenUrl).toString()

                            // Fetch data using Retrofit

                            apiService.fetchDataFromUrl(accessToken, "student").enqueue(object :
                                Callback<List<Course>> {
                                override fun onResponse(call: Call<List<Course>>, response: Response<List<Course>>) {
                                    if (response.isSuccessful) {
                                        val data = response.body()
                                        data?.let {
                                            // Log data to console
                                            println("Data fetched successfully: $data")
                                        }
                                    } else {
                                        println("Failed to fetch data: ${response.code()}")
                                    }
                                }

                                override fun onFailure(call: Call<List<Course>>, t: Throwable) {
                                    println("Network error: ${t.message}")
                                }
                            })

                            // Optionally, you can navigate to another activity or do other tasks
                        } else {
                            // If sign up fails, display a message to the user.
                            Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    // Function to extract base URL from access token URL
    private fun extractBaseUrl(url: String): String {
        val index = url.indexOf("/api/")
        return if (index != -1) {
            url.substring(0, index) // Include '/api/' in the base URL
        } else {
            url // Fallback to original URL if '/api/' is not found
        }
    }

    private fun extractAccessToken(url: String): String? {
        val accessTokenKey = "access_token="
        val startIndex = url.indexOf(accessTokenKey)

        if (startIndex != -1) {
            // Move index to start of access token value
            val tokenStartIndex = startIndex + accessTokenKey.length

            // Find end of access token value
            var endIndex = url.indexOf('&', tokenStartIndex)
            if (endIndex == -1) {
                endIndex = url.length
            }

            // Extract access token
            return url.substring(tokenStartIndex, endIndex)
        }

        return " "
    }
}