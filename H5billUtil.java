package com.tchzt.h5util;
import com.tchzt.base.po.*;
import com.tchzt.base.util.ConfMap;
import com.tchzt.cdbank.base.cachet.action.vo.CachetQuery;
import com.tchzt.cdbank.base.stamp.action.vo.StampQuery;
import com.tchzt.cdbank.cards.action.web.CardsQuery;
import com.tchzt.cdbank.client.accounts.action.vo.AccountsQuery;
import com.tchzt.h5util.form.AutoIdentifyRequestForm;
import com.tchzt.h5util.form.IdentifyForm;
import com.tchzt.h5util.form.InitForm;
import com.tchzt.h5util.form.MapForm;
import com.tchzt.util.Base64Utils;
import com.tchzt.util.BlobUtil;
import com.tchzt.util.Document4jUtils;
import com.tchzt.util.StaticVariable;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.codehaus.xfire.client.Client;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.net.URL;
import java.sql.Blob;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName H5billUtil
 * @Description TODO
 * @Author whl
 * @Date 2021/7/23 17:41
 * Version 1.0
 **/
public class H5billUtil {
    public String initParam(Map<String, Object> pMap) throws Exception {
        
        Clerks clk = (Clerks) pMap.get("clerks");
        String taskId = (String) pMap.get("taskId");
        List<Vchtypes> vlist = (List) pMap.get("vlist");
        List<Currency> cList = (List) pMap.get("cList");
        String viewlogs = (String) pMap.get("viewlogs");
        String accountNo = (String) pMap.get("accountNo");
        String voucherNo = (String) pMap.get("voucherNo");
        String voucherDate = (String) pMap.get("voucherDate");
        String amount = (String) pMap.get("amount");
        String sealType = (String) pMap.get("sealType");
        String voucherType = (String) pMap.get("voucherType");
        String sysCode = (String) pMap.get("sysCode");
        
        String scanUrl= ConfMap.confmap.get("scanUrl");
        InitForm initForm = new InitForm();
        if (vlist != null) {
            List<MapForm> bfList = new ArrayList<MapForm>();
            MapForm billForm = null;
            for (Vchtypes vch : vlist) {
                billForm = new MapForm();
                billForm.setCode(vch.getVchtype());
                billForm.setName(vch.getVchdesc());
                bfList.add(billForm);
            }
            initForm.setVoucherTypes(bfList);
        }
        if (cList != null) {
            List<MapForm> currencyformList = new ArrayList<MapForm>();
            MapForm currencyForm = null;
            for (Currency currency : cList) {
                currencyForm = new MapForm();
                currencyForm.setCode(currency.getCurcode());
                currencyForm.setName(currency.getCurdesc());
                currencyformList.add(currencyForm);
            }
            initForm.setCurrencyTypes(currencyformList);
        }
        List<MapForm> stampTypeList = new ArrayList<MapForm>();
        MapForm stampTypeForm = new MapForm();
        stampTypeForm.setCode("1");
        stampTypeForm.setName("印鉴");
        stampTypeList.add(stampTypeForm);
        stampTypeForm = new MapForm();
        stampTypeForm.setCode("2");
        stampTypeForm.setName("公章");
        stampTypeList.add(stampTypeForm);
//        stampTypeForm = new MapForm();
//        stampTypeForm.setCode("4");
//        stampTypeForm.setName("法人章");
//        stampTypeList.add(stampTypeForm);
        initForm.setSealTypes(stampTypeList);
        
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(ScanFields.SCANMETHOD, "02-1");
        paramMap.put(ScanFields.USERNO, clk.getClkno());
        paramMap.put(ScanFields.ORGCODE, clk.getDepno());
        paramMap.put(ScanFields.VOUCHERTYPES, JSONArray.fromObject(initForm.getVoucherTypes()).toString());
        paramMap.put(ScanFields.CURRENCYTYPES, JSONArray.fromObject(initForm.getCurrencyTypes()).toString());
        paramMap.put(ScanFields.SEALTYPES, JSONArray.fromObject(initForm.getSealTypes()).toString());
        paramMap.put(ScanFields.ISHIGHEQUIP, "1"); //不支持高拍仪
        paramMap.put(ScanFields.AUTOIDEN, "0"); // 扫描完 不自动验印
        paramMap.put(ScanFields.SHOWCOINCIDENT, "1"); // 不显示重合度
        //paramMap.put("showSetDpi", "0");//是否允许设置dpi 0不允许
        paramMap.put(ScanFields.SHOWARTIFICIALMATCH, "1"); // 不显示人工匹配按钮
        paramMap.put(ScanFields.ISSETUP, "0");//扫描页面，显示设置按钮
        paramMap.put(ScanFields.SHOWSMALLCODE, "1"); //0显示小码 1不显示
        JSONObject customObject = new JSONObject(); //定制化参数custom
        customObject.put(ScanFields.SMALLCODEEDIT,"0"); // 干预页面小码可编辑 1可以 0不可以
        customObject.put(ScanFields.ISSHOWREASONBTN,"0"); // 所有渠道干预 不显示“原因”按钮

        //人工干预页面
        if (taskId != null) {
            paramMap.put("taskId", taskId);
            paramMap.put(ScanFields.COMMITPROMPT, "人工验印确认");
            paramMap.put(ScanFields.COMMITMETHOD, "1");
            paramMap.put(ScanFields.SHOWAMOUNT, "0");
            paramMap.put(ScanFields.ISINTERVENE, "0");

            //如果查看验印日志时候需要隐藏干预按钮
            if(viewlogs != null && !"".equals(viewlogs)){
                paramMap.put(ScanFields.ISINTERVENE, "5");
                paramMap.put(ScanFields.SHOWVIEW, "3"); //
                customObject.put(ScanFields.ISSHOWREASONBTN,"1"); // 如果查看日志时 显示“原因”按钮
            }
            //有taskId时，自动提交
            customObject.put(ScanFields.ISSYNCCOMMIT,"0"); // 0 不自动提交

            //验印
        } else if (sysCode != null && "3009".equals(sysCode)) {
            //外围系统调用H5验印 (国库)
            paramMap.put("accountNo", accountNo);
            paramMap.put("voucherNo", voucherNo);
            paramMap.put("voucherDate", voucherDate);
            paramMap.put("amount", amount);
            paramMap.put("currency", "CNY");
            paramMap.put("sealType", sealType);
            paramMap.put("isModify", "0"); //设置页面输入框不可编辑
            paramMap.put("voucherType", voucherType);
            paramMap.put(ScanFields.COMMITMETHOD, "1");  //接口提交
            paramMap.put(ScanFields.ISINTERVENE, "3");  //隐藏页面提交按钮
            customObject.put("sysCode", "3009");
        } else {
            //本系统页面验印
            paramMap.put(ScanFields.COMMITPROMPT, "提交");
            paramMap.put(ScanFields.COMMITMETHOD, "0");
            paramMap.put(ScanFields.SHOWAMOUNT, "0");
            customObject.put("sysCode", "3006");
        }
        paramMap.put("custom", customObject);
        //获取扫描url地址
        String res = PostUtil.postRequest(paramMap, scanUrl);
        return res;
    }

