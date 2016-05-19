package com.demo_downloadservice.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.apache.http.HttpStatus;

import android.content.Context;
import android.content.Intent;
import android.nfc.cardemulation.OffHostApduService;
import android.util.Log;

import com.demo_downloadservice.db.ThreadDAOImpl;
import com.demo_downloadservice.entities.FileInfo;
import com.demo_downloadservice.entities.ThreadInfo;

/**
 * 下载文件类
 * @author Allen
 *
 */
public class DownloadTask {
	
	private Context mContext = null;
	private FileInfo mFileInfo = null;
	private ThreadDAOImpl mDao = null;
	public boolean isPause = false;
	private ThreadInfo threadInfo = null;
	private int finished = 0;
	
	public DownloadTask(Context mContext, FileInfo mFileInfo) {
		this.mContext = mContext;
		this.mFileInfo = mFileInfo;
		mDao = new ThreadDAOImpl(mContext);
	} 
	
	//下载
	public void download(){
		//读取数据库线程信息
		List<ThreadInfo> threadInfos = mDao.getThreads(mFileInfo.getUrl());
		
		if(threadInfos.size() == 0){
			Log.i("lc", "size0:" + 0);
			threadInfo = new ThreadInfo(0, mFileInfo.getUrl(), 0, mFileInfo.getLength(), 0);
		}else{
			Log.i("lc", "size:" + threadInfos.size());
			threadInfo = threadInfos.get(0);
		}
		//创建子线程开始下载
		new DownloadThread(threadInfo).start();
	}
	
	
	/*
	 * 下载线程
	 */
	
	class DownloadThread extends Thread{
		private ThreadInfo mThreadInfo = null;
		
		public DownloadThread(ThreadInfo mThreadInfo) {
			this.mThreadInfo = mThreadInfo;
		}

		@Override
		public void run() {
			//向数据库出入线程信息
			if(!mDao.isExists(mThreadInfo.getUrl(), mThreadInfo.getId())){
				Log.i("lc", "mDao.isExists:" );
				mDao.insertThread(mThreadInfo);
			}
			HttpURLConnection conn = null;
			InputStream is = null;
			RandomAccessFile raf = null;
			try {
				URL url = new URL(mFileInfo.getUrl());
				conn = (HttpURLConnection)url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				//设置下载位置
				int start = mThreadInfo.getStart() + mThreadInfo.getFinished();
				conn.setRequestProperty("Range", "bytes=" + start + "-" + mThreadInfo.getEnd());
				//设置文件写入位置
				File file = new File(DownloadService.DOWNLOAD_PATH,mFileInfo.getFileName());
				raf = new RandomAccessFile(file, "rwd");
				raf.seek(start);
				Intent intent = new Intent(DownloadService.ACTION_UPDATE);
				finished = mThreadInfo.getFinished();
				
				
				//开始下载
				if(conn.getResponseCode() == HttpStatus.SC_PARTIAL_CONTENT){
					//读取数据
					is = conn.getInputStream();
					byte[] buffer = new byte[1024*4];
					int len = -1;
					long time = System.currentTimeMillis();
					while ((len = is.read(buffer)) != -1) {
						//写入文件
						raf.write(buffer, 0, len);
						//把下载进度广播给Avtivity
						finished += len;
						if(System.currentTimeMillis() - time > 500){
							time = System.currentTimeMillis();
							intent.putExtra("finished", finished * 100 / mFileInfo.getLength());
							mContext.sendBroadcast(intent);
						}
						
						//下载暂停时，保存下载进度
						if(isPause){
							mDao.updateThread(mThreadInfo.getUrl(), mThreadInfo.getId(), finished);
							return;
						}
					}
					//下载完成，删除线程
					mDao.deleteThread(mThreadInfo.getUrl(), mThreadInfo.getId());
					
				}
				
				
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				try {
					is.close();
					raf.close();
					conn.disconnect();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			
			
			
			//开始下载
			
		}
		
	}
	
	
}
