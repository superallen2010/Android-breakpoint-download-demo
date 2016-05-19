package com.demo_downloadservice.services;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpStatus;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.demo_downloadservice.entities.FileInfo;

public class DownloadService extends Service {
	public static final String DOWNLOAD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test";
	public static final String ACTION_START = "ACTION_START";
	public static final String ACTION_STOP = "ACTION_STOP";
	public static final String ACTION_UPDATE = "ACTION_UPDATE";
	public static final int INIT_MSG = 0;
	
	private DownloadTask downloadTask = null;
	
	private Handler mHandler = new Handler(){

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case INIT_MSG:
				FileInfo fileInfo = (FileInfo)msg.obj;
				Log.i("lc", "Init:" + fileInfo);
				downloadTask = new DownloadTask(DownloadService.this, fileInfo);
				downloadTask.download();
				break; 

			default:
				break;
			}
		}
		
	};
	
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (ACTION_START.equals(intent.getAction())) {
			FileInfo fileInfo = (FileInfo) intent
					.getSerializableExtra("fileInfo");
			Log.i("test", "start:" + fileInfo.toString());
			//启动初始化线程
			new InitThread(fileInfo).start();
		} else if (ACTION_STOP.equals(intent.getAction())) {
			FileInfo fileInfo = (FileInfo) intent
					.getSerializableExtra("fileInfo");
			Log.i("test", "stop:" + fileInfo.toString());
			if(downloadTask != null){
				downloadTask.isPause = true;
			}
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/*
	 * 初始化子线程
	 */
	class InitThread extends Thread {
		private FileInfo mFileInfo = null;

		public InitThread(FileInfo mFileInfo) {
			super();
			this.mFileInfo = mFileInfo;
		}

		@Override
		public void run() {
			HttpURLConnection conn = null;
			RandomAccessFile raf  = null;
			try {
				// 连接网络
				URL url = new URL(mFileInfo.getUrl());
				conn = (HttpURLConnection)url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				int length = -1;
				if(conn.getResponseCode() == HttpStatus.SC_OK){
					//获取文件长度
					length = conn.getContentLength();
				}
				if(length<0){
					return;
				}
				
				File dirFile = new File(DOWNLOAD_PATH);
				if(!dirFile.exists()){
					dirFile.mkdir();
				}
				File file = new File(dirFile, mFileInfo.getFileName());
				//在文件任意文件写入操作
				raf = new RandomAccessFile(file, "rwd");
				raf.setLength(length);
				mFileInfo.setLength(length);
				mHandler.obtainMessage(INIT_MSG, mFileInfo).sendToTarget();
				//Message msg = Message.obtain();
				//msg.obj = mFileInfo;
				
				// 获取文件长度
				// 在本地创建文件
				// 设置文件长度
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				try {
					raf.close();
					conn.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
	}

}
