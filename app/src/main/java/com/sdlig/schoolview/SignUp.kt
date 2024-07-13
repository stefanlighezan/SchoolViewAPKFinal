package com.sdlig.schoolview

import ApiService
import Course
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.ArrayList

class SignUp : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etEmailSignUp: EditText
    private lateinit var etPasswordSignup: EditText
    private lateinit var etAccessToken: EditText
    private lateinit var signupBtn: Button
    private lateinit var privateAccessToken: String

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
                signUpUser(email, password, accessToken)
            }
        }
    }

    private fun signUpUser(email: String, password: String, accessToken: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser

                    val retrofit = createRetrofitInstance(accessToken)
                    privateAccessToken = accessToken
                    val apiService = retrofit.create(ApiService::class.java)

                    val allCourses = ArrayList<Course>()
                    fetchCourses(apiService, 1, accessToken, allCourses)
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error occurred"
                    Toast.makeText(this, "Failed to create account: $errorMessage", Toast.LENGTH_SHORT).show()
                    task.exception?.let { e ->
                        Log.e("SignUp", "Failed to create account", e)
                    }
                }
            }
    }

    private fun createRetrofitInstance(accessToken: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(getBaseUrl(accessToken))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun fetchCourses(apiService: ApiService, page: Int, accessToken: String, allCourses: MutableList<Course>) {
        apiService.getCourses(accessToken, "student", page).enqueue(object : Callback<List<Course>> {
            override fun onResponse(call: Call<List<Course>>, response: Response<List<Course>>) {
                if (response.isSuccessful) {
                    val courses: List<Course>? = response.body()

                    courses?.let {
                        allCourses.addAll(it)
                    }

                    val nextPage = page + 1
                    if (courses != null && courses.isNotEmpty()) {
                        fetchCourses(apiService, nextPage, accessToken, allCourses)
                    } else {
                        Toast.makeText(baseContext, "Fetched ${allCourses.size} courses", Toast.LENGTH_SHORT).show()
                        firestoreUpdate(allCourses as ArrayList<Course>)
                    }
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Failed to fetch courses"
                    Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()

                    Log.e("SignUp", "Failed to fetch courses: $errorMessage")
                }
            }

            override fun onFailure(call: Call<List<Course>>, t: Throwable) {
                val errorMessage = "Network error: ${t.message}"
                Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
                Log.e("SignUp", errorMessage, t)
            }
        })
    }

    private fun firestoreUpdate(allCourses: ArrayList<Course>) {
        val listOfHashmaps = arrayListOf<HashMap<String, Any>>()
        allCourses.forEach { course ->
            if (!course.isNull()) {
                listOfHashmaps.add(
                    hashMapOf(
                        "name" to course.name!!,
                        "notes" to arrayListOf<Any>(),
                        "created_at" to course.created_at!!,
                        "id" to course.id!!
                    )
                )
            }
        }

        val settings = hashMapOf(
            "viewOutdatedCourses" to hashMapOf(
                "name" to "View Outdated Courses",
                "checked" to false
            ),
            "deleteCourses" to hashMapOf(
                "name" to "Delete Courses",
                "checked" to false
            )
        )

        try {
            Firebase.firestore.collection("users")
                .add(
                    hashMapOf(
                        "uid" to auth.currentUser!!.uid,
                        "courses" to listOfHashmaps,
                        "drafts" to listOf<Any>(),
                        "accessToken" to privateAccessToken,
                        "settings" to settings
                    )
                ).addOnSuccessListener {
                    Toast.makeText(this@SignUp, "Successfully Created Account. Sign In Again!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignUp, Auth::class.java))
                }.addOnFailureListener { e ->
                    Toast.makeText(this@SignUp, "Error updating Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("SignUp", "Error updating Firestore", e)
                }
        } catch (e: Exception) {
            Toast.makeText(this@SignUp, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("SignUp", "Error creating Firestore document", e)
        }
    }

    private fun getBaseUrl(url: String): String {
        val startIndex = url.indexOf("https://")
        val endIndex = url.indexOf("/api/v1/courses")

        return if (startIndex == -1 || endIndex == -1) {
            " "
        } else {
            url.substring(startIndex, endIndex + "/api/v1/courses".length) + "/"
        }
    }
}
