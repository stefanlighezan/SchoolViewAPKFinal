package com.sdlig.schoolview

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

class Auth : AppCompatActivity() {
    private lateinit var etEmail: AppCompatEditText
    private lateinit var etPassword: AppCompatEditText
    private lateinit var loginBtn: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        //setup views
        etEmail = findViewById(R.id.etEmailSignUp)
        etPassword = findViewById(R.id.etPasswordSignup)
        loginBtn = findViewById(R.id.signupBtn)

        findViewById<Button>(R.id.btnGoToSignUp).setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
        //

        auth = Firebase.auth
        var user: FirebaseUser? = auth.currentUser

        if(user != null) {
            //
            val intent = Intent(this, MainApp::class.java)
            startActivity(intent)
        }

        loginBtn.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    user = auth.currentUser
                    Toast.makeText(this, "Logged In Successfully!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainApp::class.java)
                    startActivity(intent)
                }
                .addOnFailureListener {
                    Log.i("FIREBASE AUTH ERROR", "${it.message}, ${it.cause}")
                }
        }
    }
}