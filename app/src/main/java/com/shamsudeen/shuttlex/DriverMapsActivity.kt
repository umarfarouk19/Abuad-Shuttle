package com.shamsudeen.shuttlex

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class DriverMapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mMap: GoogleMap

    private var riderMarker: Marker? = null
    private var driverMarker: Marker? = null

    var requestId: String? = null

    val db = Firebase.firestore

    private var auth: FirebaseAuth = Firebase.auth


    var locationManager: LocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        performPermissionsRequest()

        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val acceptRequest: Button = findViewById(R.id.accept_request)

        acceptRequest.setOnClickListener {
            acceptTheRequest()
        }

        requestId = intent.getStringExtra("request-id")
    }

    private fun acceptTheRequest() {
        Toast.makeText(applicationContext, "Request Accepted", Toast.LENGTH_SHORT).show()


        val requestRef = requestId?.let { db.collection("requests").document(it) }

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
        val loc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        val geoPoint = GeoPoint(loc?.latitude!!, loc.longitude)
        val curr = auth.currentUser
        requestRef?.update(
            "driverId", curr!!.uid,
            "driverLocation", geoPoint
        )?.addOnSuccessListener {
            requestRef.get().addOnSuccessListener { document ->

                val riderGeoPoint = document.data!!["userLocation"] as GeoPoint

                val directionsIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "http://maps.google.com/maps?saddr=" +
                                geoPoint.latitude + "," +
                                geoPoint.longitude + "&daddr=" +
                                riderGeoPoint.latitude + "," +
                                riderGeoPoint.longitude
                    )
                )

                startActivity(directionsIntent)
            }

        }?.addOnFailureListener {
            Toast.makeText(applicationContext, "Oops an error occurred when accepting the request", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        requestId?.let {
            db.collection("requests").document(it)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val riderGeoPoint: GeoPoint = document["userLocation"] as GeoPoint
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

                        val yourLocation: Location? =
                            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                        locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1f, this)

                        riderMarker?.remove()
                        driverMarker?.remove()

                        riderMarker = mMap.addMarker(
                            MarkerOptions().position(
                                LatLng(
                                    riderGeoPoint.latitude,
                                    riderGeoPoint.longitude
                                )
                            ).title("Rider")
                        )!!

                        driverMarker = mMap.addMarker(
                            MarkerOptions().position(
                                LatLng(
                                    yourLocation?.latitude!!,
                                    yourLocation.longitude
                                )
                            ).title("You")
                        )!!

                        val builder = LatLngBounds.Builder()

                        builder.include(riderMarker!!.position)
                        builder.include(driverMarker!!.position)

                        val bounds = builder.build()

                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 2))

                    }
                }
                .addOnFailureListener { _ ->
                    Toast.makeText(applicationContext, "Oops an error occurred", Toast.LENGTH_SHORT)
                        .show()
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
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                0
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION

                    ),
                    0
                )
            }

        }

    }

    override fun onLocationChanged(p0: Location) {
        val requestRef = requestId?.let { db.collection("requests").document(it) }

        val loc = GeoPoint(p0.latitude, p0.longitude)

        requestRef
            ?.update("driverLocation", loc)
            ?.addOnSuccessListener {
                Log.d(
                    "User-Location-Update",
                    "DocumentSnapshot successfully updated! $loc"
                )
            }
            ?.addOnFailureListener { e -> Log.w("Update", "Error updating document", e) }
    }

}