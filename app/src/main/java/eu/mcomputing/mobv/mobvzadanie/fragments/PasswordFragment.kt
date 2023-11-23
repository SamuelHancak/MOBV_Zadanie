package eu.mcomputing.mobv.mobvzadanie.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import eu.mcomputing.mobv.mobvzadanie.R
import eu.mcomputing.mobv.mobvzadanie.data.DataRepository
import eu.mcomputing.mobv.mobvzadanie.databinding.FragmentPasswordBinding
import eu.mcomputing.mobv.mobvzadanie.viewmodels.PasswordViewModel

class PasswordFragment : Fragment(R.layout.fragment_password) {
    private lateinit var modelViewModel: PasswordViewModel
    private lateinit var binding: FragmentPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        modelViewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PasswordViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[PasswordViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            model = modelViewModel
        }.also { bnd ->
            modelViewModel.changePasswordResult.observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    Snackbar.make(
                        bnd.changePasswordButton,
                        it,
                        Snackbar.LENGTH_SHORT
                    ).show()

                    if (it == "Success! Password was changed.") {
                        requireView().findNavController().navigate(R.id.action_to_profile)
                        bnd.editTextPassword.text.clear()
                        bnd.editTextNewPassword.text.clear()
                    }
                    
                    modelViewModel.clearResult()
                }
            }

        }
    }
}