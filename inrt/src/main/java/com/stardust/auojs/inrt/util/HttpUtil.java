package com.stardust.auojs.inrt.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {
	public static String doPost(String posturl) {
		String resultData = "";
		try {
			URL url = new URL(posturl);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setReadTimeout(5000); // 设置从主机读取数据超时（单位：毫秒）
			urlConnection.setConnectTimeout(5000); // 设置连接主机超时（单位：毫秒）
			// 设置请求的头
			urlConnection.setRequestProperty("Content-Type", "application/x-java-serialized-object");
			urlConnection.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String inputLine = null;
			while (((inputLine = reader.readLine()) != null)) {
				resultData += inputLine + "\n";
			}
			// 关闭
			reader.close();
			urlConnection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultData;
	}
}
