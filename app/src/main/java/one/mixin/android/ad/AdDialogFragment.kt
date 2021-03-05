package one.mixin.android.ad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import one.mixin.android.R
import one.mixin.android.databinding.DialogAdHomeBinding

/**
 * Created by cc on 2021/3/4.
 */
class AdDialogFragment : DialogFragment() {
    private var binding: DialogAdHomeBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.apply {
            setWindowAnimations(R.style.BottomSheet_Animation)
        }
        binding = DialogAdHomeBinding.inflate(LayoutInflater.from(context), null, false)
        return binding?.root.apply {
            dialog?.window?.decorView?.background = android.R.color.transparent.toDrawable()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.vAd?.findViewById<TextView>(R.id.tv_ignore)?.setOnClickListener {
            dismiss()
        }
    }

    fun bindData(data: AdBean) {
        binding?.vAd?.bindData(data)
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
        }
    }
}