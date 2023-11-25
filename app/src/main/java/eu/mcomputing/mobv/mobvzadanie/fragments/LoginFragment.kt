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
import eu.mcomputing.mobv.mobvzadanie.data.PreferenceData
import eu.mcomputing.mobv.mobvzadanie.databinding.FragmentLoginBinding
import eu.mcomputing.mobv.mobvzadanie.viewmodels.AuthViewModel

class LoginFragment : Fragment() {
    private lateinit var viewModel: AuthViewModel
    private lateinit var binding: FragmentLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[AuthViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            model = viewModel
        }.also { bnd ->
            viewModel.logoutUser()

            viewModel.loginResult.observe(viewLifecycleOwner) {
                if (it !== null && it.isNotEmpty()) {
                    Snackbar.make(
                        bnd.submitButton,
                        it,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }

            bnd.forgotPasswordText.setOnClickListener {
                viewModel.forgottenPassword()
                viewModel.forgotPasswordResult.observe(viewLifecycleOwner) {
                    if (it.second.isNotEmpty()) {
                        Snackbar.make(
                            bnd.forgotPasswordText,
                            it.second,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            bnd.submitButton.setOnClickListener {
                viewModel.loginUser()
                viewModel.userResult.observe(viewLifecycleOwner) {
                    it?.let { user ->
                        PreferenceData.getInstance().putUser(requireContext(), user)
                        if (PreferenceData.getInstance().getSharing(requireContext())) {
                            requireView().findNavController().navigate(R.id.action_to_map)
                        } else {
                            requireView().findNavController().navigate(R.id.action_to_profile)
                        }
                    } ?: PreferenceData.getInstance().putUser(requireContext(), null)
                    if (it !== null) {
                        viewModel.clearInputs()
                    }
                }
            }

            bnd.backBtn.apply {
                setOnClickListener {
                    it.findNavController().navigate(R.id.action_to_intro)
                }
            }

        }
    }
}