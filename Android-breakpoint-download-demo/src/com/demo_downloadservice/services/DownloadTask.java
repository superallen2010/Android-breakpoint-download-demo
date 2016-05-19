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
 * �����ļ���
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
	
	//����
	public void download(){
		//��ȡ���ݿ��߳���Ϣ
		List<ThreadInfo> threadInfos = mDao.getThreads(mFileInfo.getUrl());
		
		if(threadInfos.size() == 0){
			Log.i("lc", "size0:" + 0);
			threadInfo = new ThreadInfo(0, mFileInfo.getUrl(), 0, mFileInfo.getLength(), 0);
		}else{
			Log.i("lc", "size:" + threadInfos.size());
			threadInfo = threadInfos.get(0);
		}
		//�������߳̿�ʼ����
		new DownloadThread(threadInfo).start();
	}
	
	
	/*
	 * �����߳�
	 */
	
	class DownloadThread extends Thread{
		private ThreadInfo mThreadInfo = null;
		
		public DownloadThread(ThreadInfo mThreadInfo) {
			this.mThreadInfo = mThreadInfo;
		}

		@Override
		public void run() {
			//�����ݿ�����߳���Ϣ
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
				//��������λ��
				int start = mThreadInfo.getStart() + mThreadInfo.getFinished();
				conn.setRequestProperty("Range", "bytes=" + start + "-" + mThreadInfo.getEnd());
				//�����ļ�д��λ��
				File file = new File(DownloadService.DOWNLOAD_PATH,mFileInfo.getFileName());
				raf = new RandomAccessFile(file, "rwd");
				raf.seek(start);
				Intent intent = new Intent(DownloadService.ACTION_UPDATE);
				finished = mThreadInfo.getFinished();
				
				
				//��ʼ����
				if(conn.getResponseCode() == HttpStatus.SC_PARTIAL_CONTENT){
					//��ȡ����
					is = conn.getInputStream();
					byte[] buffer = new byte[1024*4];
					int len = -1;
					long time = System.currentTimeMillis();
					while ((len = is.read(buffer)) != -1) {
						//д���ļ�
						raf.write(buffer, 0, len);
						//�����ؽ��ȹ㲥��Avtivity
						finished += len;
						if(System.currentTimeMillis() - time > 500){
							time = System.currentTimeMillis();
							intent.putExtra("finished", finished * 100 / mFileInfo.getLength());
							mContext.sendBroadcast(intent);
						}
						
						//������ͣʱ���������ؽ���
						if(isPause){
							mDao.updateThread(mThreadInfo.getUrl(), mThreadInfo.getId(), finished);
							return;
						}
					}
					//������ɣ�ɾ���߳�
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
			
			
			
			
			//��ʼ����
			
		}
		
	}
	
	
}
