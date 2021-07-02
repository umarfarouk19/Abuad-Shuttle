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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {
    private var auth: FirebaseAuth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val email: EditText = findViewById(R.id.login_email_edit_text)
        val password: EditText = findViewById(R.id.login_password_edit_text)
        val button: Button = findViewById(R.id.login_button)

        val progressBar: ProgressBar = findViewById(R.id.progressBar2)

        val switchToRegister: TextView = findViewById(R.id.switch_to_register)

        switchToRegister.setOnClickListener {
            startActivity(Intent(applicationContext, RegisterActivity::class.java))
        }

        performPermissionsRequest()

        button.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            button.visibility = View.INVISIBLE

            auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("Login-Status", "signInWithEmail:success")
                        val user = auth.currentUser

                        redirect(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        progressBar.visibility = View.INVISIBLE
                        button.visibility = View.VISIBLE

                        Log.w("Login-Status", "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
//                        updateUI(null)
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

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            redirect(currentUser)
        }
    }
}