    public Map<String, String> getStampByCondition(IdentifyForm identifyForm) {
        Map<String, String> resultMap = new HashMap<String, String>();
        // 调用webservice传递账号，凭证号，凭证类型和凭证日期获取印鉴库
        Map<String, Object> stampMap = new HashMap<String, Object>();
        try {
            if (identifyForm.getAccountNo() != null
                    && identifyForm.getAccountNo().trim().length() > 0) {
                stampMap.put("accno", identifyForm.getAccountNo().trim());
            }else {
                resultMap.put("result", "4");
                resultMap.put("resultstr", "请输入账号");
                return resultMap;
            }
            if (identifyForm.getStamplibNo() != 0) {
                stampMap.put("stamplibNo", identifyForm.getStamplibNo());
            }
            if (identifyForm.getCachetlibNo() != 0) {
                stampMap.put("cachetlibNo", identifyForm.getCachetlibNo());
            }
            if (identifyForm.getTaskCode() != null) {
                stampMap.put("taskCode", identifyForm.getTaskCode());
            }
            if (identifyForm.getVoucherNo() != null
                    && identifyForm.getVoucherNo().trim().length() > 0) {
                stampMap.put("vchno", identifyForm.getVoucherNo().trim());
            }else {

//                resultMap.put("result", "4");
//                resultMap.put("resultstr", "请输入凭证号码");
//                return resultMap;
                stampMap.put("vchno", "");
            }
            if (identifyForm.getVoucherDate() != null
                    && identifyForm.getVoucherDate().trim().length() > 0) {
                stampMap.put("vchdate", identifyForm.getVoucherDate().trim());
            }
            if (identifyForm.getVoucherType() != null
                    && identifyForm.getVoucherType().trim().length() > 0) {
                stampMap.put("vchtype", identifyForm.getVoucherType());
            }else {
                resultMap.put("result", "4");
                resultMap.put("resultstr", "请选择凭证类型");
                return resultMap;
            }
            if (identifyForm.getSealType() != null) {
                stampMap.put("idtype", identifyForm.getSealType());
            }
            if (identifyForm.getSystemCode() != null) {
                stampMap.put("systemCode", identifyForm.getSystemCode());
            }

            String stampUrl=ConfMap.confmap.get("p_ws_url");
            System.out.println("stampUrl==="+stampUrl);
            //验印时先校验账户状态能否验印
            //传3007去印鉴是全部放行的【20230711信创改造需求：新票据验印，账户状态除销户外，其他异常状态均可验印成功】
            if ("YANYIN".equals(identifyForm.getTaskCode())) {
                Client client2 = new Client(new URL(stampUrl));
                Object[] results2 = client2.invoke("queryByAccno", new Object[]{Document4jUtils.getXmlStr2(stampMap)});
                Map<String, Object> pmap = Document4jUtils.getXmlSubNoRes((String) results2[0], "xfaceTradeDTO");
                String resultCode = (String) pmap.get("resultCode");
                String resultstr = (String) pmap.get("resultStr");
                if (!"0000".equals(resultCode)) {
                    resultMap.put("result", "4");
                    resultMap.put("resultstr", resultstr);
                    return resultMap;
                }
            }
            Client client = new Client(new URL(stampUrl));
            Object[] results = client.invoke("getStampByConditionByH5",
                    new Object[] { Document4jUtils.getXmlStr(stampMap) });
            Document doc = DocumentHelper.parseText((String) results[0]);
            Element root = doc.getRootElement();
            Element resultEl = root.element("result");
            Element resultMesEl = root.element("resultstr");
            Element infoEl = root.element("info");
            String resultStr = resultEl.getText();
            String resultMesStr = resultMesEl.getText();

            resultMap.put("result", resultStr);
            resultMap.put("resultstr", resultMesStr);
            String infoStr = null;
            //1表示获取印鉴成功
            if ("1".equals(resultStr)) {
                if(infoEl != null){
                    infoStr = infoEl.getText();
                    JSONObject jsonStr =JSONObject.fromObject(infoStr);
                    String sealCkeckId=jsonStr.getString("sealCkeckId");
                    String cardNo=jsonStr.getString("cardNo");
                    String busiSerialNo=jsonStr.getString("busiSerialNo");
                    String accDepNo=jsonStr.getString("accDepNo");
                    resultMap.put("busiSerialNo", busiSerialNo);
                    resultMap.put("sealCkeckId", sealCkeckId);
                    resultMap.put("cardNo", cardNo);
                    resultMap.put("accDepNo", accDepNo);
                }
                Element stampimageEl = root.element("stampimage");
                if (stampimageEl != null) {
                    if ("0".equals(identifyForm.getSealType())) {
                        resultMap.put("stampimage", stampimageEl.getText());
                    }else if ("1".equals(identifyForm.getSealType())) {
                        resultMap.put("cachetimage", stampimageEl.getText());
                    }
                }
                Element stampEL = root.element("stamp");
                if (stampEL != null){
                    Element busiSerialNo = root.element("busiSerialNo");
                    if (busiSerialNo != null) {
                        resultMap.put("busiSerialNo", busiSerialNo.getText());
                    }
                    resultMap.put("stamp", stampEL.getText());
                }
                Element companynameEl = root.element("companyname");
                if (companynameEl != null){
                    resultMap.put("companyname", companynameEl.getText());
                }

            }
        }catch (Exception e) {
            e.printStackTrace();
            resultMap.put("result", "4");
            resultMap.put("resultstr", "获取预留印鉴参数缺失");
        }
        return resultMap;
    }
    public String getSocketResultByJson(String json) {
        String result = null;
        String socketIp=ConfMap.confmap.get("sidrvNode_IP");
        Integer socketPort=Integer.parseInt(ConfMap.confmap.get("sidrvNode_Port"));
        try {
            SocketUtils client = new SocketUtils(socketIp,socketPort);
            client.sendRequest(json);
            result = client.getResponse();
            //SocketUtils2 client2 = new SocketUtils2();
            // result= client2.sendMsgToBackNodeByJsonBuffer(json.trim());
        } catch (Exception e) {
            e.printStackTrace();
            result = "-1";
        }
        return result;
    }
    public String getAutoIdentify(AutoIdentifyRequestForm form) {
        String socketIp=ConfMap.confmap.get("sidrvNode_IP");
        Integer socketPort=Integer.parseInt(ConfMap.confmap.get("sidrvNode_Port"));
        String result = null;
        try {
            form.setTaskCode("AUTO_IDEN");
            form.setYwType("YANYIN");
            form.setShowEpArea(1);
            form.setStampGroup(1);
            SocketUtils client = new SocketUtils(socketIp,socketPort);
            client.sendRequest(JSONObject.fromObject(form).toString());
            result = client.getResponse();
        } catch (Exception e) {
            e.printStackTrace();
            result = "-1";
        }
        return result;
    }
    public IdrlogsTemp cpIdrlogs(Idrlogs idr){
        IdrlogsTemp  idrTemp= new IdrlogsTemp(idr.getIdx(), idr.getEventid(), idr.getAccno(), idr.getVchno(),
                idr.getVchtype(), idr.getVchdate(), idr.getAmount(), idr.getClkno(),
                idr.getWarclk(),  idr.getDepno(), idr.getOpttime(), idr.getSysidres(),
                idr.getSysstpres(), idr.getManidres(),idr.getManstpres(),idr.getFname(),
                idr.getIdrname(), idr.getMemo());
        idrTemp.setBusitype(idr.getBusitype());
        idrTemp.setBusiSerialNo(idr.getBusiSerialNo());
        idrTemp.setCardNo(idr.getCardNo());
        idrTemp.setSealCkeckId(idr.getSealCkeckId());
        idrTemp.setSupervisor(idr.getSupervisor());
        idrTemp.setIdres(idr.getIdres());
        idrTemp.setIdresStr(idr.getIdresStr());
        idrTemp.setSupervisortime(idr.getSupervisortime());
        idrTemp.setFcardimg(idr.getFcardimg());
        idrTemp.setBcardimg(idr.getBcardimg());
        idrTemp.setIdresByte(idr.getIdresByte());
        idrTemp.setSupervisorcachet(idr.getSupervisorcachet());
        idrTemp.setSupervisorstamp(idr.getSupervisorstamp());
        return idrTemp;
    }
    /**
    * @Author whl
    * @Description TODO 调取batchManager的webserivce接口queryByAccno 查询账户建模状态
    * @Date 2022/12/12 16:57
    * @Param [identifyForm]
    * @Return java.util.Map<java.lang.String,java.lang.String>
    **/
    public JSONObject queryByAccnoFormbatchManager(Map<String, Object> pamMap) {
        String stampUrl = ConfMap.confmap.get("p_ws_url");
        JSONObject customObject = new JSONObject();
        try {
            Client client = new Client(new URL(stampUrl));
            String pamXml= Document4jUtils.mapToXml(pamMap,"queryByAccno");
            String reqStr= StaticVariable.queryByaccnoXML1+ pamXml + StaticVariable.queryByaccnoXML2;
            Object[] results = client.invoke("queryByAccno",new Object[]{reqStr});
            String resultStr = (String) results[0];

            String resultState=resultStr.substring(resultStr.indexOf("<resultCode>")+12,resultStr.indexOf("</resultCode>"));
            String resultMsg= resultStr.substring(resultStr.indexOf("<resultStr>")+11, resultStr.indexOf("</resultStr>"));

            customObject.put("resultState", resultState);
            customObject.put("resultMsg", resultMsg);

        } catch (Exception e) {
            customObject.put("resultState", "1111");
            customObject.put("resultMsg", "调取batchManager的webserivce接口queryByAccno异常");
            e.printStackTrace();
        }
        customObject.put("reqType", "");
        customObject.put("result", "");
        List resultList=new ArrayList();
        customObject.put("resultList", resultList);
        return customObject;
    }
}
