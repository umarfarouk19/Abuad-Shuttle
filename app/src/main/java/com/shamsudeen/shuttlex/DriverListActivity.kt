package com.shamsudeen.shuttlex

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DriverListActivity : AppCompatActivity(), LocationListener {

    private var auth: FirebaseAuth = Firebase.auth

    val db = Firebase.firestore

    var locationManager: LocationManager? = null

    val requests = ArrayList<Request>()



    var userId: String? = null

    lateinit var recyclerView: RecyclerView
    lateinit var recyclerAdapter: RequestRecyclerAdapter
    lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_list)

        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager


        userId = intent.getStringExtra("user-id")

        recyclerView = findViewById(R.id.rv)

        val refreshLayout: SwipeRefreshLayout = findViewById(R.id.swipe)

        refreshLayout.setOnRefreshListener {
            initialiseRecyclerView()
            refreshLayout.isRefreshing = false
        }


        performPermissionsRequest()

        initialiseRecyclerView()
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val theta = lon1 - lon2
        var dist = (Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + (Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta))))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515
        return dist
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }

    private fun initialiseRecyclerView() {
        requests.clear()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            performPermissionsRequest()
        }

        var yourLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if(yourLocation != null) {
            yourLocation = yourLocation as Location
        }

        val userDocRef = userId?.let { db.collection("users").document(it) }

        val loc = yourLocation?.latitude?.let { GeoPoint(it, yourLocation.longitude) }
        userDocRef
            ?.update("location", loc)
            ?.addOnSuccessListener {
                Log.d(
                    "User-Location-Update",
                    "DocumentSnapshot successfully updated! $loc"
                )
            }
            ?.addOnFailureListener { e -> Log.w("Update", "Error updating document", e) }

        db.collection("requests")
            .whereEqualTo("driverId", null)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {

                    val requestGeoPoint = document.data["userLocation"] as GeoPoint

                    val lat = yourLocation?.latitude
                    val lon = yourLocation?.longitude

                    val distance: Double = calculateDistance(requestGeoPoint.latitude, requestGeoPoint.longitude,
                        lat!!,
                        lon!!
                    ) * 1.61

                    requests.add(Request(distance, document.id))

                }

                requests.sortedWith(compareBy { it.requestId })

                recyclerAdapter =
                    RequestRecyclerAdapter(
                        applicationContext,
                        requests
                    )

                layoutManager = LinearLayoutManager(applicationContext)

                recyclerView.layoutManager = layoutManager

                recyclerView.adapter = recyclerAdapter


            }
            .addOnFailureListener { exception ->
                Log.w("TAG", "Error getting documents: ", exception)

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


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.logout -> {
                Firebase.auth.signOut()
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onLocationChanged(p0: Location) {
        val userDocRef = userId?.let { db.collection("users").document(it) }

        val loc = GeoPoint(p0.latitude, p0.longitude)
        userDocRef
            ?.update("location", loc)
            ?.addOnSuccessListener {
                Log.d(
                    "User-Location-Update",
                    "DocumentSnapshot successfully updated! $loc"
                )
            }
            ?.addOnFailureListener { e -> Log.w("Update", "Error updating document", e) }
    }
}