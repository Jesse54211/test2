package com.tchzt.util;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import com.sunyard.client.SunEcmClientApi;

import com.sunyard.client.bean.ClientBatchBean;
import com.sunyard.client.bean.ClientBatchFileBean;
import com.sunyard.client.bean.ClientBatchIndexBean;
import com.sunyard.client.bean.ClientFileBean;
import com.sunyard.client.impl.SunEcmClientSocketApiImpl;
import com.tchzt.base.po.ModelCode;
import com.tchzt.base.util.ConfMap;
import com.tchzt.base.util.ModelCodeMap;
import com.tchzt.cdbank.base.modelcode.business.ModelCodeBusinessIf;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 客户端使用示例
 *
 * @author Warren
 *
 */
public class EcmClient {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
//	private String modelCode = "JZZY";
//	String filePartName = "JZZY_PART";
	private String ecmIP=ConfMap.confmap.get("ecmIP");
	private String ecmPort=ConfMap.confmap.get("ecmPort");
	private String ecmUn=ConfMap.confmap.get("ecmUn");
	private String ecmPd=ConfMap.confmap.get("ecmPd");
	private String fullFilePath=ConfMap.confmap.get("fullFilePath");


	/**
	 * 上传接口调用
	 */
	private String upload(String fileName, Integer eventId, String tranDate) {
		SunEcmClientApi api = new SunEcmClientSocketApiImpl(ecmIP,Integer.parseInt(ecmPort));
		ClientBatchBean batchBean = new ClientBatchBean();
		batchBean.setUser(ecmUn);
		batchBean.setPassWord(ecmPd);
		batchBean.setBreakPoint(false);
		batchBean.setModelCode(ModelCodeMap.modelmap.get(eventId).getModelcode());
		batchBean.setOwnMD5(false);


		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
		String idx = sdf.format(date); //BUSI_SERIAL_NO
		//文件格式
		String fileFormat = fileName.substring(fileName.lastIndexOf(".")+1);

		// ---------------设置索引对象----------------
		ClientBatchIndexBean batchIndexBean = new ClientBatchIndexBean();
		batchIndexBean.addCustomMap("AMOUNT", "1");
		batchIndexBean.addCustomMap("CREATEDATE", tranDate);
		batchIndexBean.addCustomMap("BUSI_SERIAL_NO", idx);
		batchIndexBean.setVersion("1");

		// ---------------设置文档部件信息----------------
		ClientBatchFileBean batchFileBean = new ClientBatchFileBean();
		batchFileBean.setFilePartName(ModelCodeMap.modelmap.get(eventId).getFilepartname());
		// ---------------添加文件-----------------------
		ClientFileBean bean = new ClientFileBean();
		bean.setFileFormat(fileFormat);
		bean.setFileName(fileName);
		bean.setFilesize("145896");
		batchFileBean.addFile(bean);
		String shouname = fileName.substring(fileName.lastIndexOf("/")+1);
		bean.addOtherAtt("SHOWNAME", shouname);

		// -------------批次添加索引对象，文档部件对象-----
		batchBean.addDocument_Object(batchFileBean);
		batchBean.setIndex_Object(batchIndexBean);
		String massage = "";
		try {
			massage = api.upload(batchBean, "");
			System.out.println("影像平台文件上传返回信息>>:" + massage);
			//文件上传完成之后，删除本地图片
			deleteFile(fileName);
		}
		catch (Exception e) {
			logger.error("程序异常了!", e);
		}
		return massage;
	}
	/**
	 * @Title: 从影像平台查询文件(图片、.DZYY重合图等),返回文件信息
	 * @Description: TODO
	 * @param contentid
	 * @return String
	 * @throws @CreateTime: 2018-2-7 下午06:01:05
	 * @see
	 */
	public String queryFile(String contentid, String admsdate,int eventid) {
		String resultMsg = null;
		SunEcmClientApi api = null;
		ClientBatchBean clientBatchBean = null;
		ModelCode modelCode = ModelCodeMap.modelmap.get(eventid);
		try {
			api = new SunEcmClientSocketApiImpl(ecmIP, Integer.parseInt(ecmPort));
			clientBatchBean = new ClientBatchBean();
			clientBatchBean.setModelCode(modelCode.getModelcode());
			clientBatchBean.setUser(ecmUn);
			clientBatchBean.setPassWord(ecmPd);
			clientBatchBean.setDownLoad(false);
			clientBatchBean.getIndex_Object().setContentID(contentid);
			clientBatchBean.getIndex_Object().addCustomMap("CREATEDATE",admsdate);
			resultMsg = api.queryBatch(clientBatchBean, "");
			System.out.println("影像查询参数>>:" + "contentid:" + contentid + ",admsdate:" + admsdate + ",eventid:" + eventid);
			System.out.println("影像查询返回信息>>:" + resultMsg);
			
		}
		catch (Exception e) {
			logger.error("程序异常了!", e);
		}
		return resultMsg;
	}

