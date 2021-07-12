package com.stardust.auojs.inrt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;


import androidx.annotation.RequiresApi;

import com.stardust.auojs.inrt.util.MarketJavascriptInterface;

import java.util.List;

public class FeatureActivity extends Activity {
	private WebView webView;
	private ProgressBar mProgressBar;
	private String url ="http://mk.autoxjs.com/pages/ykapp/choiseFeature";
	MarketJavascriptInterface marketJavascriptInterface;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feature_webview);
		init();
	}

	public void onResume() {
		super.onResume();
	}


	public void onPause() {
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		if(webView.canGoBack()){
			webView.goBack();
		}else{
			super.onBackPressed();
		}

	}

	@SuppressLint({ "NewApi", "SetJavaScriptEnabled" })
	private void init() {
		marketJavascriptInterface = new MarketJavascriptInterface(this);
		webView = (WebView) findViewById(R.id.webView);
		mProgressBar =(ProgressBar) findViewById(R.id.progress_bar);
		WebSettings settings = webView.getSettings();
		webView.setWebViewClient(new WebViewClient());
		webView.loadUrl(url);
		settings.setJavaScriptEnabled(true);
		settings.setSupportZoom(true);
		//settings.setBuiltInZoomControls(true);
		//扩大比例的缩放
		///----
		settings.setBuiltInZoomControls(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(true);
		settings.setDomStorageEnabled(true);
		settings.setDisplayZoomControls(false);
		///-----

		settings.setUseWideViewPort(true);
		//自适应屏幕
		settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
		settings.setLoadWithOverviewMode(true);
		webView.setWebViewClient(new MyWebViewClient());
		webView.setWebChromeClient(new MyWebChromeClient());
		webView.addJavascriptInterface(marketJavascriptInterface,"android");


	}

	protected class MyWebChromeClient extends WebChromeClient {

		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			super.onProgressChanged(view, newProgress);
			mProgressBar.setProgress(newProgress);
		}
	}
	protected class MyWebViewClient extends WebViewClient {

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			mProgressBar.setProgress(0);
			mProgressBar.setVisibility(View.VISIBLE);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			mProgressBar.setVisibility(View.GONE);
		}

		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
			handler.sendEmptyMessage(3);
			super.onReceivedSslError(view, handler, error);
		}

		@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
			return shouldOverrideUrlLoading(view, request.getUrl().toString());
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if ((!url.endsWith(".apk")&&!url.contains(".apk?"))
					&&(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://"))) {
				view.loadUrl(url);
			} else {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				List<ResolveInfo> intentActivities = FeatureActivity.this.getPackageManager().queryIntentActivities(intent, 0);
				if (intentActivities.isEmpty()) {
					return false;
				}
				try {
					FeatureActivity.this.startActivity(Intent.createChooser(intent, "打开方式"));
				} catch (ActivityNotFoundException e) {
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}
	}




}
