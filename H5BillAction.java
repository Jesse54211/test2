package com.tchzt.cdbank.identify.bill.action;

import com.google.gson.Gson;
import com.opensymphony.xwork2.ActionContext;
import com.tchzt.base.po.*;
import com.tchzt.base.po.Currency;
import com.tchzt.base.struts2base.BaseActionCallUtil;
import com.tchzt.cdbank.base.action.BaseAction;
import com.tchzt.cdbank.base.currency.business.CurrencyBusinessIf;
import com.tchzt.cdbank.base.modelcode.business.ModelCodeBusinessIf;
import com.tchzt.cdbank.config.vchtypes.business.VchtypesBusinessIf;
import com.tchzt.cdbank.statistics.idrlogs.business.IdrlogsBusinessIf;
import com.tchzt.cdbank.system.clerks.business.ClerksBusinessIf;
import com.tchzt.h5util.H5billUtil;
import com.tchzt.h5util.form.*;
import com.tchzt.util.*;
import com.tchzt.util.XskyClient.XskyClient;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings({"serial", "unused", "static-access"})
public class H5BillAction extends BaseAction {
    @Autowired(required = false)
    @Qualifier("currencyBusinessImpl")
    private CurrencyBusinessIf currencyBusiness;

    @Autowired(required = false)
    @Qualifier("idrlogsBusinessImpl")
    private IdrlogsBusinessIf idrlogsBusiness;

    @Autowired(required = false)
    @Qualifier("vchtypesBusinessImpl")
    private VchtypesBusinessIf vchtypesBusiness;
    // Session 中获取
    private Clerks clerks = new Clerks();
    public String scanUrl;
    public String jsonRequest;
    public Idrlogs idrlogsPrint = new Idrlogs();
    public IdentifyForm identifyForm;

    @Autowired(required = false)
    @Qualifier("clerksBusinessImpl")
    private ClerksBusinessIf clerksBusiness;

    @Autowired(required = false)
    @Qualifier("modelCodeBusinessImpl")
    private ModelCodeBusinessIf modelCodeBusiness;

    @Override
    public String execute() {
        clerks = (Clerks) ActionContext.getContext().getSession().get("clerks");
        HttpServletRequest request = ServletActionContext.getRequest();
        String a = request.getParameter("methodName");
        if ("toBillbyH5".equals(a)) {
            //到开始的验印界面
            return BaseActionCallUtil.call(this, "toBillbyH5", "toBillbyH5", "toBillbyH5", "error");
        }else if ("customSeal".equals(a)) {
            //外部系统使用H5验印 返回验印页面的URL
            return BaseActionCallUtil.call(this, "customSeal", null, null, null);
        } else if ("imageIdentify".equals(a)) {
            return BaseActionCallUtil.call(this, "imageIdentify", null, null, "error");
        } else if ("childImageIdentify".equals(a)) {
            return BaseActionCallUtil.call(this, "childImageIdentify", null, null, "error");
        } else if ("getIdentifyResult".equals(a)) {
            return BaseActionCallUtil.call(this, "getIdentifyResult", null, null, "error");
        } else if ("saveIdentifyResult".equals(a)) {
            //保存验印结果
            return BaseActionCallUtil.call(this, "saveIdentifyResult", null, null, "error");
        } else if ("saveH5IdentifyResult".equals(a)) {
            //H5保存验印结果
            return BaseActionCallUtil.call(this, "saveH5IdentifyResult", "print", "print", "error");
        } else if ("getPrivsByOpen".equals(a)) {
            return BaseActionCallUtil.call(this, "getPrivsByOpen", null, null, "error");
        } else if ("sealOpen".equals(a)) {
            //验印加载标准图
            return BaseActionCallUtil.call(this, "sealOpen", null, null, "error");
        } else {
            return "login";
        }
    }
    
