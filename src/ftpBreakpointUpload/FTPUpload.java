package ftpBreakpointUpload;

import org.apache.commons.net.ftp.FTPClient;  
import org.apache.commons.net.ftp.FTPClientConfig;  
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class FTPUpload {
	// 服务器名. 
    private String hostName;  
    // 端口号 
    private int serverPort;  
    // 用户名. 
    private String userName;  
    // 密码. 
    private String password;
    private OutputStream outputStream;
    // FTP连接. 
    private static FTPClient ftpClient = new FTPClient();
    
    public FTPUpload() {  
    	//ftp服务器地址、端口号、用户名和密码
        /*this.hostName = "192.168.1.107";  
        this.serverPort = 21;  
        this.userName = "FTP001";  
        this.password = "123456";*/  
    	this.hostName = "192.168.1.101";  
        this.serverPort = 21;  
        this.userName = "root";  
        this.password = "root";
    }  

    /** 
     * 上传单个文件. 
     * @param localFile 本地文件 
     * @param remotePath FTP目录 
     * @param listener 监听器 
     * @throws IOException 
     */  
    public void upload(File singleFile, String remotePath,UploadProgressListener listener) throws IOException {  
        // 上传之前初始化  
    	this.uploadBeforeOperate(remotePath, listener);  
        uploadDirFiles(singleFile.getAbsolutePath(), remotePath, listener);
        // 上传完成之后,如果outputStream不为null则关闭输出流
        if(outputStream!=null)
        	outputStream.close();
        // 上传完成之后关闭连接  
        this.uploadAfterOperate(listener);  
    }  

	/**
	 * 判断指定文件中是否存在相同名称的文件
	 * 
	 * @param remotePath :FTP上的远程目录
	 * @param fileName:文件名称
	 * @return boolean :判断是否存在相同名称
	 * 
	 */
	//上传整个目录到FTP的指定目录中
	public void uploadDirFiles(String dirPath,String toRemotePath,UploadProgressListener listener) throws IOException{
		if (dirPath!=null && !dirPath.equals("")) {
			//建立上传目录的File对象
			File dirFile = new File(dirPath);
			//判断File对象是否为目录类型
			if (dirFile.isDirectory()) {
				//如果是目录类型。
				//在FTP上创建一个和File对象文件相同名称的文件夹
				ftpClient.makeDirectory(toRemotePath+"/"+dirFile.getName());
				//获得File对象中包含的子目录数组
				File[] subFiles = dirFile.listFiles();
				//路径
				String path = toRemotePath+"/"+dirFile.getName();
				System.out.println(path);
				//判断数组是否为空
				if (subFiles!=null && subFiles.length>0) {
					//遍历整个File数组
					for (int i = 0; i < subFiles.length; i++) {
						//判断是否为目录类型
						if (subFiles[i].isDirectory()) {
							//递归调用自身方法，进行到下一层级的目录循环
							uploadDirFiles(subFiles[i].getAbsolutePath(), path, listener);
						} else {
							File localFile = new File(dirPath+"/"+subFiles[i].getName());
							uploadingFile(localFile, path,listener);
						}
					}
				}
			} else {
				File localFile = new File(dirPath);
				uploadingFile(localFile, toRemotePath, listener);
			}
		}
	}

	/** 
     * 上传单个文件. 
     * @param localFile  本地文件 
     * @return true上传成功, false上传失败 
     * @throws IOException 
     */  
    private boolean uploadingFile(File localFile, String remotePath, UploadProgressListener listener){  
        boolean flag = true;  
        Long localFileLength = localFile.length();
        Long serverFileLength = 0L;
        String localFileName = localFile.getName();
        try{
            ftpClient.changeWorkingDirectory(remotePath);
	        FTPFile[] files = ftpClient.listFiles(localFileName);
	        if(files.length>0&&files[0].getSize()>0){
	        	serverFileLength = files[0].getSize();
	        	if(serverFileLength<localFileLength){
		        	RandomAccessFile randomLocalFile = new RandomAccessFile(localFile, "r");
		        	randomLocalFile.seek(serverFileLength);
		        	outputStream = ftpClient.appendFileStream(new String(localFileName.getBytes("utf-8"), "utf-8"));
		        	ftpClient.setRestartOffset(serverFileLength);
		        	byte[] bytes = new byte[1024];
		    		int c;
		    		while ((c = randomLocalFile.read(bytes)) != -1) {
		    			serverFileLength += c;
		    			listener.onUploadProgress(MainActivity.FTP_UPLOAD_LOADING, serverFileLength, localFile); 
		    			outputStream.write(bytes, 0, c);
		    		}
		    		outputStream.flush();
		    		randomLocalFile.close();
		    		//out.close();
	        	}
	        }else{
	        	/*// 不带进度的方式  
	            InputStream inputStream = new FileInputStream(localFile);  
	            flag = ftpClient.storeFile(localFile.getName(), inputStream);  
	            inputStream.close();*/  
	        	
	        	// 带有进度的方式  
	            BufferedInputStream buffIn = new BufferedInputStream(new FileInputStream(localFile));  
	            ProgressInputStream progressInput = new ProgressInputStream(buffIn, listener, localFile); 
	            flag = ftpClient.storeFile(localFile.getName(), progressInput);  
	            buffIn.close(); 
	        }
        }catch(Exception e){
        	e.printStackTrace();
        }
        listener.onUploadProgress(MainActivity.FTP_UPLOAD_SUCCESS, 0, localFile); 
        return flag;  
    }  
      
    /** 
     * 上传文件之前初始化相关参数 
     * @param remotePath  FTP目录 
     * @param listener  监听器 
     * @throws IOException 
     */  
    private void uploadBeforeOperate(String remotePath, UploadProgressListener listener) throws IOException {  
        // 打开FTP服务  
        try {  
            this.openConnect();  
            listener.onUploadProgress(MainActivity.FTP_CONNECT_SUCCESS, 0, null);  
        } catch (IOException e1) {  
            e1.printStackTrace();  
            listener.onUploadProgress(MainActivity.FTP_CONNECT_FAIL, 0, null);  
            return;  
        }  
        // 设置模式  
        ftpClient.setFileTransferMode(org.apache.commons.net.ftp.FTP.STREAM_TRANSFER_MODE);  
        // FTP下创建文件夹  
        ftpClient.makeDirectory(remotePath);  
        // 改变FTP目录  
        ftpClient.changeWorkingDirectory(remotePath);  
    }   
    
    /** 
     * 打开FTP服务. 
     * @throws IOException 
     */  
    public void openConnect() throws IOException {  
        // 中文转码  
        ftpClient.setControlEncoding("UTF-8");  
        // 服务器响应值  
        int reply; 
        // 连接至服务器  
        ftpClient.connect(hostName, serverPort);  
        // 获取响应值  
        reply = ftpClient.getReplyCode();  
        if (!FTPReply.isPositiveCompletion(reply)) {  
            // 断开连接  
            ftpClient.disconnect();  
            throw new IOException("connect fail: " + reply);  
        }  
        // 登录到服务器  
        ftpClient.login(userName, password);  
        // 获取响应值  
        reply = ftpClient.getReplyCode();  
        if (!FTPReply.isPositiveCompletion(reply)) {  
            // 断开连接  
            ftpClient.disconnect();  
            throw new IOException("connect fail: " + reply);  
        } else {  
            // 获取登录信息  
            FTPClientConfig config = new FTPClientConfig(ftpClient.getSystemType().split(" ")[0]);  
            config.setServerLanguageCode("zh");  
            ftpClient.configure(config);  
            // 使用被动模式设为默认  
            ftpClient.enterLocalPassiveMode();  
            // 二进制文件支持  
            ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);  
        }  
    }  
  
    /** 
     * 上传完成之后关闭连接 
     * @param listener 
     * @throws IOException 
     */  
    private void uploadAfterOperate(UploadProgressListener listener) throws IOException {  
        this.closeConnect();  
        listener.onUploadProgress(MainActivity.FTP_DISCONNECT_SUCCESS, 0, null);  
    }
    /** 
     * 关闭FTP服务. 
     *  
     * @throws IOException 
     */  
    public void closeConnect() throws IOException {  
        if (ftpClient != null) {  
            // 退出FTP  
            ftpClient.logout();  
            // 断开连接  
            ftpClient.disconnect();  
        }  
    } 
    /* 
     * 上传进度监听 
     */  
    public interface UploadProgressListener {  
        public void onUploadProgress(String currentStep, long uploadSize, File file);  
    }  
}
