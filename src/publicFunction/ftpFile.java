package publicFunction;

import java.net.URLDecoder;
import android.net.Uri;
import android.util.Log;

public class ftpFile {
	public ftpFile(){}
	/** 
     * 编码 utf-8编码解决中文和空格问题
     * @throws Exception 
     */  
	public String getRealPath(Uri fileUrl){
		String fileName = null;
		Uri filePathUri = fileUrl;
		if(fileUrl!= null){
		    if (fileUrl.getScheme().compareTo("file")==0) {         //file:///开头的uri
		       try{
		    	   //使用utf-8编码解决中文和空格问题
		    	   fileName = URLDecoder.decode(filePathUri.toString(),"utf-8");
			       fileName = fileName.replace("file://", ""); //去掉file://
		       }
		       catch(Exception e){
		    	   e.printStackTrace();
		       }
			   Log.d("fileName：", fileName);
		   }
		}
		return fileName;
	}
}
