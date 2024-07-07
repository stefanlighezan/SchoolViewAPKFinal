package com.sdlig.schoolview

import ApiService
import Course
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
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
    private lateinit var apiService: ApiService

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
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Firebase authentication successful
                            val firebaseUser = auth.currentUser

                            val retrofit: Retrofit = Retrofit.Builder()
                                .baseUrl(getBaseUrl(accessToken))
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()

                            // Create ApiService instance
                            val apiService: ApiService = retrofit.create(ApiService::class.java)

                            // Variable to hold all courses
                            var allCourses: MutableList<Course> = ArrayList()

                            // Fetch courses recursively to handle pagination
                            fetchCourses(apiService, 1, getAccessToken(accessToken), allCourses)

                            privateAccessToken = accessToken
                            firestoreUpdate(allCourses)




                        } else {
                            // Firebase authentication failed
                            Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun getAccessToken(str: String): String{
        val start = str.indexOf("access_token=")
        if(start != -1) {
            return str.substring(start + "access_token=".length)
        }
        else return " "
    }

    private fun firestoreUpdate(allCourses: MutableList<Course>) {
        var hashmapList: ArrayList<HashMap<String, Any>> = arrayListOf()

        for(course in allCourses) {
            if(!course.isNull() && !course.isOutdated()) {
                hashmapList.add(hashMapOf<String, Any>(
                    "name" to course.name,
                    "id" to course.id,
                    "created_at" to course.created_at,
                    "notes" to arrayListOf<Any>()
                ))
            }
        }

        Firebase.firestore
            .collection("users")
            .add(hashMapOf<String, Any>(
                "uid" to auth.currentUser!!.uid,
                "accessToken" to privateAccessToken,
                "drafts" to arrayListOf<Any>(),
                "courses" to hashmapList
            ))
            .addOnSuccessListener {
                Toast.makeText(this, "Account Successfully Created", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, Auth::class.java)
                startActivity(intent)
            }
    }

    // Function to fetch courses recursively until all pages are retrieved
    private fun fetchCourses(apiService: ApiService, page: Int, accessToken: String, allCourses: MutableList<Course>) {
        apiService.getCourses(accessToken, "student", page).enqueue(object : Callback<List<Course>> {
            override fun onResponse(call: Call<List<Course>>, response: Response<List<Course>>) {
                if (response.isSuccessful) {
                    val courses: List<Course>? = response.body()

                    // Add courses to the list
                    courses?.let {
                        allCourses.addAll(it)
                    }

                    // Check if there are more pages to fetch
                    val nextPage = page + 1
                    if (courses != null && courses.isNotEmpty()) {
                        // Fetch next page recursively
                        fetchCourses(apiService, nextPage, accessToken, allCourses)
                    } else {
                        // All courses fetched, do something with allCourses list
                        // For example, save to Firestore or display in UI
                        // Note: Ensure you handle this according to your app's logic
                        // For now, just print the number of courses fetched
                        Toast.makeText(baseContext, "Fetched ${allCourses.size} courses", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Handle API error
                    Toast.makeText(baseContext, "Failed to fetch courses", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Course>>, t: Throwable) {
                // Handle network error
                Toast.makeText(baseContext, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun getBaseUrl(url: String): String {
        val startIndex = url.indexOf("https://")
        val endIndex = url.indexOf("/api/v1/courses")

        if (startIndex == -1 || endIndex == -1) {
            return " "
        }

        return url.substring(startIndex, endIndex + "/api/v1/courses".length) + "/"
    }
}
