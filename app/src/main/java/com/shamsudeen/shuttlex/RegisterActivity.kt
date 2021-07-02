package com.shamsudeen.shuttlex

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private var auth: FirebaseAuth = Firebase.auth

    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val email: EditText = findViewById(R.id.register_email_edit_text)
        val password: EditText = findViewById(R.id.register_password_edit_text)
        val switch: Switch = findViewById(R.id.user_type_switch)
        val button: Button = findViewById(R.id.register_button)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)

        val switchToLogin: TextView = findViewById(R.id.switch_to_login)

        switchToLogin.setOnClickListener {
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }

        performPermissionsRequest()

        button.setOnClickListener {

            progressBar.visibility = View.VISIBLE
            button.visibility = View.INVISIBLE
//            progressBar
            auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("TAG", "createUserWithEmail:success")
                        val user = auth.currentUser

                        var userType = ""

                        userType = if (switch.isChecked) {
                            "driver"
                        } else {
                            "rider"
                        }

                        val profileUpdates = userProfileChangeRequest {
                            displayName = userType
                        }

                        user!!.updateProfile(profileUpdates)
                            .addOnCompleteListener { _ ->
                                if (task.isSuccessful) {
                                    val newDoc = hashMapOf(
                                        "email" to user.email,
                                        "userType" to user.displayName
                                    )

                                    db.collection("users").document(user.uid)
                                        .set(newDoc)
                                        .addOnSuccessListener {
                                            redirect(user)
                                            finish()
                                        }

                                        .addOnFailureListener { e ->
                                            Log.d("TAG", "Error writing document", e)
                                            progressBar.visibility = View.INVISIBLE
                                            button.visibility = View.VISIBLE
                                        }
                                } else {
                                    progressBar.visibility = View.INVISIBLE
                                    button.visibility = View.VISIBLE
                                }
                            }


                    } else {
                        // If sign in fails, display a message to the user.
                        Log.d("TAG", "createUserWithEmail:failure", task.exception)
                        progressBar.visibility = View.INVISIBLE
                        button.visibility = View.VISIBLE
                        Toast.makeText(
                            baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

    }

    private fun performPermissionsRequest() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ),
                0
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION

                    ),
                    0
                )
            }
        }
    }

    private fun redirect(user: FirebaseUser?) {
        if (user?.displayName == "rider") {
            val intent = Intent(applicationContext, RiderMapsActivity::class.java)
            intent.putExtra("user-id", user.uid)
            startActivity(intent)
        } else if (user?.displayName == "driver") {
            val intent = Intent(applicationContext, DriverListActivity::class.java)
            intent.putExtra("user-id", user.uid)
            startActivity(intent)
        }
    }
}