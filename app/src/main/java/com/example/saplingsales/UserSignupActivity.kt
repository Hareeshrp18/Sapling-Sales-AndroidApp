package com.example.saplingsales

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class UserSignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usersignup)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        progressDialog = ProgressDialog(this)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etMobile = findViewById<EditText>(R.id.etMobile)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        tvLogin.setOnClickListener {
            startActivity(Intent(this, UserLoginActivity::class.java))
            finish()
        }

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("361380567794-ko70ta04qmmj4bos3mbf7dq8b1fkriiv.apps.googleusercontent.com")  // Replace with correct Web Client ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, options)

        btnSignup.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val mobile = etMobile.text.toString().trim()
            val email = etEmail.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (validateAllFields(username, mobile, email, password, confirmPassword)) {
                registerUser(username, mobile, email, password)
            }
        }

        btnGoogleSignIn.setOnClickListener {
            showLoading("Signing in with Google...")
            googleSignIn()
        }
    }

    private fun googleSignIn() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            checkIfGoogleUserExists(account.idToken!!)
        } catch (e: ApiException) {
            hideLoading()
            Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkIfGoogleUserExists(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential).addOnCompleteListener(this) { authTask ->
            if (authTask.isSuccessful) {
                val user = auth.currentUser
                user?.let {
                    firestore.collection("users").document(user.uid).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                // 🚫 User exists → Show toast & sign out
                                hideLoading()
                                auth.signOut()
                                googleSignInClient.signOut()
                                Toast.makeText(this, "User already exists. Try logging in.", Toast.LENGTH_LONG).show()
                            } else {
                                // ✅ New user → Save to Firestore & Allow Login
                                saveUserToFirestore(user, user.displayName ?: "GoogleUser", "N/A")
                            }
                        }
                        .addOnFailureListener {
                            hideLoading()
                            Toast.makeText(this, "Error checking user: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                }
            } else {
                hideLoading()
                Toast.makeText(this, "Google Authentication Failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveUserToFirestore(user: FirebaseUser, username: String, mobile: String) {
        val userData = hashMapOf(
            "userId" to user.uid,
            "username" to username,
            "mobile" to mobile,
            "email" to (user.email ?: "")
        )

        firestore.collection("users").document(user.uid).set(userData)
            .addOnSuccessListener {
                hideLoading()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                hideLoading()
                Toast.makeText(this, "Firestore Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun registerUser(username: String, mobile: String, email: String, password: String) {
        showLoading("Checking user...")

        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result?.signInMethods
                if (!signInMethods.isNullOrEmpty()) {
                    hideLoading()
                    Toast.makeText(this, "User already exists. Try logging in.", Toast.LENGTH_LONG).show()
                } else {
                    checkFirestoreBeforeSignup(username, mobile, email, password)
                }
            } else {
                hideLoading()
                Toast.makeText(this, "Error checking user: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun createFirebaseUser(username: String, mobile: String, email: String, password: String) {
        showLoading("Creating account...")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val userData = hashMapOf(
                            "userId" to it.uid,
                            "username" to username,
                            "mobile" to mobile,
                            "email" to email
                        )

                        firestore.collection("users").document(it.uid).set(userData)
                            .addOnSuccessListener {
                                hideLoading()
                                Toast.makeText(this, "Signup Successful!", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                hideLoading()
                                Toast.makeText(this, "Firestore Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    hideLoading()
                    Toast.makeText(this, "Signup Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }


    private fun checkFirestoreBeforeSignup(username: String, mobile: String, email: String, password: String) {
        firestore.collection("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    hideLoading()
                    Toast.makeText(this, "User already exists. Try logging in.", Toast.LENGTH_LONG).show()
                } else {
                    createFirebaseUser(username, mobile, email, password)
                }
            }
            .addOnFailureListener {
                hideLoading()
                Toast.makeText(this, "Error checking user: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showLoading(message: String) {
        progressDialog.setMessage(message)
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        if (progressDialog.isShowing) progressDialog.dismiss()
    }

    private fun isValidUsername(username: String): Boolean {
        return when {
            username.isEmpty() -> {
                findViewById<EditText>(R.id.etUsername).error = "Username cannot be empty"
                false
            }
            username.length < 5 -> {
                findViewById<EditText>(R.id.etUsername).error = "Username must be at least 5 characters long"
                false
            }
            username.length > 30 -> {
                findViewById<EditText>(R.id.etUsername).error = "Username cannot exceed 30 characters"
                false
            }
            !username[0].isLetter() -> {
                findViewById<EditText>(R.id.etUsername).error = "Username must start with a letter"
                false
            }
            !username.matches(Regex("^[A-Za-z][A-Za-z0-9_]*$")) -> {
                findViewById<EditText>(R.id.etUsername).error = "Username can only contain letters, numbers, and underscores"
                false
            }
            else -> true
        }
    }

    private fun isValidMobile(mobile: String): Boolean {
        return when {
            mobile.isEmpty() -> {
                findViewById<EditText>(R.id.etMobile).error = "Mobile number cannot be empty"
                false
            }
            mobile.length != 10 -> {
                findViewById<EditText>(R.id.etMobile).error = "Mobile number must be 10 digits"
                false
            }
            !mobile.matches(Regex("^[6-9][0-9]{9}$")) -> {
                findViewById<EditText>(R.id.etMobile).error = "Invalid mobile number format. Must start with 6-9"
                false
            }
            else -> true
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                findViewById<EditText>(R.id.etEmail).error = "Email cannot be empty"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                findViewById<EditText>(R.id.etEmail).error = "Invalid email format"
                false
            }
            email.length > 50 -> {
                findViewById<EditText>(R.id.etEmail).error = "Email is too long"
                false
            }
            else -> true
        }
    }

    private fun isValidPassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                findViewById<EditText>(R.id.etPassword).error = "Password cannot be empty"
                false
            }
            password.length < 8 -> {
                findViewById<EditText>(R.id.etPassword).error = "Password must be at least 8 characters long"
                false
            }
            password.length > 50 -> {
                findViewById<EditText>(R.id.etPassword).error = "Password is too long"
                false
            }
            !password.contains(Regex("[A-Z]")) -> {
                findViewById<EditText>(R.id.etPassword).error = "Password must contain at least one uppercase letter"
                false
            }
            !password.contains(Regex("[0-9]")) -> {
                findViewById<EditText>(R.id.etPassword).error = "Password must contain at least one number"
                false
            }
            !password.contains(Regex("[@#\$%^&+=!]")) -> {
                findViewById<EditText>(R.id.etPassword).error = "Password must contain at least one special character (@#\$%^&+=!)"
                false
            }
            password.contains(" ") -> {
                findViewById<EditText>(R.id.etPassword).error = "Password cannot contain spaces"
                false
            }
            else -> true
        }
    }

    private fun isValidConfirmPassword(password: String, confirmPassword: String): Boolean {
        return when {
            confirmPassword.isEmpty() -> {
                findViewById<EditText>(R.id.etConfirmPassword).error = "Confirm password cannot be empty"
                false
            }
            confirmPassword != password -> {
                findViewById<EditText>(R.id.etConfirmPassword).error = "Passwords do not match"
                false
            }
            else -> true
        }
    }

    private fun validateAllFields(username: String, mobile: String, email: String, password: String, confirmPassword: String): Boolean {
        val isUsernameValid = isValidUsername(username)
        val isMobileValid = isValidMobile(mobile)
        val isEmailValid = isValidEmail(email)
        val isPasswordValid = isValidPassword(password)
        val isConfirmPasswordValid = isValidConfirmPassword(password, confirmPassword)

        return isUsernameValid && isMobileValid && isEmailValid && isPasswordValid && isConfirmPasswordValid
    }
}
