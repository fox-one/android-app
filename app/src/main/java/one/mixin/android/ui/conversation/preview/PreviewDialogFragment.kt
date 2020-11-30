package one.mixin.android.ui.conversation.preview

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.uber.autodispose.android.lifecycle.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.R
import one.mixin.android.databinding.FragmentPreviewBinding
import one.mixin.android.databinding.FragmentPreviewVideoBinding
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.screenWidth
import one.mixin.android.extension.toast
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.widget.VideoTimelineView
import java.util.concurrent.TimeUnit

class PreviewDialogFragment : DialogFragment(), VideoTimelineView.VideoTimelineViewDelegate {

    companion object {
        const val IS_VIDEO: String = "IS_VIDEO"
        fun newInstance(isVideo: Boolean = false): PreviewDialogFragment {
            val previewDialogFragment = PreviewDialogFragment()
            previewDialogFragment.arguments = bundleOf(
                IS_VIDEO to isVideo
            )
            return previewDialogFragment
        }
    }

    private val isVideo by lazy {
        requireArguments().getBoolean(IS_VIDEO)
    }

    private var currentState = false

    private val mixinPlayer: MixinPlayer by lazy {
        MixinPlayer().apply {
            setOnVideoPlayerListener(videoListener)
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(0x00000000))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        }
        if (currentState) {
            mixinPlayer.start()
        }
    }

    override fun onPause() {
        super.onPause()
        currentState = mixinPlayer.isPlaying()
        mixinPlayer.pause()
    }

    fun release() {
        if (isVideo) {
            mixinPlayer.release()
        }
    }

    private var mediaDialogView: View? = null

    private var _videoBinding: FragmentPreviewVideoBinding? = null
    private val videoBinding get() = requireNotNull(_videoBinding)
    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = requireNotNull(_binding)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setWindowAnimations(R.style.BottomSheet_Animation)
        }
        dialog.setOnShowListener {
            if (isVideo) {
                val mimeType = getMimeType(uri!!)
                if (mimeType == null || !mimeType.startsWith("video", true)) {
                    context?.toast(R.string.error_format)
                    dismiss()
                }
                mixinPlayer.loadVideo(uri.toString())
                mixinPlayer.setVideoTextureView(videoBinding.dialogVideoTexture)
                videoBinding.time.setVideoPath(uri!!.getFilePath(requireContext()))
                Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(this).subscribe {
                        if (mixinPlayer.duration() != 0 && mixinPlayer.isPlaying()) {
                            videoBinding.time.progress = mixinPlayer.getCurrentPos().toFloat() / mixinPlayer.duration()
                        }
                    }
                videoBinding.dialogOk.setOnClickListener {
                    action!!(uri!!)
                    dismiss()
                }
            } else {
                binding.dialogSendIb.setOnClickListener { action!!(uri!!); dismiss() }
                binding.dialogIv.loadImage(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (isVideo) {
            _videoBinding = FragmentPreviewVideoBinding.inflate(LayoutInflater.from(context), null, false)
        } else {
            _binding = FragmentPreviewBinding.inflate(LayoutInflater.from(context), null, false)
        }
        mediaDialogView = if (isVideo) {
            videoBinding.root
        } else {
            binding.root
        }
        if (isVideo) {
            videoBinding.dialogPlay.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    mixinPlayer.pause()
                } else {
                    mixinPlayer.start()
                }
            }
            videoBinding.time.setDelegate(this)
            videoBinding.dialogCancel.setOnClickListener {
                dismiss()
            }
        } else {
            binding.dialogCloseIv.setOnClickListener {
                dismiss()
            }
        }
        return mediaDialogView
    }

    private var uri: Uri? = null
    private var action: ((Uri) -> Unit)? = null
    fun show(fragmentManager: FragmentManager, uri: Uri, action: (Uri) -> Unit) {
        try {
            super.showNow(
                fragmentManager,
                if (isVideo) {
                    "PreviewVideoDialogFragment"
                } else {
                    "PreviewDialogFragment"
                }
            )
        } catch (ignored: IllegalStateException) {
        }
        this.uri = uri
        this.action = action
    }

    private val videoListener = object : MixinPlayer.VideoPlayerListenerWrapper() {
        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            val ratio = width / height.toFloat()
            val lp = videoBinding.dialogVideoTexture.layoutParams
            val screenWidth = requireContext().screenWidth()
            val screenHeight = requireContext().screenHeight()
            if (screenWidth / ratio > screenHeight) {
                lp.height = screenHeight
                lp.width = (screenHeight * ratio).toInt()
            } else {
                lp.width = screenWidth
                lp.height = (screenWidth / ratio).toInt()
            }
            videoBinding.dialogVideoTexture.layoutParams = lp
        }
    }

    override fun didStopDragging() {
        if (currentState) {
            mixinPlayer.start()
        }
    }

    override fun didStartDragging() {
        currentState = mixinPlayer.isPlaying()
        mixinPlayer.pause()
    }

    override fun onPlayProgressChanged(progress: Float) {
        mixinPlayer.seekTo((progress * mixinPlayer.duration()).toInt())
    }
}
