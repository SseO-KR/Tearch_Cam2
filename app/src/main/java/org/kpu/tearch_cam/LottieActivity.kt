package org.kpu.tearch_cam

import android.animation.Animator
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.airbnb.lottie.LottieAnimationView
import org.kpu.tearch_cam.utils.Constants.TAG

class LottieActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lottie)

        Log.d(TAG, "LottieActivity - onCreate() called")

        val animationView = findViewById<LottieAnimationView>(R.id.lottie)

        animationView.addAnimatorListener(object: Animator.AnimatorListener{
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                Log.d(TAG, "LottiActivity - finish()")
                finish()
                val intent = Intent(this@LottieActivity, MainActivity::class.java)
                startActivity(intent)
            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {

            }
        })
    }
}