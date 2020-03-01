package com.example.webviewfragmentex

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        moveToWebView(PSE_COMMUNITY_URL, "title")
//        moveToWebView(GUC_COMMUNITY_URL, "title")
    }

    private fun moveToWebView(url: String, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CustomWebViewFragment.newInstance(url, title))
            .commit()
    }

    companion object {
        private const val GUC_COMMUNITY_URL =
            "https://steamcommunity.com/groups/zeguc/announcements"
        private const val PSE_COMMUNITY_URL = "https://steamcommunity.com/groups/POSSESSION"
    }
}
