package com.shamsudeen.shuttlex

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.collections.hashMapOf as hashMapOf1

class RiderMapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mMap: GoogleMap

    private var riderMarker: Marker? = null
    private var driverMarker: Marker? = null

    private lateinit var requestPickupButton: Button

    private lateinit var pickupStatusButton: Button


    var isRequestActive = false

    var locationManager: LocationManager? = null

    val db = Firebase.firestore

    private var registration: ListenerRegistration? = null

    var userId: String? = null
    var currentRequestId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rider_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestPickupButton = findViewById(R.id.request_pickup_button)

        pickupStatusButton = findViewById(R.id.request_status_button)

        requestPickupButton.setOnClickListener { requestPickup() }

        performPermissionsRequest()

        userId = intent.getStringExtra("user-id")

        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun requestPickup() {
        if(!isRequestActive) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                performPermissionsRequest()
                return
            }

            val lat = locationManager?.getLastKnownLocation(GPS_PROVIDER)?.latitude
            val lon = locationManager?.getLastKnownLocation(GPS_PROVIDER)?.longitude

            val loc = GeoPoint(lat!!, lon!!)

            val newDoc = hashMapOf1(
                "riderId" to userId,
                "userLocation" to loc,
                "driverId" to null,
                "driverLocation" to null
            )

            db.collection("requests")
                .add(newDoc)
                .addOnSuccessListener {
                    isRequestActive = true
                    requestPickupButton.text = "Cancel Request"

                    currentRequestId = it.id

                    checkForUpdates()

                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        applicationContext,
                        "Oops, and error occurred",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("Error Requesting Pickup", "Error ${e.message}")
                }


        } else {
            registration?.remove()
            currentRequestId?.let {
                db.collection("requests").document(it)
                    .delete()
                    .addOnSuccessListener {
                        Log.d("Delete-request", "DocumentSnapshot successfully deleted!")
                        currentRequestId = null
                        isRequestActive = false
                        requestPickupButton.text = "Request Pickup"

                        Toast.makeText(applicationContext, "Request Cancelled", Toast.LENGTH_SHORT)
                            .show()

                    }
                    .addOnFailureListener { e -> Log.w("Delete-request", "Error deleting document", e) }
            }

        }

    }

    private fun checkForUpdates() {
        val docRef = db.collection("requests").document(currentRequestId!!)

        registration = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Update-Check", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d("Update-Check", "Current data: ${snapshot.data}")

                if(snapshot.data?.get("driverId") != null && snapshot.data?.get("driverId") != "") {
                    if(snapshot.data?.get("driverLocation") != null) {
                        val loc = snapshot.data?.get("driverLocation") as GeoPoint
                        val lat = loc.latitude
                        val lon = loc.longitude

                        Log.d("Hopop", loc.latitude.toString())

                        val latLng = LatLng(lat, lon)

                        driverMarker?.remove()

                        driverMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Shuttle"))

                        pickupStatusButton.visibility = View.VISIBLE
                        requestPickupButton.visibility = View.INVISIBLE

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))

                    }
                }
            } else {
                Log.d("Update-Check", "Current data: null")
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            performPermissionsRequest()
            return
        }
            var location = locationManager?.getLastKnownLocation(GPS_PROVIDER)

        if (location != null) {
            location = location as Location
        }

//            locationManager.get

        if (location != null) {
            onLocationChanged(location)
        }

        locationManager!!.requestLocationUpdates(GPS_PROVIDER, 400, 1f, this)


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
        val yourLocation = LatLng(p0.latitude, p0.longitude)

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

        riderMarker?.remove()

        riderMarker = mMap.addMarker(MarkerOptions().position(yourLocation).title("You"))!!

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(yourLocation, 18f))
    }


}