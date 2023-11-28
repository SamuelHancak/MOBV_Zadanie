package eu.mcomputing.mobv.mobvzadanie.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.gson.JsonParser
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.squareup.picasso.Picasso
import eu.mcomputing.mobv.mobvzadanie.R
import eu.mcomputing.mobv.mobvzadanie.data.DataRepository
import eu.mcomputing.mobv.mobvzadanie.data.PreferenceData
import eu.mcomputing.mobv.mobvzadanie.data.db.entities.UserEntity
import eu.mcomputing.mobv.mobvzadanie.databinding.FragmentMapBinding
import eu.mcomputing.mobv.mobvzadanie.widgets.bottomBar.BottomBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin


class MapFragment : Fragment() {
    private lateinit var binding: FragmentMapBinding
    private var lastLocation: Point? = null
    private lateinit var annotationManager: CircleAnnotationManager
    private lateinit var annotationImgManager: PointAnnotationManager
    private var users: List<UserEntity>? = null
    private lateinit var mapView: MapView

    private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)


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
            addMarkers(it)
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!hasPermissions(requireContext())) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
        }.also { bnd ->
            bnd.bottomBar.setActive(BottomBar.MAP)
            mapView = binding.mapView
            DataRepository.getInstance(requireContext()).getUsers().observe(viewLifecycleOwner) {
                users = it
            }

            annotationManager = bnd.mapView.annotations.createCircleAnnotationManager()
            annotationImgManager = bnd.mapView.annotations.createPointAnnotationManager()

            annotationImgManager.addClickListener {
                PreferenceData.getInstance().putUserProfileId(
                    requireContext(),
                    it.getData()?.asJsonObject?.get("id").toString()
                )
                if (PreferenceData.getInstance()
                        .getUserProfileId(requireContext()) == PreferenceData.getInstance()
                        .getUser(requireContext())?.id
                ) {
                    requireView().findNavController().navigate(R.id.action_to_profile)
                } else {
                    requireView().findNavController().navigate(R.id.action_to_user)
                }
                true
            }

            val hasPermission = hasPermissions(requireContext())
            onMapReady(hasPermission)

            bnd.myLocation.setOnClickListener {
                if (!hasPermissions(requireContext())) {
                    requestPermissionLauncher.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } else {
                    mapView.getMapboxMap()
                        .setCamera(CameraOptions.Builder().center(lastLocation).zoom(14.0).build())
                }
            }
        }
    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun onMapReady(enabled: Boolean) {
        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(2.0)
                .build()
        )
        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            if (enabled) {
                initLocationComponent()
                setupGesturesListener()
            }
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

    private fun generateRandomPoint(centerPoint: Point): Point {
        val random = Random()
        val angle = random.nextDouble() * 2 * Math.PI
        val distance = random.nextDouble() * 0.003

        val deltaX = distance * cos(angle)
        val deltaY = distance * sin(angle)

        return Point.fromLngLat(
            centerPoint.longitude() + deltaX,
            centerPoint.latitude() + deltaY,
            0.0
        )
    }

    private fun addMarkers(point: Point) {
        lifecycleScope.launch {
            if (users != null) {
                val randomUserAnnotations = mutableListOf<PointAnnotationOptions>()
                for (user in users!!) {
                    val randomPoint: Point
                    if (user.uid == PreferenceData.getInstance()
                            .getUser(requireContext())?.id
                    ) {
                        randomPoint = point
                    } else {
                        randomPoint = generateRandomPoint(point)
                    }
                    val imgAnnotationOptions2 = PointAnnotationOptions()
                        .withPoint(randomPoint)
                        .withIconSize(1.5)
                        .withData(JsonParser.parseString("{\"id\": ${user.uid}}"))
                    if (user.photo.isEmpty()) {
                        bitmapFromDrawableRes(
                            requireContext(),
                            R.drawable.baseline_account_box_24
                        )?.let {
                            imgAnnotationOptions2.withIconImage(it)
                        }
                    } else {
                        val bitmap =
                            loadBitmapFromUrl(user.photo)
                        imgAnnotationOptions2.withIconImage(bitmap!!).withIconSize(0.2)
                    }
                    randomUserAnnotations.add(imgAnnotationOptions2)
                }
                annotationImgManager.create(randomUserAnnotations)
            }

        }
    }

    private suspend fun loadBitmapFromUrl(image: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val bitmap = Picasso.get().load("https://upload.mcomputing.eu/${image}").get()

            var file = context?.getDir("Images", Context.MODE_PRIVATE)
            if (!file?.exists()!!) {
                file.mkdir()
            }
            file = File(file, image.split("/").last())
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.flush()
            out.close()
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.e("MapFragment", e.toString())
            null
        }
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