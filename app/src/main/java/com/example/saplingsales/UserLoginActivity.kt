package com.example.saplingsales

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.saplingsales.activities.UserScreenActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class UserLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        progressDialog = ProgressDialog(this)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)
        val tvSignup = findViewById<TextView>(R.id.tvSignup)

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("361380567794-ko70ta04qmmj4bos3mbf7dq8b1fkriiv.apps.googleusercontent.com") // Replace with your actual Client ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, options)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and Password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        btnGoogleSignIn.setOnClickListener {
            showLoading("Signing in with Google...")
            googleSignIn()
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, UserSignupActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            navigateToMain()
        }
    }

    private fun loginUser(email: String, password: String) {
        showLoading("Logging in...")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                hideLoading()
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            hideLoading()
            Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { authTask ->
                if (authTask.isSuccessful) {
                    val user = auth.currentUser
                    checkUserExistsInFirestore(user)
                } else {
                    hideLoading()
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkUserExistsInFirestore(user: FirebaseUser?) {
        if (user == null) {
            hideLoading()
            Toast.makeText(this, "Something went wrong. Try again!", Toast.LENGTH_LONG).show()
            return
        }

        val userRef = firestore.collection("users").document(user.uid)

        userRef.get()
            .addOnSuccessListener { document ->
                hideLoading()
                if (document.exists()) {
                    navigateToMain()
                } else {
                    Toast.makeText(
                        this, "User does not exist. Please sign up first.", Toast.LENGTH_LONG
                    ).show()
                    auth.signOut() // Sign out if user is not found
                    googleSignInClient.signOut()
                }
            }
            .addOnFailureListener {
                hideLoading()
                Toast.makeText(this, "Error checking user: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, UserScreenActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showLoading(message: String) {
        progressDialog.setMessage(message)
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        if (progressDialog.isShowing) progressDialog.dismiss()
    }
}
