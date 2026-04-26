package com.example.a12quiz

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.a12quiz.ui.QuestionsActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val startButton: Button = findViewById(R.id.button_start)
        val editTextName: EditText = findViewById(R.id.name)

        startButton.setOnClickListener {
            if (editTextName.text.isNotEmpty()) {
                Intent(this@MainActivity, QuestionsActivity::class.java).also {
                    startActivity(it)
                    finish() //Stops back button from going from questions to main
                }
            } else {
                Toast.makeText(this@MainActivity, "Please enter your name", Toast.LENGTH_LONG).show()
            }
        }

    } //22:16:22
}