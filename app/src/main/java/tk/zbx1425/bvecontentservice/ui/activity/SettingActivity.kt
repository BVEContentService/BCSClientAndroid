package tk.zbx1425.bvecontentservice.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import tk.zbx1425.bvecontentservice.ui.component.SettingFragment


class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.beginTransaction()
                .add(
                    android.R.id.content,
                    SettingFragment()
                )
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}