	/**
	 * @Title: saveImage保存图片到本地 二进制文件
	 * @Description: TODO
	 * @param retbb
	 * @return String
	 * @throws @CreateTime: 2018-2-7 下午06:01:05
	 * @see
	 */
	public String saveImage(byte[] retbb) throws IOException {
		String filename =fullFilePath + "/" + UUIDUtil.getUuid() + ".jpg";
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(filename);
			fos.write(retbb, 0, retbb.length);
			fos.flush();
			System.out.println("图片转化完成");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (fos != null) {
					fos.close();
				}
			}
			catch (Exception e2) {
				e2.printStackTrace();
			}

		}
		return filename;
	}

	/**
	 * 从本地删除单个文件
	 * @param   sPath    被删除文件的文件名
	 * @return 单个文件删除成功返回true，否则返回false
	 */
	public boolean deleteFile(String sPath) {
		Boolean flag = false;
		File file = new File(sPath);
		// 路径为文件且不为空则进行删除
		 if (file.isFile() && file.exists()) {
			file.delete();
			flag = true;
		 }
		 return flag;
		}


	/**
	 * 上传文件直接返回影像平台batchId，上传失败则返回""
	 * tranDate为查询时候需要的日期，传入则使用，传空则取当前
	 * @param filename
	 * @return batchId
	 */
	public String  uploadFile(String filename,Integer eventId, String tranDate) {
		File file = new File(filename);

		if("".equals(tranDate)){
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			tranDate = sdf.format(new Date());
		}

		if (file.exists()) {
			String batchId = upload(filename,eventId,tranDate);
			if (batchId != null && !batchId.equals("")) {
				String[] ressxy = batchId.split(">>");
				if (ressxy.length > 1) {
					return ressxy[1];
				}
			}
		}

		return "";
	}

	/**
	 * 验印重合图整个报文转文件
	 * @param retbb
	 * @return 文件名
	 * @throws IOException
	 */
	public String saveIdresFile(byte[] retbb) throws IOException {
		String filename = fullFilePath + "/" + UUIDUtil.getUuid() + ".dzyy";
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(filename);
			fos.write(retbb, 0, retbb.length);
			fos.flush();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (fos != null) {
					fos.close();
				}
			}
			catch (Exception e2) {
				e2.printStackTrace();
			}

		}
		return filename;
	}
	/**
	 * 从ecm删除一个文件
	 * @param contentID
	 * @param
	 * @return massage
	 */
	public String deleteFileFromECM(String contentID,String admsdate,int eventid) {
		SunEcmClientApi api = new SunEcmClientSocketApiImpl(ecmIP,Integer.parseInt(ecmPort));
		ClientBatchBean batchBean = new ClientBatchBean();
		batchBean.setUser(ecmUn);
		batchBean.setPassWord(ecmPd);
		batchBean.setBreakPoint(false);
		batchBean.setModelCode(ModelCodeMap.modelmap.get(eventid).getModelcode());
		batchBean.setOwnMD5(false);
		batchBean.getIndex_Object().setContentID(contentID);
		batchBean.getIndex_Object().addCustomMap("CREATEDATE",admsdate);

		String massage = "";
		try {
			massage = api.delete(batchBean, "");
			System.out.println("影像平台删除参数>>:" + "contentID:" + contentID + ",admsdate:" + admsdate + ",eventid" + eventid);
			System.out.println("影像平台删除返回信息>>:" + massage);
		}
		catch (Exception e) {
			logger.error("程序异常了!", e);
		}
		return massage;
	}

	/**
	 * @param idrNameAndFileNo 格式 xx##xx 传入则获取指定fileNo的文件
	 * @param ecmRes 影像平台返回的报文
	 * @return 文件URL
	 * @throws DocumentException
	 */
	public String getFileUrlFromXml(String idrNameAndFileNo, String ecmRes) throws DocumentException {
		String fileNo = ""; //文件号
		if(idrNameAndFileNo.contains("##")){
			fileNo = idrNameAndFileNo.split("##")[1];
		}
		String fileUrl = "";
		String resxml = ecmRes.substring(ecmRes.indexOf("<files"), ecmRes.indexOf("</files>") + 8);
		//Filebean不止一个 遍历
		Document doc = null;
		doc = DocumentHelper.parseText(resxml);
		Element rootElt = doc.getRootElement(); // 获取根节点
		Iterator iter = rootElt.elementIterator(); // 遍历根节点
		while (iter.hasNext()) {
			Element itemEle = (Element) iter.next(); //itemEle就是FileBean
			String format = itemEle.attributeValue("FILE_FORMAT").trim();
			String url = itemEle.attributeValue("URL").trim();
			String file_no = itemEle.attributeValue("FILE_NO").trim();
			ArrayList filterFormatList = new ArrayList<>(Arrays.asList("xml","db"));
			if ( !filterFormatList.contains(format) ) {
				if(!"".equals(fileNo) && fileNo.equals(file_no)) {
					fileUrl = url;
					return fileUrl;
				}else {
					//保证idrNameAndFileNo传不传，都能返回图片，如果遍历不到，就返回最后一张
					fileUrl = url;
				}
			}
		}
		System.out.println("解析影像平台报文返回获得的文件URL>>fileUrl:" + fileUrl + "fileNo:" + fileNo);
		return fileUrl;
	}

	/**
	 * 根据影像平台返回的Url，在线读取文件内容
	 * @param url
	 * @return base64Str
	 */
	public String getIdresByUrl(String url){
		// 直接根据url去获取
		BufferedReader br = null;
		String base64Str = "";
		try {
			URL u = new URL(url);
			URLConnection con = u.openConnection();
			br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String read = "";
			while ((read = br.readLine()) != null) {
				//就只有一行，只需要读一行
				base64Str = read;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return base64Str;
	}


}
