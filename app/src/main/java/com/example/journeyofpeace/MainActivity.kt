package com.example.journeyofpeace

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.drawerlayout.widget.DrawerLayout
import com.example.journeyofpeace.api.NearbyPlacesResponse
import com.example.journeyofpeace.api.PlacesService
import com.example.journeyofpeace.ar.PlaceNode
import com.example.journeyofpeace.model.Place
import com.example.journeyofpeace.model.getPositionVector
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

//private const val CAMERA_REQUEST_CODE = 101
private const val LOCATION_REQUEST_CODE = 100

/**
 * The main activity for the Journey of Peace app being developed for Failte Feirste Thiar.
 * The app is an Augmented Reality app using geo-location and is aimed at tourism and education.
 * The class involves generating permissions for both Camera and Location in order for the app to be used,
 * a navigation menu for easy navigation of the application, A map with markers pin pointing the locations
 * in a tour format, geo-location for users to gain access to their location and whereabouts and an AR feature
 * developed for each location.
 */
class MainActivity : AppCompatActivity(), SensorEventListener, NavigationView.OnNavigationItemSelectedListener,
    OnMapReadyCallback {

    private val TAG = "MainActivity"

    private lateinit var videoRenderable: ModelRenderable
    private val HEIGHT: Float = 0.95f

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var arFragment: ArFragment
    private lateinit var mapFragment: SupportMapFragment

    private val orientationAngles = FloatArray(3)

    private lateinit var map: GoogleMap
    private var currentLocation: Location? = null
    private var places: List<Place>? = null
    private var anchorNode: AnchorNode? = null
    private var markers: MutableList<Marker> = emptyList<Marker>().toMutableList()
    private lateinit var placesService: PlacesService

    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)

    private val bSands = LatLng(54.5902, -5.9678)
    private val connCentre = LatLng(54.5979, -5.9528)
    private val clonMonastery = LatLng(54.6000, -5.9571)
    private val millCemetary = LatLng(54.5828, -5.9738)
    private val muralWall = LatLng(54.6010, -5.9564)
    private val radioFailte = LatLng(54.5998, -5.9402)

    private lateinit var markerSands: Marker
    private lateinit var markerCentre: Marker
    private lateinit var markerMonastery: Marker
    private lateinit var markerCemetary: Marker
    private lateinit var markerWall: Marker
    private lateinit var markerRadio: Marker

    /**
     * The onCreate method allows for the fragments to be created and by using the variables
     * initialized above to be linked to an ID from a particular layout within the xml files.
     * This is where the main code to run the app will be performed.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment

        val texture = ExternalTexture()
        val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.easter_rising)
        mediaPlayer.setSurface(texture.surface)
        mediaPlayer.isLooping = true

        ModelRenderable.builder()
            .setSource(this, R.raw.video_screen)
            .build()
            .thenAccept{
                    modelRenderable ->
                videoRenderable = modelRenderable
                videoRenderable.material.setFloat4("keyColor", Color(0.01843f, 1.0f, 0.0987f))
                videoRenderable.material.setExternalTexture("videoTexture", texture)
            }

        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            val anchorNode = AnchorNode(hitResult.createAnchor())

            if (!mediaPlayer.isPlaying){
                mediaPlayer.start()
                texture.surfaceTexture.setOnFrameAvailableListener {
                    anchorNode.renderable = videoRenderable
                    texture.surfaceTexture.setOnFrameAvailableListener(null)
                }
            }else{
                anchorNode.renderable = videoRenderable
            }
            val width: Float = mediaPlayer.videoWidth.toFloat()
            val height: Float = mediaPlayer.videoHeight.toFloat()

            anchorNode.localScale = Vector3(HEIGHT * (width / height), HEIGHT, 0.95f)
            arFragment.arSceneView.scene.addChild(anchorNode)
        }

        mapFragment =
            supportFragmentManager.findFragmentById(R.id.maps_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //set up location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        //create toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.nav_view)
        val Toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, 0, 0
        )
        drawerLayout.addDrawerListener(Toggle)
        Toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

        sensorManager = getSystemService()!!
        placesService = PlacesService.create()

        //setUpPermissions()
        //setUpAr()
        setUpMaps()
    }

    /*private fun setUpPermissions() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        }
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        }
    }
    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE
        )
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "You need the camera permission to use this app",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "You need the location permission to use this app",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }*/

    /**
     * Takes an item from the navigation menu and adds a notification to the item
     * only when said item is equal to the ID requested.
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        //add some event to the menu
        when (item.itemId) {
            R.id.Home -> {
                Toast.makeText(baseContext, "home", Toast.LENGTH_SHORT).show()
            }
            R.id.about_us -> {
                Toast.makeText(baseContext, "about us", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AboutUs::class.java)
                startActivity(intent)
            }
        }
        return true
    }

    /**
     *
     */
    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    /**
     *
     */
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
    /*
    /**
     * Method initiates the AR Fragment within the app.
     * It allows for an anchor to be created in order to position the fragment
     * appropriately within the scene.
     */
    private fun setUpAr() {
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            // Create anchor
            val anchor = hitResult.createAnchor()
            anchorNode = AnchorNode(anchor)
            anchorNode?.setParent(arFragment.arSceneView.scene)
            addPlaces(anchorNode!!)
        }
    }*/

    /**
     * setUpMaps() allows
     */
    private fun setUpMaps() {
        mapFragment.getMapAsync { googleMap ->
            /*if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_REQUEST_CODE
                )
            }*/
            googleMap.isMyLocationEnabled = true

            getCurrentLocation {
                val pos = CameraPosition.fromLatLngZoom(it.latLng, 13f)
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
                getNearbyPlaces(it)
            }
            googleMap.setOnMarkerClickListener { marker ->
                val tag = marker.tag
                if (tag !is Place) {
                    return@setOnMarkerClickListener false
                }
                showInfoWindow(tag)
                return@setOnMarkerClickListener true
            }
            map = googleMap
        }
    }

    /**
     * Location permissions are requested and if the permission is granted, a user
     * will then be able to access the location of their device within the map fragment
     * located below the AR fragment.
     */
    private fun getCurrentLocation(onSuccess: (Location) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            currentLocation = location
            onSuccess(location)
        }.addOnFailureListener {
            Log.e(TAG, "Could not get location")
        }
    }

    /**
     *
     */
    private fun addPlaces(anchorNode: AnchorNode) {
        val currentLocation = currentLocation
        if (currentLocation == null) {
            Log.w(TAG, "Location has not been determined yet")
            return
        }

        val places = places
        if (places == null) {
            Log.w(TAG, "No places to put")
            return
        }

        for (place in places) {
            // Add the place in AR
            val placeNode = PlaceNode(this, place)
            placeNode.setParent(anchorNode)
            placeNode.localPosition = place.getPositionVector(orientationAngles[0], currentLocation.latLng)
            placeNode.setOnTapListener { _, _ ->
                showInfoWindow(place)
            }

            // Add the place in maps
            map.let {
                val marker = it.addMarker(
                    MarkerOptions()
                        .position(place.geometry.location.latLng)
                        .title(place.name)
                )
                marker?.tag = place
                markers.add(marker!!)
            }
        }
    }

    /**
     *
     */
    private fun getNearbyPlaces(location: Location) {
        // TODO fill in API key
        val apiKey = this.getString(R.string.google_maps_key)
        placesService.nearbyPlaces(
            apiKey = apiKey,
            location = "${location.latitude},${location.longitude}",
            radiusInMeters = 2000,
            placeType = "park"
        ).enqueue(
            object : Callback<NearbyPlacesResponse> {
                override fun onFailure(call: Call<NearbyPlacesResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get nearby places", t)
                }

                override fun onResponse(
                    call: Call<NearbyPlacesResponse>,
                    response: Response<NearbyPlacesResponse>
                ) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to get nearby places")
                        return
                    }

                    val places = response.body()?.results ?: emptyList()
                    this@MainActivity.places = places
                }
            }
        )
    }

    /**
     *
     */
    private fun showInfoWindow(place: Place) {
        // Show in AR
        val matchingPlaceNode = anchorNode?.children?.filterIsInstance<PlaceNode>()?.first {
            val otherPlace = it.place ?: return@first false
            return@first otherPlace == place
        }
        matchingPlaceNode?.showInfoWindow()

        // Show as marker
        val matchingMarker = markers.firstOrNull {
            val placeTag = (it.tag as? Place) ?: return@firstOrNull false
            return@firstOrNull placeTag == place
        }
        matchingMarker?.showInfoWindow()
    }

    /**
     *
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    /**
     *
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
    }

    /**
     * When the map is ready for use, markers will appear for each of the locations
     * defined below. The locations are initialised using their Longitude and Latitude.
     * Markers are then set up for each location with a title appearing when user taps
     * on the marker within the map fragment.
     */
    override fun onMapReady(map: GoogleMap) {
        // Add some markers to the map, and add a data object to each marker.
        markerRadio = map.addMarker(
            MarkerOptions()
                .position(radioFailte)
                .title("Radio Failte")
        )
        markerRadio.tag = 0
        markerWall = map.addMarker(
            MarkerOptions()
                .position(muralWall)
                .title("Solidarity Wall")
        )
        markerWall.tag = 0
        markerSands = map.addMarker(
            MarkerOptions()
                .position(bSands)
                .title("Bobby Sands Mural")
        )
        markerSands.tag = 0
        markerMonastery = map.addMarker(
            MarkerOptions()
                .position(clonMonastery)
                .title("Clonard Monastery")
        )
        markerMonastery.tag = 0
        markerCentre = map.addMarker(
            MarkerOptions()
                .position(connCentre)
                .title("Aras Ui Chongaile")
        )
        markerCentre.tag = 0
        markerCemetary = map.addMarker(
            MarkerOptions()
                .position(millCemetary)
                .title("Milltown Cemetery")
        )
        markerCemetary.tag = 0

        // Set a listener for marker click.
        // map.setOnMarkerClickListener(this)
    }

    /*override fun onMarkerClick(p0: Marker): Boolean {
        TODO("Not yet implemented")
    }*/
}

val Location.latLng: LatLng
    get() = LatLng(this.latitude, this.longitude)