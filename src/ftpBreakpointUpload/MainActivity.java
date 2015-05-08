package ftpBreakpointUpload;

import java.io.File;
import java.io.IOException;

import publicFunction.ftpFile;

import com.example.ftpbreakpointupload.R;

import ftpBreakpointUpload.FTPUpload.UploadProgressListener;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";  
      
    public static final String FTP_CONNECT_SUCCESS = "ftp连接成功";  
    public static final String FTP_CONNECT_FAIL = "ftp连接失败";  
    public static final String FTP_DISCONNECT_SUCCESS = "ftp断开连接";  
      
    public static final String FTP_UPLOAD_SUCCESS = "ftp文件上传成功";  
    public static final String FTP_UPLOAD_FAIL = "ftp文件上传失败";  
    public static final String FTP_UPLOAD_LOADING = "ftp文件正在上传"; 
    
    public File file;
    private ProgressBar progressBar;
    private EditText selectedFile;
    private TextView fileName;
    private TextView uploadedPercent;
    private TextView uploadedSpeed;
    private String filePath;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//设置activity的显示界面
		setContentView(R.layout.activity_main);
		//进度条
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		progressBar.setIndeterminate(false);
		//已选择的文件或文件夹
		selectedFile = (EditText)findViewById(R.id.selectedFile);
		ImageButton selectFileBtn = (ImageButton)findViewById(R.id.selectFileBtn);
		//当前上次的文件或文件夹
		fileName = (TextView)findViewById(R.id.fileName);
		//已下载的百分比
		uploadedPercent = (TextView)findViewById(R.id.uploadedPercent);
		//当前下载的网速
		uploadedSpeed = (TextView)findViewById(R.id.uploadedSpeed);
		selectFileBtn.setOnClickListener(new OnClickListener() {       
        	@Override        
            public void onClick(View v) { 
        		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        		intent.setType("*/*");
        		intent.addCategory(Intent.CATEGORY_OPENABLE);
        		try {
        			startActivityForResult(intent, 1/*Intent.createChooser(intent, "请选择一个要上传的文件"),FILE_SELECT_CODE*/);
        		} catch (android.content.ActivityNotFoundException ex) {
        			// Potentially direct the user to the Market with a Dialog
        			Toast.makeText(MainActivity.this, "请安装文件管理器", Toast.LENGTH_SHORT).show();
        		}
            }
        });
		Button uploadBtn = (Button)findViewById(R.id.uploadBtn);
		uploadBtn.setOnClickListener(new OnClickListener() {       
        	@Override        
            public void onClick(View v) { 
        		if(filePath!=null){
        			file = new File(filePath);
        			new fileUploadThread(file).start(); 
        		}else{
        			//未选择文件
        			Toast.makeText(MainActivity.this, "未选择文件，请先选择需上传的文件！", Toast.LENGTH_SHORT).show();
        		}
            }  
        }); 
	}
	@SuppressWarnings("deprecation")
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (resultCode == Activity.RESULT_OK) {
			// Get the Uri of the selected file
			Uri uri = data.getData();
			filePath = new ftpFile().getRealPath(uri);
			selectedFile.setText(filePath);
			Log.d("filePath：", filePath);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	//启动新线程上传文件
	class fileUploadThread extends Thread {
		private File file;
		private long filesSize;
		private long uploadedFilesSize=0;
		//已上传的总byte数
 		private long lastTotalTxBytes=0;
 		private long lastTimeStamp = 0;
 		
		fileUploadThread(File file){
			this.file=file;
			//获取要上传文件的大小
			try{
				this.filesSize=getFilesSize(file);
			}
			catch(Exception e){
				e.printStackTrace();
			}
			lastTotalTxBytes=TrafficStats.getUidRxBytes(getApplicationInfo().uid)==TrafficStats.UNSUPPORTED?0:(TrafficStats.getTotalTxBytes()/1024);
			lastTimeStamp=System.currentTimeMillis();
		}
		//计算要上传文件的大小
		public long getFilesSize(File files) throws Exception { 
			long size = 0; 
			//判断文件是否问目录
			if (files.isDirectory()) {
				File filelist[] = files.listFiles(); 
				for (int i = 0; i < filelist.length; i++) { 
					//判断子文件是否问目录
					if (filelist[i].isDirectory()) {
						//子文件问目录则循环调用getFilesSize()函数
						size = size + getFilesSize(filelist[i]); 
					} else { 
						size = size + filelist[i].length(); 
					} 
				} 
			}else{
				size = files.length();
			}
			return size; 
		} 
        @Override  
        public void run() {  
            try {  
                //上传
                new FTPUpload().upload(file, "/f",new UploadProgressListener(){  
                    @Override  
                    public void onUploadProgress(String currentStep,long uploadSize,File uploadingFile) {  
                        // TODO Auto-generated method stub  
                        Log.d(TAG, currentStep);   
                        Message msg = new Message();
                        if(uploadingFile!=null){
	                        long fileSize = uploadingFile.length();                                       
	                        if(currentStep.equals(MainActivity.FTP_UPLOAD_SUCCESS)){  
	                        	uploadedFilesSize += fileSize;
	                            Log.d(TAG, "-----上传成功-----");  
	                        } else if(currentStep.equals(MainActivity.FTP_UPLOAD_LOADING)){
	                            float num = (float)uploadSize / (float)fileSize;  
	                            int result = (int)(num * 100);  
	                            Log.d(TAG, "-----已上传："+result + "%-----"); 
	                        }
	                        if(uploadedFilesSize<filesSize){
	                        	//测当前网速
	                        	//现在已上传的总byte数
	                        	long nowTotalTxBytes = TrafficStats.getUidTxBytes(getApplicationInfo().uid)==TrafficStats.UNSUPPORTED?0:(TrafficStats.getTotalTxBytes()/1024);
	                    		long nowTimeStamp = System.currentTimeMillis();
	                    		//当前的网速：一段时间内上传的总byte数除这段时间间隔（将毫秒转化成秒）
	                    		long networkSpeed = ((nowTotalTxBytes - lastTotalTxBytes) * 1000 / (nowTimeStamp - lastTimeStamp));
	                    		//更新时间戳和已上传的总byte数
	                    		lastTimeStamp = nowTimeStamp;
	                    		lastTotalTxBytes = nowTotalTxBytes;
	                        	Log.d(TAG, "filesSize is "+filesSize+"and uploadedFilesSize is "+uploadedFilesSize); 
	                        	Log.d(TAG, "uploadSize is "+uploadSize); 
	                        	Log.d(TAG, "progress is "+100*(uploadedFilesSize+uploadSize)/filesSize);
	                    		//发送message消息，更新界面
	                    		Bundle data = new Bundle();
	                        	msg.what=0;
	                        	//设置进度
	                        	data.putInt("progress", (int)(100*(uploadedFilesSize+uploadSize)/filesSize));
	                        	//设置当前上次的文件
	                        	data.putString("filePath", file.getAbsolutePath());
	                        	//设置当前的网速
	                        	data.putString("networkSpeed", networkSpeed+"kb/s");
	                        	msg.setData(data);
	                        }else{
	                        	msg.what = 1;
	                        }
                        }else{
                        	//连接成功显示进度条
                        	if(currentStep.equals(MainActivity.FTP_CONNECT_SUCCESS)){
                        		msg.what = 2;
                        	}
                        	//关闭连接隐藏进度条
                        	else if(currentStep.equals(MainActivity.FTP_DISCONNECT_SUCCESS)){
                        		msg.what = 3;
                        	}
                        }
                        handler.sendMessage(msg);
                    }                             
                });  
            } catch (IOException e) {  
                // TODO Auto-generated catch block  
                e.printStackTrace();  
            }  
        }  
    }
	
	//更新进度条
	Handler handler = new Handler() {  
        @Override  
        public void handleMessage(Message msg) { 
			Bundle bundle = msg.getData();
			//在进度条上面显示文件名
			fileName.setText(bundle.getString("filePath"));
        	switch(msg.what){
        		case 1:
        			progressBar.setProgress(100);
        			break;
        		//连接成功显示进度条
        		case 2:
        			progressBar.setVisibility(View.VISIBLE);
        			progressBar.setProgress(0);
            		break;
            	//关闭连接隐藏进度条
        		case 3:
        			progressBar.setProgress(100);
        			progressBar.setVisibility(View.GONE);
        			fileName.setText("");
        			uploadedPercent.setText("");
        			uploadedSpeed.setText("");
        			selectedFile.setText("");
        			//提示下载完成
        			Toast.makeText(getApplicationContext(), "上传已完成！", Toast.LENGTH_SHORT).show();
            		break;
        		default:
        			progressBar.setProgress(bundle.getInt("progress"));
        			uploadedPercent.setText("   "+bundle.getInt("progress")+"%");
        			uploadedSpeed.setText(bundle.getString("networkSpeed"));
                	Log.d(TAG, "progress is "+bundle.getInt("progress")); 
        			break;
        	}
		}  
	   
	};

}
