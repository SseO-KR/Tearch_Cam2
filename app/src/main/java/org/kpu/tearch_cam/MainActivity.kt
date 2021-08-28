package org.kpu.tearch_cam

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.widget.Button
import android.widget.EditText



import org.kpu.tearch_cam.CameraActivity
import org.kpu.tearch_cam.R
import org.kpu.tearch_cam.utils.Constants


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(Constants.TAG, "LottieActivity - onCreate() called")



        val btn_number = findViewById<Button>(R.id.btn_number)
        btn_number.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            val edit_num = findViewById<EditText>(R.id.edit_number)
            val cafeNum = edit_num.text.toString().toInt()
            intent.putExtra("cafeNum", cafeNum)
            startActivity(intent)
        }


    }





}