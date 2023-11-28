package eu.mcomputing.mobv.mobvzadanie.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.squareup.picasso.Picasso
import eu.mcomputing.mobv.mobvzadanie.R
import eu.mcomputing.mobv.mobvzadanie.broadcastReceivers.GeofenceBroadcastReceiver
import eu.mcomputing.mobv.mobvzadanie.data.DataRepository
import eu.mcomputing.mobv.mobvzadanie.data.PreferenceData
import eu.mcomputing.mobv.mobvzadanie.databinding.FragmentProfileBinding
import eu.mcomputing.mobv.mobvzadanie.viewmodels.ProfileViewModel
import eu.mcomputing.mobv.mobvzadanie.widgets.bottomBar.BottomBar
import eu.mcomputing.mobv.mobvzadanie.workers.MyWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit


class ProfileFragment : Fragment() {
    private lateinit var viewModel: ProfileViewModel
    private lateinit var binding: FragmentProfileBinding
    private lateinit var annotationManager: CircleAnnotationManager
    private var lastLocation: Point? = null

    private val PERMISSIONS_REQUIRED = when {
        Build.VERSION.SDK_INT >= 33 -> { // android 13
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        }

        Build.VERSION.SDK_INT >= 29 -> { // android 10
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        else -> {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        if (lastLocation == null || lastLocation != it) {
            lastLocation = it
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).zoom(14.0).build())
            mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
            annotationManager.deleteAll()
            val pointAnnotationOptions = CircleAnnotationOptions()
                .withPoint(it)
                .withCircleRadius(100.0)
                .withCircleOpacity(0.2)
                .withCircleColor("#000")
                .withCircleStrokeWidth(2.0)
                .withCircleStrokeColor("#fff")
            annotationManager.create(pointAnnotationOptions)
        }
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[ProfileViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!hasPermissions(requireContext())) {
            for (p in PERMISSIONS_REQUIRED) {
                requestPermissionLauncher.launch(p)
            }
        }

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            model = viewModel
        }.also { bnd ->
            bnd.bottomBar.setActive(BottomBar.PROFILE)
            mapView = binding.mapView
            annotationManager = bnd.mapView.annotations.createCircleAnnotationManager()

            onMapReady()

            val user = PreferenceData.getInstance().getUser(requireContext())
            user?.let { userIt ->
                viewModel.loadUser(userIt.id)

                viewModel.userResult.observe(viewLifecycleOwner) {
                    Picasso.get().load("https://upload.mcomputing.eu/${it?.photo}")
                        .placeholder(R.drawable.baseline_account_box_24)
                        .error(R.drawable.baseline_account_box_24).into(bnd.profileImage)
                }
            }

            bnd.logoutBtn.setOnClickListener {
                PreferenceData.getInstance().clearData(requireContext())
                PreferenceData.getInstance().clearUser(requireContext())
                it.findNavController().navigate(R.id.action_to_intro)
            }

            bnd.changePasswordBtn.setOnClickListener {
                it.findNavController().navigate(R.id.action_to_password)
            }

            bnd.uploadImageBtn.setOnClickListener {
                it.findNavController().navigate(R.id.action_to_image)
            }

            bnd.locationSwitch.isChecked = PreferenceData.getInstance()
                .getSharing(requireContext()) || (PreferenceData.getInstance()
                .getTimeSharing(requireContext()) && isWithinTimeRange())
            bnd.timeLimitedSwitch.isChecked =
                PreferenceData.getInstance().getTimeSharing(requireContext())

            bnd.locationSwitch.isEnabled = !bnd.timeLimitedSwitch.isChecked

            if (bnd.locationSwitch.isChecked) {
                turnOnSharing()
            } else {
                turnOffSharing()
            }

            bnd.locationSwitch.setOnClickListener {
                if (bnd.locationSwitch.isChecked) {
                    turnOnSharing()
                } else {
                    turnOffSharing()
                    PreferenceData.getInstance().putTimeSharing(requireContext(), false)
                    bnd.timeLimitedSwitch.isChecked = false
                }
            }

            bnd.timeLimitedSwitch.setOnClickListener {
                if (bnd.timeLimitedSwitch.isChecked) {
                    bnd.locationSwitch.isEnabled = false
                    val isInTimeRange = isWithinTimeRange()

                    if (isInTimeRange) {
                        turnOnSharing()
                    }

                    bnd.locationSwitch.isChecked = isInTimeRange
                    PreferenceData.getInstance().putTimeSharing(requireContext(), true)
                    PreferenceData.getInstance().putSharing(requireContext(), isInTimeRange)
                } else {
                    bnd.locationSwitch.isEnabled = true
                    PreferenceData.getInstance().putTimeSharing(requireContext(), false)
                }
            }
        }
    }

    private fun onMapReady() {
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(14.0)
                .build()
        )
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            initLocationComponent()
            setupGesturesListener()
        }
    }

    private fun setupGesturesListener() {
        mapView.gestures.addOnMoveListener(onMoveListener)
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        locationComponentPlugin.addOnIndicatorBearingChangedListener(
            onIndicatorBearingChangedListener
        )
    }

    @SuppressLint("MissingPermission")
    fun turnOnSharing() {
        if (!hasPermissions(requireContext())) {
            binding.locationSwitch.isChecked = false
            for (p in PERMISSIONS_REQUIRED) {
                requestPermissionLauncher.launch(p)
            }
            return
        }
        PreferenceData.getInstance().putSharing(requireContext(), true)
        viewModel.sharingLocation.value = true

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) {
            if (it != null) {
                setupGeofence(it)
            }
        }
    }

    private fun turnOffSharing() {
        PreferenceData.getInstance().putSharing(requireContext(), false)
        removeGeofence()
    }

    @SuppressLint("MissingPermission")
    private fun setupGeofence(location: Location) {
        val geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        val geofence = Geofence.Builder()
            .setRequestId("my-geofence")
            .setCircularRegion(location.latitude, location.longitude, 100f)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        val geofencePendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(
                    requireActivity(),
                    0,
                    intent,
                    PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    requireActivity(),
                    0,
                    intent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            }


        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                viewModel.updateGeofence(location.latitude, location.longitude, 100.0)
                runWorker()
            }
            addOnFailureListener {
                it.printStackTrace()
                binding.locationSwitch.isChecked = false
                PreferenceData.getInstance().putSharing(requireContext(), false)
            }
        }

    }

    private fun removeGeofence() {
        val geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        geofencingClient.removeGeofences(listOf("my-geofence"))
        viewModel.removeGeofence()
        cancelWorker()
    }

    private fun runWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<MyWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag("myworker-tag")
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "myworker",
            ExistingPeriodicWorkPolicy.KEEP, // or REPLACE
            repeatingRequest
        )
    }

    private fun cancelWorker() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("myworker")
    }

    private fun isWithinTimeRange(): Boolean {
        val currentTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = dateFormat.format(currentTime)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTimeDate = sdf.parse(formattedTime)
        val startTimeDate = sdf.parse("10:00")
        val endTimeDate = sdf.parse("17:00")

        return currentTimeDate in startTimeDate..endTimeDate
    }

    private fun onCameraTrackingDismissed() {
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }
}