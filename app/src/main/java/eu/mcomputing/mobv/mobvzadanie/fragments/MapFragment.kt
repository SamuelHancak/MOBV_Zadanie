package eu.mcomputing.mobv.mobvzadanie.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import eu.mcomputing.mobv.mobvzadanie.R
import eu.mcomputing.mobv.mobvzadanie.data.DataRepository
import eu.mcomputing.mobv.mobvzadanie.data.db.entities.UserEntity
import eu.mcomputing.mobv.mobvzadanie.databinding.FragmentMapBinding
import eu.mcomputing.mobv.mobvzadanie.widgets.bottomBar.BottomBar
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin


class MapFragment : Fragment() {
    private lateinit var binding: FragmentMapBinding
    private var selectedPoint: CircleAnnotation? = null
    private var lastLocation: Point? = null
    private lateinit var annotationManager: CircleAnnotationManager
    private lateinit var annotationImgManager: PointAnnotationManager
    private var users: List<UserEntity>? = null

    private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                initLocationComponent()
                addLocationListeners()
            }
        }

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
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

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
        }.also { bnd ->
            bnd.bottomBar.setActive(BottomBar.MAP)
            DataRepository.getInstance(requireContext()).getUsers().observe(viewLifecycleOwner) {
                users = it
            }

            annotationManager = bnd.mapView.annotations.createCircleAnnotationManager()
            annotationImgManager = bnd.mapView.annotations.createPointAnnotationManager()

            val hasPermission = hasPermissions(requireContext())
            onMapReady(hasPermission)

            bnd.myLocation.setOnClickListener {
                if (!hasPermissions(requireContext())) {
                    requestPermissionLauncher.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } else {
                    lastLocation?.let { refreshLocation(it) }
                    addLocationListeners()
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
                .center(Point.fromLngLat(14.3539484, 49.8001304))
                .zoom(2.0)
                .build()
        )
        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            if (enabled) {
                initLocationComponent()
                addLocationListeners()
            }
        }

        binding.mapView.getMapboxMap().addOnMapClickListener {
            if (hasPermissions(requireContext())) {
                onCameraTrackingDismissed()
            }
            true
        }
    }


    private fun initLocationComponent() {
        val locationComponentPlugin = binding.mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.baseline_account_box_24,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.baseline_account_box_24,
                ),
            )

        }
    }

    private fun addLocationListeners() {
        binding.mapView.location.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        binding.mapView.gestures.addOnMoveListener(onMoveListener)
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        refreshLocation(it)
    }

    private fun refreshLocation(point: Point) {
        binding.mapView.getMapboxMap()
            .setCamera(CameraOptions.Builder().center(point).zoom(14.0).build())
        binding.mapView.gestures.focalPoint =
            binding.mapView.getMapboxMap().pixelForCoordinate(point)
        lastLocation = point
        addMarker(point)
    }

    private fun generateRandomPoint(centerPoint: Point): Point {
        val random = Random()
        val angle = random.nextDouble() * 2 * Math.PI
        val distance = random.nextDouble() * 0.003

        val deltaX = distance * cos(angle)
        val deltaY = distance * sin(angle)

        val point =
            Point.fromLngLat(centerPoint.longitude() + deltaX, centerPoint.latitude() + deltaY, 0.0)
        Log.d("point - generated", point.toString())
        Log.d("point - center", centerPoint.toString())

        return point
    }

    private fun addMarker(point: Point) {
        val circleRadius = 100.0

        if (selectedPoint == null) {
            annotationManager.deleteAll()
            val pointAnnotationOptions = CircleAnnotationOptions()
                .withPoint(point)
                .withCircleRadius(circleRadius)
                .withCircleOpacity(0.2)
                .withCircleColor("#000")
                .withCircleStrokeWidth(2.0)
                .withCircleStrokeColor("#ffffff")

            selectedPoint = annotationManager.create(pointAnnotationOptions)

            if (users != null) {
                val randomUserAnnotations = mutableListOf<PointAnnotationOptions>()
                for (user in users!!) {
                    val randomPoint = generateRandomPoint(point)
                    val imgAnnotationOptions2 = PointAnnotationOptions()
                        .withPoint(randomPoint)
                        .withIconSize(1.5)
                    bitmapFromDrawableRes(
                        requireContext(),
                        R.drawable.baseline_account_box_24
                    )?.let {
                        imgAnnotationOptions2.withIconImage(it)
                    }
                    randomUserAnnotations.add(imgAnnotationOptions2)
                }
                annotationImgManager.create(randomUserAnnotations)
            }
        } else {
            selectedPoint?.let {
                it.point = point
                annotationManager.update(it)
            }
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


    private fun onCameraTrackingDismissed() {
        binding.mapView.apply {
            location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            gestures.removeOnMoveListener(onMoveListener)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.apply {
            location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            gestures.removeOnMoveListener(onMoveListener)
        }
    }

}