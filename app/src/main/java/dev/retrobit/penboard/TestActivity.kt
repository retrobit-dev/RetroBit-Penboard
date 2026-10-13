package dev.retrobit.penboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import dev.retrobit.penboard.databinding.ActivityTestBinding

/** Halaman uji coba: kolom teks sederhana supaya keyboard Penboard bisa dipakai. */
class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.header.headerTitle.text = getString(R.string.test_title)
        binding.header.headerSubtitle.text = getString(R.string.app_subtitle)
        binding.header.headerIcon.setImageResource(R.drawable.ic_launcher_foreground)
        binding.openSettings.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.field.requestFocus()
        binding.field.postDelayed({
            val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.showSoftInput(binding.field, InputMethodManager.SHOW_IMPLICIT)
        }, 250)
    }
}
