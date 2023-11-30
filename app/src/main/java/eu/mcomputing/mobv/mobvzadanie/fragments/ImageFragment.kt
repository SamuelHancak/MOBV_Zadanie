package eu.mcomputing.mobv.mobvzadanie.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import eu.mcomputing.mobv.mobvzadanie.R
import eu.mcomputing.mobv.mobvzadanie.data.DataRepository
import eu.mcomputing.mobv.mobvzadanie.databinding.FragmentImageBinding
import eu.mcomputing.mobv.mobvzadanie.viewmodels.ImageViewModel

class ImageFragment : Fragment(R.layout.fragment_image) {
    private lateinit var modelViewModel: ImageViewModel
    private lateinit var binding: FragmentImageBinding
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        modelViewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ImageViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[ImageViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            model = modelViewModel
        }.also { bnd ->
            modelViewModel.uploadImageResult.observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    Snackbar.make(
                        bnd.uploadButton,
                        it,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }

            bnd.chooseImageButton.setOnClickListener {
                openImageChooser()
            }

            bnd.uploadButton.setOnClickListener {
                modelViewModel.uploadImage(selectedImageUri, requireContext())
            }

            bnd.deleteImageButton.setOnClickListener {
                modelViewModel.deleteImage()
            }

            modelViewModel.clearResult()
        }
    }

    private fun openImageChooser() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI).also {
            it.type = "image/*"
            val mimeTypes = arrayOf("image/jpeg", "image/jpg")
            it.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(it, REQUEST_CODE_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK)
            when (requestCode) {
                REQUEST_CODE_IMAGE -> {
                    selectedImageUri = data?.data
                    binding.profileImageView.setImageURI(data?.data)
                }
            }
    }

    companion object {
        const val REQUEST_CODE_IMAGE = 101
    }
}