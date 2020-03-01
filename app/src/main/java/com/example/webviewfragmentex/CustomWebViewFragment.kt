package com.example.webviewfragmentex

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.include_webview_footer_layout.*

/**
 * CustomWebViewFragment
 *
 */
class CustomWebViewFragment : Fragment() {
    private lateinit var webView: WebView
    private lateinit var progressLayout: View
    private lateinit var internetErrorLayout: View
    private lateinit var footerLayout: View
    private var url: String? = null
    private var title: String? = null

    //--------------------------------------------------- LifeCycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            url = it.getString(ARGS_URL)
            title = it.getString(ARGS_TITLE)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = onClickGoBack()
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    //--------------------------------------------------- initView
    @SuppressLint("SetJavaScriptEnabled")
    private fun initView() = view?.let { v ->
        footerLayout = v.findViewById(R.id.footerLayout)
        progressLayout = v.findViewById(R.id.loadingProgressLayout)
        internetErrorLayout = v.findViewById(R.id.internetErrorLayout)
        webView = v.findViewById(R.id.webView)

        // JavaScriptを有効化
        webView.settings.javaScriptEnabled = true

        // Web Storage を有効化
        webView.settings.domStorageEnabled = true
        webView.webViewClient = createWebViewClient()

        webView.clearCache(true)
        webView.loadUrl(url)

        initFooterLayout()

        // Hardware Acceleration ON
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
    }

    private fun onClickGoBack() = when (webView.canGoBack()) {
        true -> webView.goBack()
        else -> closeFragment()
    }

    private fun closeFragment() {
        requireActivity().finish()
    }

    //--------------------------------------------------- WebViewClient
    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return handleOverrideUrlLoading(view, Uri.parse(url))
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return handleOverrideUrlLoading(view, request?.url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                Log.d(LOG_TAG, "onPageStarted(): url=$url")
                super.onPageStarted(view, url, favicon)

                showInternetError(false)
                showLoadingProgress(true)
                updateFooterLayout()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.d(LOG_TAG, "onReceivedError(): url=${view?.url}")
                super.onReceivedError(view, request, error)

                showInternetError(true)
                updateFooterLayout()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(LOG_TAG, "onPageFinished(): url=${url}")
                super.onPageFinished(view, url)

                showLoadingProgress(false)
                updateFooterLayout()
            }

            fun handleOverrideUrlLoading(view: WebView?, uri: Uri?): Boolean {
                val url = view?.url
                Log.d(LOG_TAG, "handleOverrideUrlLoading(): url=${url}")
                updateFooterLayout()

                if (URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url)) {
                    return false
                }

                uri ?: return false
                Intent(Intent.ACTION_VIEW).also {
                    it.data = uri
                    startActivity(it)
                }
                return true
            }
        }
    }

    //--------------------------------------------------- LoadingProgress
    private fun showLoadingProgress(active: Boolean) = when (active) {
        true -> progressLayout.visibility = View.VISIBLE
        else -> progressLayout.visibility = View.GONE
    }

    //--------------------------------------------------- InternetError
    private fun showInternetError(active: Boolean) = when (active) {
        true -> setupInternetErrorLayout(View.VISIBLE, true, onClick = { webView.reload() })
        else -> setupInternetErrorLayout(View.GONE, false, null)
    }


    private fun setupInternetErrorLayout(
        visibility: Int,
        isClickable: Boolean,
        onClick: (() -> Unit)?
    ) = internetErrorLayout.let {
        it.visibility = visibility
        it.isClickable = isClickable
        it.setOnClickListener {
            onClick?.invoke()
        }
    }

    //--------------------------------------------------- WebViewFooterLayout
    private fun initFooterLayout() {
        // init clicks
        webview_go_back_button.setOnClickListener {
            onClickGoBack()
        }

        webview_go_forward_button.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }
        webview_refresh_button.setOnClickListener {
            webView.reload()
        }
        webview_menu_list_button.setOnClickListener {
            showMenuListDialog()
        }

        updateFooterLayout()
    }

    private fun showMenuListDialog() {
        val items = FOOTER_MENU_LIST_ITEMS
        AlertDialog.Builder(requireActivity())
            .setItems(items) { _, which ->
                val item = items[which]
                Log.d(LOG_TAG, "click menu list item: $item")
                val url = webView.url
                when (which) {
                    0 -> openExternalBrowser(url)
                    1 -> copyToClipboard(requireContext(), "", url)
                    else -> {
                        // NOP
                    }
                }
            }
            .show()
    }

    private fun openExternalBrowser(url: String) = try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        // NOP
    }

    private fun copyToClipboard(
        context: Context,
        label: String?,
        text: String?
    ) {
        val clipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun updateFooterLayout() {
        when (webView.canGoBack()) {
            true -> setFooterItemIsClickable(webview_go_back_button, true)
            else -> setFooterItemIsClickable(webview_go_back_button, false)
        }
        when (webView.canGoForward()) {
            true -> setFooterItemIsClickable(webview_go_forward_button, true)
            else -> setFooterItemIsClickable(webview_go_forward_button, false)
        }
    }

    private fun setFooterItemIsClickable(view: TextView, isClickable: Boolean) {
        view.isClickable = isClickable
        when (view.isClickable) {
            true -> view.setTextColor(Color.WHITE)
            else -> view.setTextColor(Color.GRAY)
        }
    }

    //--------------------------------------------------- companion object
    companion object {
        val LOG_TAG: String = CustomWebViewFragment::class.java.simpleName
        const val ARGS_URL: String = "ARGS_URL"
        const val ARGS_TITLE: String = "ARGS_TITLE"
        val FOOTER_MENU_LIST_ITEMS = arrayOf("ブラウザで開く", "リンクをコピー")

        fun newInstance(
            url: String,
            title: String
        ) = CustomWebViewFragment().also { f ->
            f.arguments = Bundle().also { b ->
                b.putString(ARGS_URL, url)
                b.putString(ARGS_TITLE, title)
            }
        }
    }
}