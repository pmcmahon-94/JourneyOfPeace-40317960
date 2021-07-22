package com.example.journeyofpeace

import android.app.Activity
import android.app.ActivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class FirstLocation : AppCompatActivity() {

    private var arFragment: ArFragment? = null
    private var lampPostRenderable: ModelRenderable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_main)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        ModelRenderable.builder().setSource(this, Uri.parse("lamppost.sfb")).build()
            .thenAccept { renderable: ModelRenderable? ->
                lampPostRenderable = renderable
            }
            .exceptionally {
                val toast = Toast.makeText(
                    this, "Unable to load andy renderable",
                    Toast.LENGTH_LONG
                )
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                null
            }
        arFragment!!.setOnTapArPlaneListener { hitresult: HitResult, _: Plane?, _: MotionEvent? ->
            if (lampPostRenderable == null) {
                return@setOnTapArPlaneListener
            }
            val anchor = hitresult.createAnchor()
            val anchorNode =
                AnchorNode(anchor)
            anchorNode.setParent(arFragment!!.arSceneView.scene)
            val lamp = TransformableNode(arFragment!!.transformationSystem)
            lamp.setParent(anchorNode)
            lamp.renderable = lampPostRenderable
            lamp.select()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val MIN_OPENGL_VERSION = 3.0
        fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
            val openGlVersionString =
                (activity.getSystemService(ACTIVITY_SERVICE) as ActivityManager)
                    .deviceConfigurationInfo.glEsVersion
            if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
                Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
                Toast.makeText(
                    activity,
                    "Sceneform requires OpenGL ES 3.0 or later",
                    Toast.LENGTH_LONG
                ).show()
                activity.finish()
                return false
            }
            return true
        }
    }
}