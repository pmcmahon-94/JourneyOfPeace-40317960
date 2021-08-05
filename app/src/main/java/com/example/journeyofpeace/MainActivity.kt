package com.example.journeyofpeace

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.tasks.Task
import com.google.android.material.navigation.NavigationView
import com.example.journeyofpeace.ar.PlacesArFragment

private const val CAMERA_REQUEST_CODE = 101

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var toolbar: androidx.appcompat.widget.Toolbar
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var arFragment: PlacesArFragment
    private lateinit var mapFragment: SupportMapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //set up location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        //create toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.nav_view)
        val Toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,0,0
        )
        drawerLayout.addDrawerListener(Toggle)
        Toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

        setUpPermissions()
        fetchLocation()
    }

    private fun fetchLocation() {

        val task: Task<Location> = fusedLocationProviderClient.lastLocation

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }
        task.addOnSuccessListener {
            if (it != null){
                Toast.makeText(applicationContext, "${it.latitude} ${it.longitude}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setUpPermissions() {
        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "You need the camera permission to use this app", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        //add some event to the menu
        when(item.itemId){
            R.id.Home -> {
                Toast.makeText(baseContext, "home", Toast.LENGTH_SHORT).show()
            }
            R.id.about_us -> {
                Toast.makeText(baseContext, "about us", Toast.LENGTH_SHORT).show()
            }
        }


        return true
    }

    private fun setUpMaps() {
        mapFragment.getMapAsync { googleMap ->
            googleMap.isMyLocationEnabled = true
            // ...
        }
    }
}