    public void toBillbyH5() throws Exception {
        String scanres = null;
        Clerks clk = null;
        List<Vchtypes> vlist = vchtypesBusiness.getAll();
        List<Currency> cList = currencyBusiness.getAll();
        H5billUtil h5ul = new H5billUtil();
        HttpServletResponse response = ServletActionContext.getResponse();
        HttpServletRequest request = ServletActionContext.getRequest();
        String clerkNo = request.getParameter("clerkNo");
        if (null != clerkNo && !"".equals(clerkNo)) {
            clk = clerksBusiness.getByClkno(clerkNo);
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("clerks", clk != null ? clk : clerks);
        paramMap.put("taskId", request.getParameter("taskId"));
        paramMap.put("vlist", vlist);
        paramMap.put("cList", cList);
        paramMap.put("viewlogs", request.getParameter("viewlogs"));
        try {
            scanres = h5ul.initParam(paramMap);
            JSONObject jsonObject = JSONObject.fromObject(scanres);
            if (jsonObject != null) {
                scanUrl = jsonObject.getString("result");
            }
        }catch (Exception e){
            scanUrl = "eeee";
        }
    }
    
    /**
     * 外部系统使用H5验印
     */
    public void customSeal() throws Exception {
        HttpServletResponse response = ServletActionContext.getResponse();
        HttpServletRequest request = ServletActionContext.getRequest();
        Clerks clk = null;
        PrintWriter out = null;
        JSONObject resultObj = new JSONObject();
        try {
            List<Vchtypes> vlist = vchtypesBusiness.getAll();
            List<Currency> cList = currencyBusiness.getAll();
            H5billUtil h5ul = new H5billUtil();
            if (null != request.getParameter("clerkNo") && !"".equals(request.getParameter("clerkNo"))) {
                String clerkNo = request.getParameter("clerkNo");
                clk = clerksBusiness.getByClkno(clerkNo);
            }
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("clerks", clk);
            paramMap.put("vlist", vlist);
            paramMap.put("cList", cList);
            paramMap.put("accountNo", request.getParameter("accountNo"));
            paramMap.put("voucherNo", request.getParameter("voucherNo"));
            paramMap.put("voucherDate", request.getParameter("voucherDate"));
            paramMap.put("amount", request.getParameter("amount"));
            paramMap.put("sealType", request.getParameter("sealType"));
            paramMap.put("voucherType", request.getParameter("voucherType"));
            paramMap.put("sysCode", request.getParameter("sysCode")); //系统号 3009 国库系统
            // 账户建模状态判断，建模了的，建模状态正常，再取url,  不正常，则返回不正常的码值的信息
            Map<String, Object> queryByAccnoMap = new HashMap<>();
            queryByAccnoMap.put("accno", request.getParameter("accountNo"));
            queryByAccnoMap.put("idtype", request.getParameter("sealType"));
            queryByAccnoMap.put("vchdate", request.getParameter("voucherDate"));
            queryByAccnoMap.put("releaseFlag","N");
            JSONObject resObject= h5ul.queryByAccnoFormbatchManager(queryByAccnoMap);
            String scanRes="";
            if(resObject.get("resultState").equals("0000")){
                //账户已经建模，且建模状态正常，返回扫描页面url报文
                scanRes = h5ul.initParam(paramMap);
            }else{
                //返回建模查询相关信息
                scanRes= resObject.toString();
            }

            response.setContentType("text/xml;charset=GB2312");
            out = response.getWriter();
            //返回
            out.write(scanRes);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            resultObj.put("resultState","1111");
            resultObj.put("resultMsg","获取失败");
            response.setContentType("text/xml;charset=GB2312");
            out = response.getWriter();
            out.write(resultObj.toString());
            out.flush();
            out.close();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public void imageIdentify() throws Exception {
        System.out.println("======imageIdentify=========");
        HttpServletResponse response = ServletActionContext.getResponse();
        HttpServletRequest request = ServletActionContext.getRequest();
        PrintWriter out = null;
        IdentifyResponseForm form = new IdentifyResponseForm();
        String resultState = "0000";
        String resultMsg = "成功";

        Map<String, Object> mReqLjava = request.getParameterMap();

        Object obj = IdentifyForm.class.newInstance();
        BeanUtils.populate(obj, mReqLjava);
        IdentifyForm identifyForm = (IdentifyForm) obj;
        H5billUtil h5billUtil = new H5billUtil();
        try {
            if (identifyForm != null) {
                form.setReqType(identifyForm.getReqType());
                form.setFile_id(identifyForm.getFile_id());
                String imageUrl = identifyForm.getImageURL();
                if (imageUrl != null) {
                    //TODO
                    ImageUtil imgUtil = new ImageUtil();
                    byte[] imageContent = imgUtil.readImageUrl(imageUrl);

                    if (imageContent != null) {
                        String base64 = Base64Utils.encode(imageContent);
                        if (base64 != null) {
                            Map<String, String> map = h5billUtil.getStampByCondition(identifyForm);
                            if (map != null) {
                                String resultStr = map.get("result");
                                if ("1".equals(resultStr)) {
                                    //socket发送自动验印请求
                                    AutoIdentifyRequestForm autoForm = new AutoIdentifyRequestForm();
                                    autoForm.setAmount(identifyForm.getAmount());
                                    List<ImageBlockForm> imageList = new ArrayList<ImageBlockForm>();
                                    ImageBlockForm imageBlockForm = new ImageBlockForm();
                                    imageBlockForm.setImageIndex(0);
                                    imageBlockForm.setImageBase64(base64);
                                    imageList.add(imageBlockForm);
                                    List<StampBlockForm> stampList = new ArrayList<StampBlockForm>();
                                    StampBlockForm stampBlockForm = new StampBlockForm();
                                    stampBlockForm.setStampNO(0);
                                    if ("1".equals(identifyForm.getSealType())) {
                                        stampBlockForm.setStampBase64(map.get("stampimage"));
                                    } else if ("2".equals(identifyForm.getSealType())) {
                                        stampBlockForm.setStampBase64(map.get("cachetimage"));
                                    } else if ("4".equals(identifyForm.getSealType())) {
                                        stampBlockForm.setStampBase64(map.get("legalimage"));
                                    }
                                    stampList.add(stampBlockForm);
                                    autoForm.setStampList(stampList);
                                    autoForm.setImageList(imageList);
                                    String identifyResult = h5billUtil.getAutoIdentify(autoForm);
                                    if ("-1".equals(identifyResult)) {
                                        resultState = "3333";
                                        resultMsg = "与自动验印服务连接异常";
                                    } else {
                                        Map<String, Class> classMap = new HashMap<String, Class>();
                                        classMap.put("stampList", StampResultForm.class);
                                        classMap.put("sealGroups", GroupForm.class);
                                        AutoIdentifyResponseForm autoIdentifyResponseForm = (AutoIdentifyResponseForm)
                                                JSONObject.toBean(JSONObject.fromObject(identifyResult), AutoIdentifyResponseForm.class, classMap);
                                        if ("0".equals(autoIdentifyResponseForm.getResultCode())) {
                                            //成功
                                            IdentifyResultForm identifyResultForm = new IdentifyResultForm();
                                            identifyResultForm.setVsResult(autoIdentifyResponseForm.getStampGroupRet());
                                            AccountForm accountForm = new AccountForm();
                                            accountForm.setAccoutNo(identifyForm.getAccountNo());
                                            accountForm.setAccoutName(map.get("companyname"));
                                            identifyResultForm.setAccountMsg(accountForm);
                                            identifyResultForm.setResData(autoIdentifyResponseForm.getResData());
                                            identifyResultForm.setCurrentSeal(autoIdentifyResponseForm.getCurrentSeal());
                                            identifyResultForm.setSealGroups(autoIdentifyResponseForm.getSealGroups());
                                            identifyResultForm.setSealInfos(changeList(autoIdentifyResponseForm.getStampList()));
                                            form.setResult(identifyResultForm);
                                        } else {
                                            resultState = "3333";
                                            resultMsg = autoIdentifyResponseForm.getResultStr();
                                        }
                                    }
                                } else {
                                    resultState = "2222";
                                    resultMsg = map.get("resultstr");
                                }
                            }
                        } else {
                            resultState = "1111";
                            resultMsg = "根据图像地址无法获取图像";
                        }
                    } else {
                        resultState = "1111";
                        resultMsg = "根据图像地址无法获取图像";
                    }
                } else {
                    resultState = "1111";
                    resultMsg = "图像地址不能为空";
                }
            } else {
                resultState = "1111";
                resultMsg = "参数不能为空";
            }
            form.setResultMsg(resultMsg);
            form.setResultState(resultState);
            response.setContentType("text/xml;charset=GB2312");
            out = response.getWriter();
            out.write(JSONObject.fromObject(form).toString());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            resultState = "1111";
            resultMsg = "参数缺失或系统异常";
            form.setResultMsg(resultMsg);
            form.setResultState(resultState);
            response.setContentType("text/xml;charset=GB2312");
            try {
                out = response.getWriter();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            out.write(JSONObject.fromObject(form).toString());
            out.flush();
        } finally {
            if (out != null) {
                out.close();
                out = null;
            }
        }
    }

    public void childImageIdentify() throws Exception {
        System.out.println("======childImageIdentify=========");
    }

    public void getIdentifyResult() throws Exception {
        System.out.println("======getIdentifyResult=========");
    }

    public void saveH5IdentifyResult() throws Exception {
        System.out.println("======saveH5IdentifyResult=========");
         HttpServletResponse response = ServletActionContext.getResponse();
        HttpServletRequest request = ServletActionContext.getRequest();
        String resultState = "0000";
        String resultMsg = "成功";
        Map<String, Object> mReqLjava = request.getParameterMap();
        Map<String, Object> mReq = new HashMap<String, Object>();
        EcmClient ecmClient = new EcmClient();
        XskyClient xskyClient = new XskyClient();

        for (String key : mReqLjava.keySet()) {
            String[] valueArry = (String[]) mReqLjava.get(key);
            mReq.put(key, valueArry[0]);
        }

        try {
            if (mReq != null && mReq.size() > 0) {
                JSONObject resultObject = JSONObject.fromObject(mReq.get("custom"));
                Integer idx = Integer.parseInt(resultObject.get("idx").toString());
                Idrlogs ilogs = idrlogsBusiness.getByIdx(idx);

                if(null != mReq.get("sysidres")){
                    ilogs.setSysidres(Integer.parseInt(mReq.get("sysidres").toString()));
                }
                if(null != mReq.get("syssptres")){
                    ilogs.setSysstpres(mReq.get("syssptres").toString());
                }
                if(null != mReq.get("manidres")){
                    ilogs.setManidres(Integer.parseInt(mReq.get("manidres").toString()));
                }
                if(null != mReq.get("manstpres")){
                    ilogs.setManstpres(mReq.get("manstpres").toString());
                }
                ilogs.setClkno(clerks.getClkno());
//                ilogs.setDepno(clerks.getDepno()); //接口验印时已经插入账号所在机构号了
                //保存结果时 将之前保存的整个验印结果 更新替换为重合图段

                if (StringUtils.isNotBlank(mReq.get("warclk").toString())) {
                    ilogs.setWarclk(mReq.get("warclk").toString());
                }
    
                String resData = (String) mReq.get("resData");
                //更新ECM文件
                if(null != resData && !"".equals(resData)){
                    String filename_t = ecmClient.saveIdresFile(resData.getBytes());
                    File f = new File(filename_t);
                    if (f.exists()) {
                        String oldKey = ilogs.getIdresno() + "/0";
                        xskyClient.deleteObject(oldKey);
                        String key = xskyClient.putObject(null,filename_t, f);
                        ilogs.setIdresno(key);
                    }
                }
                //记录用户点击操作和小码修改操作
                int standardImage = resultObject.getInt("standardImage"); //是否点击标准图
                int rotateImage =  resultObject.getInt("rotateImage"); //是否点击旋转图
                int outLineImage =  resultObject.getInt("outLineImage"); //是否点击轮廓图
                int dogEarImage = resultObject.getInt("dogEarImage"); //是否点击折角图
                String editSmallCode = resultObject.getString("editSmallCode"); // 编辑后的小码
                String ocrLib = resultObject.getString("ocrLib"); // 预留小码
                String ocrTar = resultObject.getString("ocrTar"); // 票据小码
                ilogs.setOcrLib(ocrLib);
                ilogs.setEditSmallCode(editSmallCode);
                ilogs.setOcrTar(ocrTar);
                ilogs.setClickImage(standardImage + "," + rotateImage + "," + outLineImage + "," + dogEarImage);

//                Blob idresimg  = new BlobImpl(com.tchzt.base.util.Base64Utils.getFromBASE64(""));
//                ilogs.setIdres(idresimg);
                idrlogsBusiness.updateIdrlogs(ilogs);
                idrlogsPrint = ilogs;  //用于打印
                xskyClient.shutdown();
    
            } else {
                resultMsg = "无验印结果返回,保存验印结果失败";
                resultState = "eeee";
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    public void saveIdentifyResult() throws Exception {
        System.out.println("======saveIdentifyResult=========");
        HttpServletResponse response = ServletActionContext.getResponse();
        HttpServletRequest request = ServletActionContext.getRequest();
        PrintWriter out = null;
        InitResponseForm form = new InitResponseForm();
        String resultState = "0000";
        String resultMsg = "成功";
        Map<String, Object> mReqLjava = request.getParameterMap();
        Map<String, Object> mReq = new HashMap<String, Object>();
        EcmClient ecmClient = new EcmClient();
        XskyClient xskyClient = new XskyClient();

        for (String key : mReqLjava.keySet()) {
            String[] valueArry = (String[]) mReqLjava.get(key);
            mReq.put(key, valueArry[0]);
        }
        try {
            if (mReq != null && mReq.size() > 0) {
                JSONObject resultObject = JSONObject.fromObject(mReq.get("custom"));
                Integer idx = Integer.parseInt(resultObject.get("idx").toString());
                Idrlogs ilogs = idrlogsBusiness.getByIdx(idx);

                if(null != mReq.get("sysidres")){
                    ilogs.setSysidres(Integer.parseInt(mReq.get("sysidres").toString()));
                }
                if(null != mReq.get("syssptres")){
                    ilogs.setSysstpres(mReq.get("syssptres").toString());
                }
                if(null != mReq.get("manidres")){
                    ilogs.setManidres(Integer.parseInt(mReq.get("manidres").toString()));
                }
                if(null != mReq.get("manstpres")){
                    ilogs.setManstpres(mReq.get("manstpres").toString());
                }
                ilogs.setClkno(mReq.get("userNo").toString());
//                ilogs.setDepno(mReq.get("orgCode").toString());  //接口验印时已经插入账号所在机构号了

                //保存结果时 将之前保存的整个验印结果 更新替换为重合图段
                String resData = (String) mReq.get("resData");
//                Blob idresimg = new BlobImpl(com.tchzt.base.util.Base64Utils.getFromBASE64(""));
//                ilogs.setIdres(idresimg);
                //更新文件
                if(null != resData && !"".equals(resData)){
                    //先保存本地
                    String filename_t = ecmClient.saveIdresFile(resData.getBytes());
                    File f = new File(filename_t);
                    if (f.exists()) {
                        String oldKey = ilogs.getIdresno() + "/0";
                        xskyClient.deleteObject(oldKey);
                        String key = xskyClient.putObject(null,filename_t, f);
                        ilogs.setIdresno(key);
                    }
                }
                //记录用户点击操作和小码修改操作
                int standardImage = resultObject.getInt("standardImage"); //是否点击标准图
                int rotateImage =  resultObject.getInt("rotateImage"); //是否点击旋转图
                int outLineImage =  resultObject.getInt("outLineImage"); //是否点击轮廓图
                int dogEarImage = resultObject.getInt("dogEarImage"); //是否点击折角图
                String editSmallCode = resultObject.getString("editSmallCode"); // 编辑后的小码
                String ocrLib = resultObject.getString("ocrLib"); // 预留小码
                String ocrTar = resultObject.getString("ocrTar"); // 票据小码
                ilogs.setOcrLib(ocrLib);
                ilogs.setEditSmallCode(editSmallCode);
                ilogs.setOcrTar(ocrTar);
                ilogs.setClickImage(standardImage + "," + rotateImage + "," + outLineImage + "," + dogEarImage);

                idrlogsBusiness.updateIdrlogs(ilogs);
            } else {
                resultMsg = "无验印结果返回,保存验印结果失败";
                resultState = "eeee";
            }
            xskyClient.shutdown();
            form.setResultMsg(resultMsg);
            form.setResultState(resultState);
            response.setContentType("text/xml;charset=GB2312");
            out = response.getWriter();
            out.write(JSONObject.fromObject(form).toString());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
                out = null;
            }
        }
    }

    public void getPrivsByOpen() throws Exception {
        System.out.println("======getPrivsByOpen=========");
    }

    public void sealOpen() throws Exception {
        HttpServletResponse response = ServletActionContext.getResponse();
        System.out.println("======sealOpen=========");
        H5billUtil h5billUtil = new H5billUtil();
        ImageUtil imageUtil = new ImageUtil();
        ImageUtilH5 imgUtil5 = new ImageUtilH5();
        XskyClient xskyClient = new XskyClient();
        EcmClient ecmClient = new EcmClient();
        Map<String, String> map = null;
        String result = null;
        //在调用后台之前是否有错误
        String mesg = null;
        PrintWriter out = null;
        //批量验印图片地址，单张模式
        String batchImageUrl = null;
        try {
            JSONObject requestObject = JSONObject.fromObject(jsonRequest);
            if (requestObject.get("taskId") != null) {
                Integer taskId = Integer.parseInt(requestObject.getString("taskId"));
                Idrlogs idr = idrlogsBusiness.getByIdx(taskId);
                if (idr != null) {
                    String idresno = idr.getIdresno();
                    if (StringUtils.isNotBlank(idresno)) {
                        String idresUrl;
                        String base64Str = "";
                        if (idresno.startsWith("xsky-")) {
                            List<String> object = xskyClient.getObject(idr.getIdresno());
                            if (object != null && object.size() > 0) {
                                idresUrl = object.get(0);
                                base64Str = xskyClient.getIdresByUrl(idresUrl);//方法直接从URL读取返回内容
                            }
                        } else {
                            //否则就是存在巨杉的
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                            String ecmRes = ecmClient.queryFile(idr.getIdresno(), sdf.format(idr.getOpttime()), idr.getEventid());
                            idresUrl = ecmClient.getFileUrlFromXml("", ecmRes);
                            base64Str = ecmClient.getIdresByUrl(idresUrl);
                        }
                        byte[] resultByte = Base64Utils.getFromBASE64(base64Str);
                        String resStr = new String(resultByte);
                        result = resStr;
                        if (!result.startsWith("{\"currentSeal")) { //只有重合图
                            Gson gson = new Gson();
                            H5ResSidrvSend h5ResSidrvSend;
                            h5ResSidrvSend = new H5ResSidrvSend(base64Str);
                            result = h5billUtil.getSocketResultByJson(gson.toJson(h5ResSidrvSend));
                            if (result.length() < 1000) {
                                h5ResSidrvSend = new H5ResSidrvSend(resStr);
                                result = h5billUtil.getSocketResultByJson(gson.toJson(h5ResSidrvSend));
                            }
                        }
                    } else {
                        //历史日志 存数据库的
                        result = new String(idr.getIdresByte());
                        if (!result.startsWith("{\"currentSeal")) {
                            Gson gson = new Gson();
                            H5ResSidrvSend h5ResSidrvSend = new H5ResSidrvSend(com.tchzt.base.util.StringUtils.inverseLn(Base64Utils.getBASE64(idr.getIdresByte())));
                            result = h5billUtil.getSocketResultByJson(gson.toJson(h5ResSidrvSend));
                        }
                    }

                    JSONObject resultObject = JSONObject.fromObject(result);
                    resultObject.put("accountNo", idr.getAccno());
                    resultObject.put("voucherType", idr.getVchtype());
                    resultObject.put("voucherNo", idr.getVchno());
                    resultObject.put("amount", String.format("%.2f",idr.getAmount()));
                    resultObject.put("currency", "CNY");
                    resultObject.put("sealType", "1");
                    // 数据库 0印鉴1公章，前端 1印鉴2公章
                    if ("1".equals(idr.getSupervisorcachet())) {
                        resultObject.put("sealType", "2");
                    }

                    if (null == idr.getVchdate()) {
                        resultObject.put("voucherDate", "");
                    } else {
                        resultObject.put("voucherDate", idr.getVchdate());
                    }
                    //系统结果串，用于小码干预通过后，查看日志时展示正确的系统通过状态。
                    resultObject.put("syssptres",idr.getSysstpres());
                    Map<String, Object> customMap = new HashMap<>();
                    customMap.put("sealCkeckId", idr.getSealCkeckId());
                    customMap.put("billCardno", idr.getCardNo());
                    customMap.put("idx", idr.getIdx());
                    resultObject.put("custom", customMap);
                    //选章识别的图片显示，先去影像平台根据Idrname取图片
                    SimpleDateFormat datefm = null;
                    String admsdate = "";
                    String imgURLStr = "";
                    String idrname = idr.getIdrname();
                    if (idrname.startsWith("xsky-")) {
                        List<String> object = xskyClient.getObject(idrname);
                        if (object != null && object.size() > 0) {
                            imgURLStr = object.get(0);
                        }
                    }else {
                        try {
                            datefm = new SimpleDateFormat("yyyyMMdd");
                            if (null != idr.getSupervisortime() && !"".equals(idr.getSupervisortime())) {
                                admsdate = idr.getSupervisortime();
                            } else {
                                datefm.format(idr.getOpttime());
                            }
                            String ecmImageRes = ecmClient.queryFile(idr.getIdrname().split("##")[0], admsdate, idr.getEventid());
                            imgURLStr = ecmClient.getFileUrlFromXml(idr.getIdrname(), ecmImageRes);
                        } catch (Exception e) {
                            datefm = new SimpleDateFormat("yyyyMMDD");
                            admsdate = datefm.format(idr.getOpttime());
                            String ecmImageRes = ecmClient.queryFile(idr.getIdrname().split("##")[0], admsdate, idr.getEventid());
                            imgURLStr = ecmClient.getFileUrlFromXml(idr.getIdrname(), ecmImageRes);
                        }
                    }
//                    resultObject.put("imageUrl", imgURLStr);
                    //柜面验印选章识别需要定义票据影像的dpi
                    JSONArray newArray = new JSONArray();
                    JSONObject image = new JSONObject();
                    image.put("index", 0);
                    image.put("url", imgURLStr);
                    if (StringUtils.isNotBlank(idr.getSupervisorstamp())) {
                        image.put("dpi", Integer.parseInt(idr.getSupervisorstamp()));
                    } else {
                        image.put("dpi", 200);
                    }

                    newArray.add(image);
                    resultObject.put("images", newArray);
                    //根据eventId封装右侧要素信息
                    List<ShowInfoVo> showInfoList = new ArrayList<>();
                    buildShowInfoList(idr, showInfoList);
                    resultObject.put("showInfoList", JSONArray.fromObject(showInfoList));

                    result = resultObject.toString();
                }
            } else {
                //处理下json，如包含印鉴组合，去除凭证分组、系统分组
                if (requestObject.get("sealGroups") != null) {
                    JSONArray jsonArray = JSONArray.fromObject(requestObject.get("sealGroups"));
                    JSONArray newArray = new JSONArray();
                    for (Object object : jsonArray) {
                        JSONObject group = JSONObject.fromObject(object);
                        if ("0".equals(group.getString("groupType"))) {
                            newArray.add(group);
                        }
                    }
                    requestObject.put("sealGroups", newArray);
                }
                //处理下json，将图像url转为base64字符串
                if (requestObject.get("imageList") != null) {
                    JSONArray jsonArray = JSONArray.fromObject(requestObject.get("imageList"));
                    JSONArray newArray = new JSONArray();
                    //验印类型：验印，复核
                    String ywType = (String)requestObject.get("ywType");
                    for (Object object : jsonArray) {
                        JSONObject image = JSONObject.fromObject(object);
                        String imageBase64 = image.getString("imageBase64");
                        //前端说传的路径没问题，那这里替换一下改成client
                        imageBase64 = imageBase64.replace("/localService/","/clientService/");
                        image.put("imageBase64", Base64Utils.encode(imageUtil.readImageUrl(imageBase64)));
                        newArray.add(image);
                        batchImageUrl = imageBase64;
                        if("YANYIN".equals(ywType)){
                            break; //如果是验印，只取第一张（正面）去验印,遍历一次就退出
                        }
                    }
                    if("FUHE".equals(ywType) && jsonArray.size()==2){
                        newArray.remove(0);    //如果是复核并且传了两张，删掉第一张用背面旧印鉴验印
                    }
                    requestObject.put("imageList", newArray);
                } else {
                    if (requestObject.get("imageBase64") != null) {
                        String imageUrl = requestObject.getString("imageBase64");
                        if (imageUrl != null) {
                            imageUrl = imageUrl.replace("/localService/","/clientService/");
                            requestObject.put("imageBase64", Base64Utils.encode(imageUtil.readImageUrl(imageUrl)));
                            batchImageUrl = imageUrl;
                        }
                    }
                }
                //转换印鉴数据块
                if (requestObject.get("taskCode") != null) {

                    System.out.println("taskCode===" + requestObject.getString("taskCode"));
                    if (requestObject.getString("taskCode").equals("AUTO_IDEN")) {
                        //扫描完成，自动验印
                        if (requestObject.get("ywType") != null) {
                            //复核验印
                            if(requestObject.getString("ywType").equals("FUHE")){
                                if (requestObject.get("accountNo") != null) {
                                    JSONArray newArray = null;
                                    if (requestObject.get("stamplibNo") != null) {
                                        IdentifyForm identifyForm = new IdentifyForm();
                                        identifyForm.setAccountNo(requestObject.getString("accountNo"));
                                        identifyForm.setStamplibNo(requestObject.getInt("stamplibNo"));
                                        identifyForm.setCachetlibNo(requestObject.getInt("cachetlibNo"));
                                        identifyForm.setVoucherNo("999");
                                        identifyForm.setVoucherType("299");
                                        identifyForm.setTaskCode("FUHE");

                                        map = h5billUtil.getStampByCondition(identifyForm);
                                        if(map != null){
                                            String stamp = map.get("stamp");
                                            newArray = JSONArray.fromObject(stamp);
                                            if (newArray.toString().length() < 1000) {
                                                mesg = "建库印鉴信息有误，需重新建库";
                                            }
                                        }
                                    }else {
                                        IdentifyForm identifyForm = new IdentifyForm();
                                        identifyForm.setAccountNo(requestObject.getString("accountNo"));
                                        identifyForm.setStamplibNo(1);
                                        identifyForm.setCachetlibNo(1);
                                        identifyForm.setVoucherNo("999");
                                        identifyForm.setVoucherType("299");
                                        identifyForm.setTaskCode("FUHE");

                                        map = h5billUtil.getStampByCondition(identifyForm);
                                        if(map != null){
                                            String stamp = map.get("stamp");
                                            newArray = JSONArray.fromObject(stamp);
                                            if (newArray.toString().length() < 1000) {
                                                mesg = "建库印鉴信息有误无法审核，需重新建库";
                                            }
                                        }
                                    }
                                    if(newArray != null){
                                        requestObject.put("stampList", newArray);
                                    }
                                }
                            }else {
                                if (requestObject.getString("ywType").equals("YANYIN")) {
                                    IdentifyForm identifyForm = new IdentifyForm();
                                    identifyForm.setTaskCode("YANYIN");
                                    if (requestObject.get("accountNo") != null) {
                                        identifyForm.setAccountNo(requestObject.getString("accountNo"));
                                    }
                                    if (requestObject.get("voucherNo") != null) {
                                        identifyForm.setVoucherNo(requestObject.getString("voucherNo"));
                                    }
                                    if (requestObject.get("voucherType") != null) {
                                        identifyForm.setVoucherType(requestObject.getString("voucherType"));
                                    }
                                    if (requestObject.get("sealType") != null) {
                                        //验印类型需要转一下，getStampByCondition中0是验印鉴，1是验印公章
                                        if (1 != Integer.parseInt(requestObject.getString("sealType"))) {
                                            identifyForm.setSealType("1");
                                        } else {
                                            identifyForm.setSealType("0");
                                        }
                                    }
                                    if (requestObject.get("voucherDate") != null) {
                                        identifyForm.setVoucherDate(requestObject.getString("voucherDate"));
                                    }
                                    map = h5billUtil.getStampByCondition(identifyForm);
                                    String resultStr = map.get("result");
                                    if ("1".equals(resultStr)) {
                                        JSONArray newArray = new JSONArray();
                                        JSONObject image = new JSONObject();
                                        int no = 0;
                                        if ("0".equals(identifyForm.getSealType())) {
                                            if (map.containsKey("stampimage")) {
                                                image.put("stampNO", no);
                                                image.put("stampBase64", map.get("stampimage"));
                                                image.put("stampType", 0);
                                                newArray.add(image);
                                            }
                                        } else if ("1".equals(identifyForm.getSealType())) {
                                            if (map.containsKey("cachetimage")) {
                                                image.put("stampNO", no);
                                                image.put("stampBase64", map.get("cachetimage"));
                                                image.put("stampType", 1);
                                                newArray.add(image);
                                            }
                                        }
                                        requestObject.put("stampList", newArray);
                                    } else {
                                        mesg = map.get("resultstr");
                                    }
                                }
                            }
                        }
                    } else if (requestObject.getString("taskCode").equals("MAN_IDEN")) {
                        //复核选章识别
                        if(requestObject.getString("ywType").equals("FUHE")){
                            if (requestObject.get("accountNo") != null) {
                                JSONArray newArray = null;
                                if (requestObject.get("stamplibNo") != null) {
                                    IdentifyForm identifyForm = new IdentifyForm();
                                    identifyForm.setAccountNo(requestObject.getString("accountNo"));
                                    identifyForm.setStamplibNo(requestObject.getInt("stamplibNo"));
                                    identifyForm.setCachetlibNo(requestObject.getInt("cachetlibNo"));
                                    identifyForm.setVoucherNo("999");
                                    identifyForm.setVoucherType("299");
                                    identifyForm.setTaskCode("FUHE");

                                    map = h5billUtil.getStampByCondition(identifyForm);
                                    if(map != null){
                                        String stamp = map.get("stamp");
                                        newArray = JSONArray.fromObject(stamp);
                                        if (newArray.toString().length() < 1000) {
                                            mesg = "建库印鉴信息有误无法审核，需重新建库";
                                        }
                                    }
                                }else {
                                    IdentifyForm identifyForm = new IdentifyForm();
                                    identifyForm.setAccountNo(requestObject.getString("accountNo"));
                                    identifyForm.setStamplibNo(1);
                                    identifyForm.setCachetlibNo(1);
                                    identifyForm.setVoucherNo("999");
                                    identifyForm.setVoucherType("299");
                                    identifyForm.setTaskCode("FUHE");

                                    map = h5billUtil.getStampByCondition(identifyForm);
                                    if(map != null){
                                        String stamp = map.get("stamp");
                                        newArray = JSONArray.fromObject(stamp);
                                        if (newArray.toString().length() < 1000) {
                                            mesg = "建库印鉴信息有误无法审核，需重新建库";
                                        }
                                    }
                                }
                                if(newArray != null){
                                    requestObject.put("stampList", newArray);
                                }
                            }
                        }else {
                            //选章识别
                            if (requestObject.getString("ywType").equals("YANYIN")) {
                                IdentifyForm identifyForm = new IdentifyForm();
                                if (requestObject.get("accountNo") != null) {
                                    identifyForm.setAccountNo(requestObject.getString("accountNo"));
                                    identifyForm.setVoucherNo("999");
                                    identifyForm.setVoucherType("299");
                                }
                                if (requestObject.get("sealType") != null) {

                                    //验印类型需要转一下，getStampByCondition中0是验印鉴，1是验印公章
                                    if (1 != Integer.parseInt(requestObject.getString("sealType"))) {
                                        identifyForm.setSealType("1");
                                    } else {
                                        identifyForm.setSealType("0");
                                    }
                                }
                                if (requestObject.get("voucherDate") != null) {
                                    identifyForm.setVoucherDate(requestObject.getString("voucherDate"));
                                }
                                //如果是选章识别，那么不用再判断账户状态而被阻断了，因为查询账户状态时已经判断过了，这里不应再阻断
                                map = h5billUtil.getStampByCondition(identifyForm);
                                String resultStr = map.get("result");
                                if ("1".equals(resultStr)) {
                                    //子图验印需要改传 stampList 参数 弃用 stampBase64
                                    JSONArray newArray = new JSONArray();
                                    JSONObject image = new JSONObject();
                                    int no = 0;
//                                String stampBase64 = null;
                                    if ("0".equals(identifyForm.getSealType())) {
                                        if (map.containsKey("stampimage")) {
//                                        stampBase64 = map.get("stampimage");
                                            image.put("stampNO", no);
                                            image.put("stampBase64", map.get("stampimage"));
                                            image.put("stampType", 0);
                                            newArray.add(image);

                                        }
                                    } else if ("1".equals(identifyForm.getSealType())) {
                                        if (map.containsKey("cachetimage")) {
//                                        stampBase64 = map.get("cachetimage");
                                            image.put("stampNO", no);
                                            image.put("stampBase64", map.get("cachetimage"));
                                            image.put("stampType", 1);
                                            newArray.add(image);
                                        }
                                    }
//                                requestObject.put("stampBase64", stampBase64);
                                    requestObject.put("stampList", newArray);
                                } else {
                                    mesg = map.get("resultstr");
                                }
                            }
                        }
                    } else if (requestObject.getString("taskCode").equals("SEAL_SHOW")) {
                        //变更初始化界面按照套数去查印鉴信息
                        if (requestObject.get("stamplibNo") != null && requestObject.get("accountNo") != null &&
                                requestObject.get("cachetlibNo") != null && requestObject.get("legallibNo") != null) {
                            JSONObject requestObject_show = new JSONObject();
                            IdentifyForm identifyForm = new IdentifyForm();
                            identifyForm.setAccountNo(requestObject.getString("accountNo"));
                            identifyForm.setStamplibNo(requestObject.getInt("stamplibNo"));
                            identifyForm.setCachetlibNo(requestObject.getInt("cachetlibNo"));
                            identifyForm.setVoucherNo("999");
                            identifyForm.setVoucherType("299");
                            identifyForm.setTaskCode("SEAL_SHOW");

                            map = h5billUtil.getStampByCondition(identifyForm);
                            if(map != null){
                                if (!"1".equals(map.get("result"))){
                                    mesg = map.get("resultstr");
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("resultCode", "-3");
                                    jsonObject.put("resultStr", mesg);
                                    result = jsonObject.toString();
                                }else{
                                    JSONObject image = new JSONObject();
                                    int no = 0;
                                    String stamp = map.get("stamp");
                                    JSONArray newArray = JSONArray.fromObject(stamp);
                                    requestObject_show.put("taskCode", requestObject.getString("taskCode"));
                                    requestObject_show.put("stampList",newArray);
                                    requestObject_show.put("showEpArea", requestObject.getInt("showEpArea"));
                                    requestObject_show.put("stampGroup", requestObject.getInt("stampGroup"));
                                    requestObject=null;
                                    requestObject=requestObject_show;
                                }
                            }
                        }else{
                            //建库查看预留印鉴
                            IdentifyForm identifyForm = new IdentifyForm();
                            if (requestObject.get("accountNo") != null) {
                                identifyForm.setAccountNo(requestObject.getString("accountNo"));
                                identifyForm.setVoucherNo("999");
                                identifyForm.setVoucherType("299");
                            }
                            if (requestObject.get("sealType") != null) {
                                if (1 != Integer.parseInt(requestObject.getString("sealType"))) {
                                    identifyForm.setSealType("1");
                                } else {
                                    identifyForm.setSealType("0");
                                }
                            }

                            if (requestObject.get("voucherDate") != null) {
                                identifyForm.setVoucherDate(requestObject.getString("voucherDate"));
                            }
                            String sysCode = (String) requestObject.get("taskCode");
                            //查看预留，那么不用再判断账户状态而被阻断了，因为查询账户状态时已经判断过了，这里不应再阻断
                            map = h5billUtil.getStampByCondition(identifyForm);
                            String resultStr = map.get("result");
                            if ("1".equals(resultStr)) {
                                JSONArray newArray = new JSONArray();
                                JSONObject image = new JSONObject();
                                int no = 0;
                                if ("0".equals(identifyForm.getSealType())) {
                                    if (map.containsKey("stampimage")) {
                                        image.put("stampNO", no);
                                        image.put("stampBase64", map.get("stampimage"));
                                        image.put("stampType", 0);
                                        newArray.add(image);
                                    }
                                } else if ("1".equals(identifyForm.getSealType())) {
                                    if (map.containsKey("cachetimage")) {
                                        image.put("stampNO", no);
                                        image.put("stampBase64", map.get("cachetimage"));
                                        image.put("stampType", 1);
                                        newArray.add(image);
                                    }
                                }
                                requestObject.put("stampList", newArray);
                            } else {
                                mesg = map.get("resultstr");
                            }
                        }
                    }
                }
                if (mesg == null) {
                    result = h5billUtil.getSocketResultByJson(requestObject.toString());
                    if (result == null) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("resultCode", "-2");
                        jsonObject.put("resultStr", "印鉴处理后台未返回任何消息");
                        result = jsonObject.toString();
                    } else if (result.equals("-1")) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("resultCode", "-1");
                        jsonObject.put("resultStr", "与后台服务器不通，请检查配置");
                        result = jsonObject.toString();
                    } else {
                        /*TODO 如果是建库结果提交，保存后并返回信息
                         */
                        if (requestObject.get("taskCode") != null && !requestObject.get("taskCode").equals("GET_RET")) {

                            List<String> urlList = new ArrayList<>();
                            if (map != null) {
                                String busiSerialNo = map.get("busiSerialNo");
                                if(busiSerialNo.startsWith("xsky-")){
                                    urlList = xskyClient.getObject(busiSerialNo);
                                }else {
                                    urlList = imgUtil5.getImgUrlBySerialNo(busiSerialNo);
                                    if (urlList == null || urlList.isEmpty()) {
                                        // urlList = imageUtil.getImgUrlBySerialNo(busiSerialNo);
                                        urlList=imgUtil5.getTims3ScanImgUrl(busiSerialNo);
                                    }
                                }
                            }
                            if (requestObject.getString("taskCode").equals("SEAL_SHOW")) {
                                //查看预留印鉴
                                JSONObject resultObject = JSONObject.fromObject(result);
                                //返回影像流水（供h5使用流水号获取影像）用于后面显示印鉴原图
//                                String busiSerialNo = map.get("busiSerialNo");
//                                if (busiSerialNo != null) {
//                                    resultObject.put("busiSerialNo",busiSerialNo);
//                                }
                                Map<String, Object> customMap = new HashMap<>();
                                customMap.put("sealCkeckId", map.get("sealCkeckId"));
                                customMap.put("billCardno", map.get("cardNo"));
                                resultObject.put("custom", customMap);
                                //预留印鉴原图，为了兼容新旧的，都采用查询直接返回图片地址的方式，不返回流水号，busiSerialNo为0时， 表示手动传图
                                resultObject.put("busiSerialNo","0");
                                JSONArray stampArray = new JSONArray();
                                for (int i = 0; i < urlList.size(); i++) {
                                    JSONObject stampObject = new JSONObject();
                                    stampObject.put("index", i);
                                    stampObject.put("url", urlList.get(i));
                                    stampArray.add(stampObject);
                                }
                                resultObject.put("images", stampArray);
    
                                result = resultObject.toString();

                            } else if (requestObject.getString("taskCode").equals("AUTO_BUILD")) {
                                JSONObject resultObject = JSONObject.fromObject(result);
                                result = resultObject.toString();
                            } else if (requestObject.getString("taskCode").equals("AUTO_IDEN") && requestObject.getString("ywType").equals("YANYIN")) {
                                //扫描完成，自动验印
                                JSONObject accountMsg = new JSONObject();
                                JSONObject resultObject = JSONObject.fromObject(result);
                                accountMsg.put("accountNo", requestObject.getString("accountNo"));
                                accountMsg.put("accountName", map.get("companyname"));
                                resultObject.put("accountMsg", accountMsg);

                                Map<String, Object> customMap = new HashMap<>();
                                customMap.put("sealCkeckId", map.get("sealCkeckId"));
                                customMap.put("billCardno", map.get("cardNo"));
                                //customMap.put("eventId","678678");

//                                List<String> list = new LinkedList<>();
//                                list.add(urlList.get(0));
                                customMap.put("stampImgUrlList", urlList);
                                //获取初始化页面getScanUrl请求时，custom里传入系统号，用于区分渠道是国库系统还是本系统
                                Object requestCustom = requestObject.get("custom");
                                int sysCode = 3006;
                                if(requestCustom!=null){
                                    JSONObject jsonObject = JSONObject.fromObject(requestCustom);
                                    String code = (String) jsonObject.get("sysCode");
                                    if (code != null && !"".equals(code)) {
                                        sysCode = Integer.parseInt(code);
                                    }
                                }
                                //保存自动验印结果
                                Idrlogs ilogs = new Idrlogs();
                                if (sysCode == 3009) {
                                    ilogs.setEventid(3009);
                                    ilogs.setMemo("国库系统验印");
                                } else {
                                    ilogs.setEventid(3006);
                                    ilogs.setMemo("新票据验印");
                                }

                                String accountNo = requestObject.getString("accountNo");
                                String voucherNo = requestObject.getString("voucherNo");
                                String voucherType = requestObject.getString("voucherType");
                                String amount = requestObject.getString("amount");
                                String voucherDate = requestObject.getString("voucherDate");

                                String sysidres = resultObject.getString("stampGroupRet");
                                String idresimgStr = resultObject.getString("resData");
                                String sysstpres = "";
                                if (resultObject.get("stampList") != null) {
                                    JSONArray jsonArray = JSONArray.fromObject(resultObject.get("stampList"));
                                    JSONArray newArray = new JSONArray();
                                    for (Object object : jsonArray) {
                                        JSONObject stampRes = JSONObject.fromObject(object);
                                        sysstpres = sysstpres + stampRes.getString("checkRet");

                                    }
                                }
                                ilogs.setAccno(accountNo.trim());
                                ilogs.setVchno(voucherNo != null ? voucherNo.trim() : "");
                                ilogs.setVchtype(voucherType != null ? voucherType.trim() : "");
                                ilogs.setAmount(amount != null ? Double.valueOf(amount.trim()) : 0.00);
                                ilogs.setVchdate(voucherDate != null ? voucherDate.trim() : "");
                                ilogs.setSysidres(Integer.parseInt(sysidres));
                                ilogs.setSysstpres(sysstpres);
                                ilogs.setManidres(-1);
                                ilogs.setManstpres("");
                                ilogs.setSealCkeckId(map.get("sealCkeckId"));
                                ilogs.setCardNo(map.get("cardNo"));
                                ilogs.setBusiSerialNo(map.get("busiSerialNo"));
                                ilogs.setDepno(map.get("accDepNo"));
                                if("2".equals(requestObject.getString("sealType"))){//公章
                                    ilogs.setSupervisorcachet("1");//公章
                                }else {
                                    ilogs.setSupervisorcachet("0");
                                }
                                //  ilogs.setSupervisor(map.get("devicename").toString());
                                // ilogs.setWarclk(warclk);//aa记入 授权柜员

                                ilogs.setOpttime(new java.sql.Timestamp(System.currentTimeMillis()));//操作时间
                                ilogs.setBusitype(0);//业务类型
                                // ilogs.setIdrname(batchImageUrl);
                                try {
                                    // 将验印图片存至影像平台
                                    ImageUtil imgUtil = new ImageUtil();
                                    byte[] b = imgUtil.readImageUrl(batchImageUrl);
                                    String filename = ecmClient.saveImage(b);//将图片临时存到本地
                                    String batchId = ecmClient.uploadFile(filename,sysCode,"");
                                    ilogs.setIdrname(batchId);

                                    Date date = new Date();
                                    SimpleDateFormat dateSup = new SimpleDateFormat("yyyyMMdd");
                                    ilogs.setSupervisortime(dateSup.format(date));

                                } catch (IOException e) {
                                    System.out.println("新票据验印上传图片到影像平台异常");
                                    e.printStackTrace();
                                }
                                //
//                                Blob idresimg = new BlobImpl(result.getBytes());
//                                ilogs.setIdres(idresimg);
                                //先保存
                                String saveResData = com.tchzt.base.util.StringUtils.inverseLn(Base64Utils.getBASE64(result.getBytes()));
                                String filename_t = ecmClient.saveIdresFile(saveResData.getBytes());
                                File f = new File(filename_t);
                                if (f.exists()) {
                                    String batchId = xskyClient.putObject(null,filename_t,f);
                                    ilogs.setIdresno(batchId);
                                }
                                idrlogsBusiness.addIdrlogs(ilogs);
                                customMap.put("idx", ilogs.getIdx());
                                //先存空，等提交的时候再更新
                                resultObject.put("custom", customMap);
                                resultObject.put("taskId",ilogs.getIdx());

                                System.out.println("billaction记录验印日志");

//                                System.out.println("---custom扫描完成---"+resultObject.toString());
                                result = resultObject.toString();
                            }
                        }
                    }
                } else {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("resultCode", "-3");
                    jsonObject.put("resultStr", mesg);
                    result = jsonObject.toString();
                }
            }
            response.setContentType("text/xml;charset=UTF-8");
            out = response.getWriter();
            System.out.println("======sealOpen结束=========");
            xskyClient.shutdown();
            out.write(result);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
                out = null;
            }
        }
    }

    //类型转换
    private List<H5StampResultForm> changeList(List<StampResultForm> stampList) {
        List<H5StampResultForm> list = null;
        if (stampList != null) {
            list = new ArrayList<H5StampResultForm>();
            H5StampResultForm form = null;
            for (StampResultForm stampResultForm : stampList) {
                form = new H5StampResultForm();
                form.setSealNo(stampResultForm.getStampIndex());
                form.setSealResult(stampResultForm.getCheckRet());
                form.setChidrenImageURL(stampResultForm.getSrcCutImage());
                form.setOutLineImageURL(stampResultForm.getLunkImage());
                form.setSealImageURL(stampResultForm.getStampImage());
                form.setStandardImageURL(stampResultForm.getResImage());
                form.setBillImage(stampResultForm.getZhipImage());
                list.add(form);
            }
        }
        return list;
    }

    private void buildShowInfoList(Idrlogs idr, List<ShowInfoVo> showInfoList) {

        ShowInfoVo showInfo;
        /**
         * 基础数据
         */
        showInfo = new ShowInfoVo("accountNo", "账号", idr.getAccno(), true);
        showInfoList.add(showInfo);
        showInfo = new ShowInfoVo("voucherNo", "凭证号码", idr.getVchno(), true);
        showInfoList.add(showInfo);
        showInfo = new ShowInfoVo("voucherType", "凭证类型", idr.getVchtype(), true);
        showInfoList.add(showInfo);
        showInfo = new ShowInfoVo("voucherDate", "出票日期", idr.getVchdate(), true);
        showInfoList.add(showInfo);
        showInfo = new ShowInfoVo("sealType", "验印类型", "1", true);
        // 数据库: 0印鉴 1公章 , 前端: 1印鉴 2公章
        if ("1".equals(idr.getSupervisorcachet())) {
            showInfo.setValue("2");
        }
        showInfoList.add(showInfo);
        /**
         * 各系统定制数据
         */
        //不是银企对账 走下边
        if (idr.getEventid() != 3008) {
            showInfo = new ShowInfoVo("amount", "金额", String.valueOf(idr.getAmount()), true);
            showInfoList.add(showInfo);

            showInfo = new ShowInfoVo("voucherType", "币种", "CNY", true);
            showInfoList.add(showInfo);

            showInfo = new ShowInfoVo("amountArea", "金额区间", null, true);
            showInfoList.add(showInfo);

            showInfo = new ShowInfoVo("sealContails", "印鉴组合", null, true);
            showInfoList.add(showInfo);
        }

    }

}
