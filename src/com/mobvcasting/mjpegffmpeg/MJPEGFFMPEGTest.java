package com.mobvcasting.mjpegffmpeg;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MJPEGFFMPEGTest extends Activity { 
	
	public static final String LOGTAG = "MJPEG_FFMPEG";

	boolean recording;
	boolean previewRunning = false;	
	
	File jpegFile;			
	int fileCount = 0;
	
	FileOutputStream fos;
	BufferedOutputStream bos;
	Button recordButton, startScreenShot, increaseDuration;
	
	NumberFormat fileCountFormatter = new DecimalFormat("00000");
	String formattedFileCount;
	
	ProcessVideo processVideo;

	ProcessVideo1 processVideo1;
		
	String[] libraryAssets = {"ffmpeg",
			"libavcodec.so", "libavcodec.so.52", "libavcodec.so.52.99.1",
			"libavcore.so", "libavcore.so.0", "libavcore.so.0.16.0",
			"libavdevice.so", "libavdevice.so.52", "libavdevice.so.52.2.2",
			"libavfilter.so", "libavfilter.so.1", "libavfilter.so.1.69.0",
			"libavformat.so", "libavformat.so.52", "libavformat.so.52.88.0",
			"libavutil.so", "libavutil.so.50", "libavutil.so.50.34.0",
			"libswscale.so", "libswscale.so.0", "libswscale.so.0.12.0"
	};
	
	TextView tv1;
	LinearLayout ll1;
	int count;
	ImageView image;
	View v1;
	Bitmap bm ;
	BitmapDrawable bitmapDrawable;
	boolean screenEnable;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        for (int i = 0; i < libraryAssets.length; i++) {
			try {
				InputStream ffmpegInputStream = this.getAssets().open(libraryAssets[i]);
		        FileMover fm = new FileMover(ffmpegInputStream,"/data/data/com.mobvcasting.mjpegffmpeg/" + libraryAssets[i]);
		        fm.moveIt();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
        Process process = null;
        
        try {
        	String[] args = {"/system/bin/chmod", "755", "/data/data/com.mobvcasting.mjpegffmpeg/ffmpeg"};
        	process = new ProcessBuilder(args).start();        	
        	try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	process.destroy();
        	 			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File savePath = new File(Environment.getExternalStorageDirectory().getPath() + "/com.mobvcasting.mjpegffmpeg/");
		savePath.mkdirs();
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		setContentView(R.layout.main);
		
		tv1 = (TextView) findViewById(R.id.textView1);
		tv1.setText("count=" + count);
		ll1 = (LinearLayout) findViewById(R.id.LinearLayout01);
		image = (ImageView) findViewById(R.id.screenshots);
		v1 = ll1.getRootView();
		v1.setDrawingCacheEnabled(true);
		
		count=0;
		screenEnable = false;
		
		startScreenShot= (Button) this.findViewById(R.id.startScreen);
		startScreenShot.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(screenEnable==false)
				{
					screenEnable = true;
					startScreenShot.setText("Stop");
				}
				else
				{
					screenEnable = false;
					startScreenShot.setText("Start");
				}
			}
		});
		
		recordButton = (Button) this.findViewById(R.id.RecordButton);
		recordButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				processVideo = new ProcessVideo();
				processVideo.execute();
			}
		}) ;
		
		increaseDuration = (Button) this.findViewById(R.id.IncreaseButton);
		increaseDuration.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				processVideo1 = new ProcessVideo1();
				processVideo1.execute();
			}
		}) ;
		
		Timer T = new Timer();
		T.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						
						if(screenEnable==true)
						{
							count++;
							tv1.setText("count=" + count); 
							screenShot(count);
						}
						else
						{
							count=0;
						}
					}
				});
			}
		}, 1000, 1000);
		
		
    }

    @SuppressLint("NewApi")
	public void screenShot(int n)
	{
		image.setBackground(null);
		
		bm = v1.getDrawingCache();
		bitmapDrawable = new BitmapDrawable(bm);
		
		image.setBackgroundDrawable(bitmapDrawable); 
		
		fileCount=n;
		formattedFileCount = fileCountFormatter.format(fileCount);  
		jpegFile = new File(Environment.getExternalStorageDirectory().getPath() + "/com.mobvcasting.mjpegffmpeg/frame_" + formattedFileCount + ".jpg");
	
		try {
		       FileOutputStream out = new FileOutputStream(jpegFile);
		       bm.compress(Bitmap.CompressFormat.JPEG, 9, out);
		       out.flush();
		       out.close();

		} catch (Exception e) {
		       e.printStackTrace();
		}
		
	}
	    
	private class ProcessVideo extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {

	        Process ffmpegProcess = null;
	        
	        try {
	        	
	        	//ffmpeg -r 10 -b 1800 -i %03d.jpg test1800.mp4
	        	// 00000
	        	// /data/data/com.mobvcasting.ffmpegcommandlinetest/ffmpeg -r p.getPreviewFrameRate() -b 1000 -i frame_%05d.jpg video.mov
	        	
				//String[] args2 = {"/data/data/com.mobvcasting.ffmpegcommandlinetest/ffmpeg", "-y", "-i", "/data/data/com.mobvcasting.ffmpegcommandlinetest/", "-vcodec", "copy", "-acodec", "copy", "-f", "flv", "rtmp://192.168.43.176/live/thestream"};
	        	//Log.d(LOGTAG, " p.getPreviewFrameRate():"+ 30);
				
	        	
	        	String[] ffmpegCommand = {"/data/data/com.mobvcasting.mjpegffmpeg/ffmpeg", "-r", 
	        			""+30,"-b", "1000000", "-vcodec", "mjpeg", "-i",
	        			Environment.getExternalStorageDirectory().getPath() + 
	        			"/com.mobvcasting.mjpegffmpeg/frame_%05d.jpg", 
	        			Environment.getExternalStorageDirectory().getPath()
	        			+ "/com.mobvcasting.mjpegffmpeg/video.mov"};
				
				ffmpegProcess = new ProcessBuilder(ffmpegCommand).redirectErrorStream(true).start();         	
				
				OutputStream ffmpegOutStream = ffmpegProcess.getOutputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));

				String line;
				
				Log.v(LOGTAG,"***Starting FFMPEG***");
				while ((line = reader.readLine()) != null)
				{
					Log.v(LOGTAG,"***"+line+"***");
				}
				Log.v(LOGTAG,"***Ending FFMPEG***");
	
	    
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
	        
	        if (ffmpegProcess != null) {
	        	ffmpegProcess.destroy();        
	        }
	        
	        return null;
		}
		
	     protected void onPostExecute(Void... result) {
	    	 Toast toast = Toast.makeText(MJPEGFFMPEGTest.this, "Done Processing Video", Toast.LENGTH_LONG);
	    	 toast.show();
	     }
	}
	
	private class ProcessVideo1 extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {

	        Process ffmpegProcess1 = null;
	        
	        try {
	        	
	        	//ffmpeg -r 10 -b 1800 -i %03d.jpg test1800.mp4
	        	// 00000
	        	// /data/data/com.mobvcasting.ffmpegcommandlinetest/ffmpeg -r p.getPreviewFrameRate() -b 1000 -i frame_%05d.jpg video.mov
	        	
				//String[] args2 = {"/data/data/com.mobvcasting.ffmpegcommandlinetest/ffmpeg", "-y", "-i", "/data/data/com.mobvcasting.ffmpegcommandlinetest/", "-vcodec", "copy", "-acodec", "copy", "-f", "flv", "rtmp://192.168.43.176/live/thestream"};
	        	//Log.d(LOGTAG, " p.getPreviewFrameRate():"+ 30);
				
	        	
	        	String[] ffmpegCommand1 = {"/data/data/com.mobvcasting.mjpegffmpeg/ffmpeg", "-r", 
	        			""+30,"-b", "1000000", "-vcodec", "mjpeg", "-i",
	        			Environment.getExternalStorageDirectory().getPath() + 
	        			"/com.mobvcasting.mjpegffmpeg/frame_%05d.jpg", 
	        			Environment.getExternalStorageDirectory().getPath()
	        			+ "/com.mobvcasting.mjpegffmpeg/video.mov"};
	        	
	        	String[] ffmpegCommand2 = {"/data/data/com.mobvcasting.mjpegffmpeg/ffmpeg", "-i",
	        			Environment.getExternalStorageDirectory().getPath()
	        			+ "/com.mobvcasting.mjpegffmpeg/video.mov", "-vf",
	        			"setpts=12.0*PTS", Environment.getExternalStorageDirectory().getPath()
	        			+ "/com.mobvcasting.mjpegffmpeg/video1.mov"};
				
				ffmpegProcess1 = new ProcessBuilder(ffmpegCommand2).redirectErrorStream(true).start();         	
				
				OutputStream ffmpegOutStream1 = ffmpegProcess1.getOutputStream();
				BufferedReader reader1 = new BufferedReader(new InputStreamReader(ffmpegProcess1.getInputStream()));

				String line;
				
				Log.v(LOGTAG,"***Starting FFMPEG***");
				while ((line = reader1.readLine()) != null)
				{
					Log.v(LOGTAG,"***"+line+"***");
				}
				Log.v(LOGTAG,"***Ending FFMPEG***");
	
	    
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
	        
	        if (ffmpegProcess1 != null) {
	        	ffmpegProcess1.destroy();        
	        }
	        
	        return null;
		}
		
	     protected void onPostExecute(Void... result) {
	    	 Toast toast = Toast.makeText(MJPEGFFMPEGTest.this, "Done Processing Video", Toast.LENGTH_LONG);
	    	 toast.show();
	     }
	}
 }