package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3

import android.util.Log
import android.view.View
import android.view.ViewStub
import android.widget.ImageButton
import android.widget.ImageView
import com.bailout.stickk.R
import java.lang.ref.WeakReference

/** One lazy, XML-backed OpenGL preview per gestures widget. */
class CollectionGesturePreviewController {
    private val previews = mutableMapOf<Int, CollectionGestureCardGLSurfaceViewV3>()
    private var boundRoot = WeakReference<View>(null)

    fun bind(root: View, interactionEnabled: () -> Boolean, selectGesture: (View) -> Unit) {
        if (boundRoot.get() !== root) {
            previews.values.forEach { preview ->
                preview.stopAtInitialPose()
                preview.setOnFirstFrameReadyListener(null)
            }
            previews.clear()
            boundRoot = WeakReference(root)
        }
        for (slot in 0..13) {
            val gestureId = if (slot <= 10) slot + 1 else slot + 2
            val card = root.findViewById<View>(cardId(slot)) ?: continue
            val image = root.findViewById<ImageView>(imageId(slot)) ?: continue
            val play = root.findViewById<ImageButton>(playId(slot)) ?: continue
            // Collection cards are GL-only. The legacy bitmap must never be
            // exposed while EGL reconnects or while a clip is restarted.
            image.visibility = View.INVISIBLE
            play.setOnClickListener {
                if (!interactionEnabled()) return@setOnClickListener
                val preview = prepare(root, slot, gestureId)
                    ?: return@setOnClickListener
                start(gestureId, preview)
            }

            // Inflate and configure before the root's first layout. Texture
            // surfaces then start together with the screen instead of being
            // attached one UI turn after the old bitmaps were already shown.
            prepare(root, slot, gestureId)?.setOnClickListener {
                if (interactionEnabled()) selectGesture(card)
            }
        }
    }

    fun release() {
        previews.values.forEach { preview ->
            preview.stopAtInitialPose()
            preview.setOnFirstFrameReadyListener(null)
        }
        // Keep every TextureView and its already presented frame attached.
        // A normal widget rebind/collapse must not render all 14 cards again.
    }

    private fun prepare(
        root: View,
        slot: Int,
        gestureId: Int
    ): CollectionGestureCardGLSurfaceViewV3? {
        val preview = root.findViewById<CollectionGestureCardGLSurfaceViewV3>(previewId(slot))
            ?: (root.findViewById<ViewStub>(previewStubId(slot))?.inflate()
                as? CollectionGestureCardGLSurfaceViewV3)
            ?: run {
                Log.e("Collection3D", "Preview XML was not inflated for gesture=$gestureId")
                return null
            }
        preview.configureGesture(gestureId)
        previews[gestureId] = preview
        preview.setOnFirstFrameReadyListener(null)
        preview.visibility = View.VISIBLE
        return preview
    }

    private fun start(gestureId: Int, preview: CollectionGestureCardGLSurfaceViewV3) {
        previews.values.filter { other -> other !== preview }.forEach { other ->
            other.stopAtInitialPose()
            other.visibility = View.VISIBLE
        }
        preview.configureGesture(gestureId)
        previews[gestureId] = preview
        preview.visibility = View.VISIBLE
        preview.playFromStart()
        Log.d("Collection3D", "Started gesture=$gestureId")
    }

    private fun cardId(slot: Int) = when (slot) {
        0 -> R.id.gestureCollection0Btn; 1 -> R.id.gestureCollection1Btn; 2 -> R.id.gestureCollection2Btn; 3 -> R.id.gestureCollection3Btn
        4 -> R.id.gestureCollection4Btn; 5 -> R.id.gestureCollection5Btn; 6 -> R.id.gestureCollection6Btn; 7 -> R.id.gestureCollection7Btn
        8 -> R.id.gestureCollection8Btn; 9 -> R.id.gestureCollection9Btn; 10 -> R.id.gestureCollection10Btn; 11 -> R.id.gestureCollection11Btn
        12 -> R.id.gestureCollection12Btn; else -> R.id.gestureCollection13Btn
    }
    private fun imageId(slot: Int) = when (slot) {
        0 -> R.id.gestureCollection0Iv; 1 -> R.id.gestureCollection1Iv; 2 -> R.id.gestureCollection2Iv; 3 -> R.id.gestureCollection3Iv
        4 -> R.id.gestureCollection4Iv; 5 -> R.id.gestureCollection5Iv; 6 -> R.id.gestureCollection6Iv; 7 -> R.id.gestureCollection7Iv
        8 -> R.id.gestureCollection8Iv; 9 -> R.id.gestureCollection9Iv; 10 -> R.id.gestureCollection10Iv; 11 -> R.id.gestureCollection11Iv
        12 -> R.id.gestureCollection12Iv; else -> R.id.gestureCollection13Iv
    }
    private fun playId(slot: Int) = when (slot) {
        0 -> R.id.gestureCollection0PlayBtn; 1 -> R.id.gestureCollection1PlayBtn; 2 -> R.id.gestureCollection2PlayBtn; 3 -> R.id.gestureCollection3PlayBtn
        4 -> R.id.gestureCollection4PlayBtn; 5 -> R.id.gestureCollection5PlayBtn; 6 -> R.id.gestureCollection6PlayBtn; 7 -> R.id.gestureCollection7PlayBtn
        8 -> R.id.gestureCollection8PlayBtn; 9 -> R.id.gestureCollection9PlayBtn; 10 -> R.id.gestureCollection10PlayBtn; 11 -> R.id.gestureCollection11PlayBtn
        12 -> R.id.gestureCollection12PlayBtn; else -> R.id.gestureCollection13PlayBtn
    }
    private fun previewStubId(slot: Int) = when (slot) {
        0 -> R.id.gestureCollection0PreviewStub; 1 -> R.id.gestureCollection1PreviewStub; 2 -> R.id.gestureCollection2PreviewStub; 3 -> R.id.gestureCollection3PreviewStub
        4 -> R.id.gestureCollection4PreviewStub; 5 -> R.id.gestureCollection5PreviewStub; 6 -> R.id.gestureCollection6PreviewStub; 7 -> R.id.gestureCollection7PreviewStub
        8 -> R.id.gestureCollection8PreviewStub; 9 -> R.id.gestureCollection9PreviewStub; 10 -> R.id.gestureCollection10PreviewStub; 11 -> R.id.gestureCollection11PreviewStub
        12 -> R.id.gestureCollection12PreviewStub; else -> R.id.gestureCollection13PreviewStub
    }
    private fun previewId(slot: Int) = when (slot) {
        0 -> R.id.gestureCollection0GlView; 1 -> R.id.gestureCollection1GlView; 2 -> R.id.gestureCollection2GlView; 3 -> R.id.gestureCollection3GlView
        4 -> R.id.gestureCollection4GlView; 5 -> R.id.gestureCollection5GlView; 6 -> R.id.gestureCollection6GlView; 7 -> R.id.gestureCollection7GlView
        8 -> R.id.gestureCollection8GlView; 9 -> R.id.gestureCollection9GlView; 10 -> R.id.gestureCollection10GlView; 11 -> R.id.gestureCollection11GlView
        12 -> R.id.gestureCollection12GlView; else -> R.id.gestureCollection13GlView
    }
}
