package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityAboutBinding
import com.v2ray.ang.util.Utils

class AboutActivity : BaseActivity() {
    private val binding by lazy { ActivityAboutBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithToolbar(binding.root, showHomeAsUp = true, title = getString(R.string.title_about))

        binding.tileTelegram.setOnClickListener {
            Utils.openUri(this, AppConfig.TG_CHANNEL_URL)
        }

        binding.tileLogcat.setOnClickListener {
            startActivity(Intent(this, LogcatActivity::class.java))
        }

        binding.tileCheckUpdate.setOnClickListener {
            startActivity(Intent(this, CheckUpdateActivity::class.java))
        }

        binding.tvUpdateVersion.text = getString(R.string.about_update_current, BuildConfig.VERSION_NAME)

        binding.tvAboutFooter.text = getString(R.string.about_footer, BuildConfig.VERSION_NAME)
    }
}
