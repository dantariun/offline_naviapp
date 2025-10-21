package com.dantariun.sat_demo_map.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.data.model.DialogScreen
import com.dantariun.sat_demo_map.ui.dialog.inner.CancelReasonFragment
import com.dantariun.sat_demo_map.ui.dialog.inner.CancelledRescueFragment
import com.dantariun.sat_demo_map.ui.dialog.inner.DangerDetectedFragment
import com.dantariun.sat_demo_map.ui.dialog.inner.RescueRequestFragment
import com.dantariun.sat_demo_map.ui.viewmodel.AppDialogViewModel

class AppCenterDialog : DialogFragment() {

    private val viewModel: AppDialogViewModel by viewModels()
    private var listener: AppDialogListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_app_host, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setListener(listener)

        viewModel.currentScreen.observe(viewLifecycleOwner) { screen ->
            screen?.let { changeScreen(it) }
        }

        val initialScreen = arguments?.getSerializable(ARG_SCREEN) as? DialogScreen
        if (initialScreen != null) {
            viewModel.setInitialScreen(initialScreen)
        }
    }

    private fun changeScreen(screen: DialogScreen) {
        val fragment: Fragment = when (screen) {
            DialogScreen.RESCUE_REQUEST -> RescueRequestFragment()
            DialogScreen.CANCEL_REASON -> CancelReasonFragment()
            DialogScreen.DANGER_DETECTED -> DangerDetectedFragment()
            DialogScreen.CANCELLED_RESCUE -> CancelledRescueFragment()
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.fcv_dialog_content, fragment)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return

        window.setBackgroundDrawable(resources.getColor(R.color.danger_dialog_bg).toDrawable())
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }

    fun setDialogListener(listener: AppDialogListener) {
        this.listener = listener
    }

    interface AppDialogListener {
        fun onDialogResult(result: AppDialogResult)
    }

    companion object {
        private const val ARG_SCREEN = "initial_screen"

        fun newInstance(initialScreen: DialogScreen): AppCenterDialog {
            return AppCenterDialog().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_SCREEN, initialScreen)
                }
            }
        }


    }
}