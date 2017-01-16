package com.examples.ffmpeg4android;

import com.examples.ffmpeg4android_demo.R;
import com.netcompss.ffmpeg4android.CommandValidationException;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.ffmpeg4android.Prefs;
import com.netcompss.ffmpeg4android.ProgressCalculator;
import com.netcompss.loader.LoadJNI;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ProgressBarExample extends Activity  {
	
	public ProgressDialog progressBar;
	
	String workFolder = null;
	String demoVideoFolder = null;
	String demoVideoPath = null;
	String vkLogPath = null;
	LoadJNI vk;
	private final int STOP_TRANSCODING_MSG = -1;
	private final int FINISHED_TRANSCODING_MSG = 0;
	private boolean commandValidationFailedFlag = false;
	
	private void runTranscodingUsingLoader() {
		Log.i(Prefs.TAG, "runTranscodingUsingLoader started...");

 		PowerManager powerManager = (PowerManager)ProgressBarExample.this.getSystemService(Activity.POWER_SERVICE);
		WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VK_LOCK"); 
		Log.d(Prefs.TAG, "Acquire wake lock");
		wakeLock.acquire();

 		EditText commandText = (EditText)findViewById(R.id.CommandText);
 		
 		String commandStr = commandText.getText().toString();
 		///////////// Set Command using code (overriding the UI EditText) /////
 		//String commandStr = "ffmpeg -y -i /sdcard/videokit/in.mp4 -strict experimental -s 320x240 -r 30 -aspect 4:3 -ab 48000 -ac 2 -ar 22050 -vcodec mpeg4 -b 2097152 /sdcard/videokit/out.mp4";
 		//String[] complexCommand = {"ffmpeg", "-y" ,"-i", "/sdcard/videokit/in.mp4","-strict","experimental","-vcodec", "mpeg4", "-b", "150k", "-s", "320x240","-r", "25", "-ab", "48000", "-ac", "2", "-ar", "22050","/sdcard/videokit/out.mp4"};
 		///////////////////////////////////////////////////////////////////////
 		
 		//commandStr = "ffmpeg -y -i /sdcard/videokit/sample.mp4 -strict experimental -s 320x240 -r 30 -aspect 4:3 -ab 48000 -ac 2 -ar 22050 -vcodec mpeg4 -b 2097152 /sdcard/videokit/out.mp4";
 		
 		vk = new LoadJNI();
		try {
			// running complex command with validation 
			//vk.run(complexCommand, workFolder, getApplicationContext());
			
			// running without command validation
			//vk.run(complexCommand, workFolder, getApplicationContext(), false);
			
			// running regular command with validation
			vk.run(GeneralUtils.utilConvertToComplex(commandStr), workFolder, getApplicationContext());
			
			Log.i(Prefs.TAG, "vk.run finished.");
			// copying vk.log (internal native log) to the videokit folder
			GeneralUtils.copyFileToFolder(vkLogPath, demoVideoFolder);
			
		} catch (CommandValidationException e) {
			Log.e(Prefs.TAG, "vk run exeption.", e);
			commandValidationFailedFlag = true;
		
		} catch (Throwable e) {
			Log.e(Prefs.TAG, "vk run exeption.", e);
		}
		finally {
			if (wakeLock.isHeld()) {
				wakeLock.release();
				Log.i(Prefs.TAG, "Wake lock released");
			}
			else{
				Log.i(Prefs.TAG, "Wake lock is already released, doing nothing");
			}
		}

		// finished Toast
		String rc = null;
		if (commandValidationFailedFlag) {
			rc = "Command Vaidation Failed";
		}
		else {
			rc = GeneralUtils.getReturnCodeFromLog(vkLogPath);
		}
		final String status = rc;
 		ProgressBarExample.this.runOnUiThread(new Runnable() {
			  public void run() {
				  Toast.makeText(ProgressBarExample.this, status, Toast.LENGTH_LONG).show();
				  if (status.equals("Transcoding Status: Failed")) {
					  Toast.makeText(ProgressBarExample.this, "Check: " + vkLogPath + " for more information.", Toast.LENGTH_LONG).show();
				  }
			  }
			});
	}
	
	
	@Override
	  public void onCreate(Bundle savedInstanceState) {
		  Log.i(Prefs.TAG, "onCreate ffmpeg4android ProgressBarExample");
		  		
	      super.onCreate(savedInstanceState);
	      setContentView(R.layout.ffmpeg_demo_client_2);
	      
	      demoVideoFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/videokit/";
	  	  demoVideoPath = demoVideoFolder + "in.mp4";
	      
	      Log.i(Prefs.TAG, getString(R.string.app_name) + " version: " + GeneralUtils.getVersionName(getApplicationContext()) );
	      
	      Button invoke =  (Button)findViewById(R.id.invokeButton);
	      invoke.setOnClickListener(new OnClickListener() {
				public void onClick(View v){
					Log.i(Prefs.TAG, "run clicked.");
					runTranscoding();
				}
			});
	      
	      workFolder = getApplicationContext().getFilesDir() + "/";
	      //Log.i(Prefs.TAG, "workFolder: " + workFolder);
	      vkLogPath = workFolder + "vk.log";
	      GeneralUtils.copyLicenseFromAssetsToSDIfNeeded(this, workFolder);
	      GeneralUtils.copyDemoVideoFromAssetsToSDIfNeeded(this, demoVideoFolder);
	      int rc = GeneralUtils.isLicenseValid(getApplicationContext(), workFolder);
	      Log.i(Prefs.TAG, "License check RC: " + rc);
	}
	
	
	
	private Handler handler = new Handler() {
        	@Override
        	public void handleMessage(Message msg) {
        		Log.i(Prefs.TAG, "Handler got message");
        		if (progressBar != null) {
        			progressBar.dismiss();
        			
        			// stopping the transcoding native
        			if (msg.what == STOP_TRANSCODING_MSG) {
        				Log.i(Prefs.TAG, "Got cancel message, calling fexit");
        				vk.fExit(getApplicationContext());
        				
        			
        			}
        		}
        }
    };
	
	public void runTranscoding() {
		  progressBar = new ProgressDialog(ProgressBarExample.this);
		  progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		  progressBar.setTitle("FFmpeg4Android Direct JNI");
		  progressBar.setMessage("Press the cancel button to end the operation");
		  progressBar.setMax(100);
		  progressBar.setProgress(0);
		  
		  progressBar.setCancelable(false);
		  progressBar.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
		      @Override
		      public void onClick(DialogInterface dialog, int which) {
		    	  handler.sendEmptyMessage(STOP_TRANSCODING_MSG);
		      }
		  });
		  
		  progressBar.show();
	      
	      new Thread() {
	          public void run() {
	        	  Log.d(Prefs.TAG,"Worker started");
	              try {
	                  //sleep(5000);
	            	  runTranscodingUsingLoader();
	                  handler.sendEmptyMessage(FINISHED_TRANSCODING_MSG);

	              } catch(Exception e) {
	                  Log.e("threadmessage",e.getMessage());
	              }
	          }
	      }.start();
	      
	      // Progress update thread
	      new Thread() {
	    	  ProgressCalculator pc = new ProgressCalculator(vkLogPath);
	          public void run() {
	        	  Log.d(Prefs.TAG,"Progress update started");
	        	  int progress = -1;
	        	  try {
	        		  while (true) {
	        			  sleep(300);
	        			  progress = pc.calcProgress();
	        			  if (progress != 0 && progress < 100) {
	        				  progressBar.setProgress(progress);
	        			  }
	        			  else if (progress == 100) {
	        				  Log.i(Prefs.TAG, "==== progress is 100, exiting Progress update thread");
	        				  pc.initCalcParamsForNextInter();
	        				  break;
	        			  }
	        		  }

	        	  } catch(Exception e) {
	        		  Log.e("threadmessage",e.getMessage());
	        	  }
	          }
	      }.start();
	  }

}
