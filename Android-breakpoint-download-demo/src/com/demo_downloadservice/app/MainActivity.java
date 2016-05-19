package com.demo_downloadservice.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.demo_downloadservice.entities.FileInfo;
import com.demo_downloadservice.services.DownloadService;
import com.example.demo_downloadservice.R;

public class MainActivity extends Activity {

	private TextView mTvTitle = null;
	private ProgressBar mPbProgressBar = null;
	private Button mBtStart = null;
	private Button mBtStop = null;
	//����㲥������
	private BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			if(DownloadService.ACTION_UPDATE.equals(intent.getAction())){
				int finished = intent.getIntExtra("finished", 0);
				Log.i("lc", "finished:" + finished);
				mPbProgressBar.setProgress(finished);
			}
		}
	};
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// ��ʼ���ؼ�
		setUpWidget();
		// �����ļ���Ϣ����
		final FileInfo fileInfo = new FileInfo(
				0,
				"http://gdown.baidu.com/data/wisegame/18d56bb24a23a691/kugouyinle_8066.apk",
				"�ṷ����", 0, 0);
		
		//�����¼�����
		mBtStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//ͨ��intent���ݲ�����service
				Intent intent = new Intent(MainActivity.this, DownloadService.class);
				intent.setAction(DownloadService.ACTION_START);
				intent.putExtra("fileInfo", fileInfo);
				startService(intent);
			}
		});
		
		mBtStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//ͨ��intent���ݲ�����service
				Intent intent = new Intent(MainActivity.this, DownloadService.class);
				intent.setAction(DownloadService.ACTION_STOP);
				intent.putExtra("fileInfo", fileInfo);
				startService(intent);
			}
		});
	}
	
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}
	
	

	// ��ʼ�����
	private void setUpWidget() {
		mTvTitle = (TextView) findViewById(R.id.tvTitle);
		mPbProgressBar = (ProgressBar) findViewById(R.id.pbProgress);
		mBtStart = (Button) findViewById(R.id.btStart);
		mBtStop = (Button) findViewById(R.id.btStop);
		mPbProgressBar.setMax(100);
		//ע��㲥������
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadService.ACTION_UPDATE);
		registerReceiver(mReceiver, filter);
	}

}
