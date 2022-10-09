package run.halo.app.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import run.halo.app.factory.HttpClientFactory;

import java.io.File;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Copyright © 2019年 erciyuanboot. All rights reserved.
 *
 * @author 古今
 * <p>
 * xxxxx类
 * @date 2019/09/25
 * <p>
 * Modification History:
 * Date     Author    Version      Description
 * ---------------------------------------------------------*
 * 2019/09/25   古今   v1.0.0       新增
 */
public class PixivUtil {

    private static final String REGEXP_PAGE = "^[1-9]{1}$";

    private static final String REGEXP_WORKID = "^[0-9]{8}$";
    // 编码
    private static final String ECODING = "UTF-8";
    // 获取img标签正则
    private static final String IMGURL_REG = "<img.*src=(.*?)[^>]*?>";
    // 获取src路径的正则
    private static final String IMGSRC_REG = "http:\"?(.*?)(\"|>|\\s+)";

    private static final String RANKURL_PREFIX = "https://www.pixiv.net/ranking.php?";

    private static final String RANKURL_SUFFIX = "&format=json";

    private static final String DETAILIMGURL_PREFIX = "https://www.pixiv.net/ajax/illust/";

    /**
     * 获取图片列表
     *
     * @param url 请求url
     * @return 图片列表
     */
    public static List<Map<String, Object>> getImgList(String url) {
        return Download.getImgList(getCookie(), url);
    }

    /**
     * 获取大图
     *
     * @param url 请求url
     * @return 图片url
     */
    public static String getImageUrl(String url) {
        return Download.getImageUrl(getCookie(), url);
    }

    /**
     * 获取p站登录cookies
     *
     * @return cookies
     */
    private static String getCookie() {
        return "first_visit_datetime_pc=2021-12-13+16%3A06%3A54; p_ab_id=6; p_ab_id_2=7; " +
            "p_ab_d_id=1787087655; yuid_b=aTE0V0A; __utma=235335808.128325418.1639379236" +
            ".1639379236.1639379236.1; privacy_policy_agreement=3; privacy_policy_notification=0;" +
            " a_type=0; b_type=1; __utmv=235335808" +
            ".|2=login%20ever=no=1^3=plan=normal=1^5=gender=male=1^6=user_id=35260645=1^9=p_ab_id" +
            "=6=1^10=p_ab_id_2=7=1^11=lang=zh=1; " +
            "ki_t=1639379376412%3B1639379376412%3B1639379376412%3B1%3B1; ki_r=; _gcl_au=1.1" +
            ".688490101.1655342279; _ga=GA1.1.128325418.1639379236; " +
            "__cf_bm=rbtpPC1NdSdnp3i4qbmLb_PkzPVDMYzbwYbHsiR0Nqg-1655342279-0-AbfcSe4F2Zhu" +
            "+k9VErCw8NstkU3JWjqmMtyHZuxq1KSQHLQGyohtbq9bnta3PZt5y+f9HGAzbuUKWujROj25B" +
            "+lN138C7tYCDP4FRzHMICjQI2lmsfugRux+g2j5ICEV4Zuyc0HEtsT" +
            "/02989DlAkLJDEVvN9pqvZt5PDQt6otTuoswFM425zNork1V0/eHdcg==; _fbp=fb.1.1655342279625" +
            ".1715576137; PHPSESSID=35260645_BiIzyjRFgtkl8DZ4cTTTN58KfJdWBmBd; " +
            "device_token=8353e0f4ec9b0bbbf2a0b5c62764326e; c_type=25; _ga_75BBYNYN9J=GS1.1" +
            ".1655342279.1.1.1655342312.0; QSI_S_ZN_5hF4My7Ad6VNNAi=v:0:0";
    }

    /**
     * @Description: 获取每日排行url
     * @return:
     * @Author: gj
     * @Date: 2019/9/25
     */
    public static String getRankUrl(String page, String mode, String sort) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(RANKURL_PREFIX);
        if (mode != null) {
            stringBuffer.append("mode=" + mode);
        }
        if (sort != null) {
            stringBuffer.append("&content=" + sort);
        }
        if (page != null) {
            stringBuffer.append("&p=" + page);
        }
        stringBuffer.append(RANKURL_SUFFIX);
        return stringBuffer.toString();
    }


    /**
     * @Description: 获取作品详情url
     * @return:
     * @Author: gj
     * @Date: 2019/9/25
     */
    public static String getDetailUrl(String workId) {
        return DETAILIMGURL_PREFIX + workId;
    }

    /**
     * 获取图片流
     *
     * @param imgSrc 图片url
     * @param requestUrl 请求Url(来源url)
     * @return InputStream
     */
    public static InputStream getImgInputStream(String imgSrc, String requestUrl) {
        return Download.getImgInputStream(imgSrc, requestUrl);
    }

    private static class Download {
        private static List<Map<String, Object>> getImgList(String cookie, String url) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("cookie", cookie);
            httpGet.setHeader("Referer", url);
            httpGet.setHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                    " Chrome/89.0.4389.128 Safari/537.36 Edg/89.0.774.77");
            HttpResponse httpResponse = null;
            String responseBody = null;
            try {
                httpResponse = HttpClientFactory.httpClient.execute(httpGet);
                responseBody = EntityUtils.toString(httpResponse.getEntity());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            if (jsonObject.get("error") != null) {
                throw new RuntimeException((String) jsonObject.get("error"));
            }
            return (List<Map<String, Object>>) jsonObject.get("contents");
        }

        private static String getImageUrl(String cookie, String url) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("cookie", cookie);
            httpGet.setHeader("Referer", url);
            httpGet.setHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                    " Chrome/89.0.4389.128 Safari/537.36 Edg/89.0.774.77");
            HttpResponse httpResponse = null;
            String responseBody = null;
            try {
                httpResponse = HttpClientFactory.httpClient.execute(httpGet);
                responseBody = EntityUtils.toString(httpResponse.getEntity());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            if (jsonObject.get("error") != null) {
                if ((Boolean) jsonObject.get("error")) {
                    throw new RuntimeException((String) jsonObject.get("message"));
                }
            }
            JSONObject jsonObjectBody = (JSONObject) jsonObject.get("body");
            Map<String, String> urlsMap = (Map<String, String>) jsonObjectBody.get("urls");
            return urlsMap.get("original");
        }

        /***
         * 下载图片
         *
         */
        private static InputStream getImgInputStream(String imgSrc, String requestUrl) {
            InputStream in = null;
            try {
                URL uri = new URL(imgSrc);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890));
                URLConnection urlConnection = uri.openConnection(proxy);
                urlConnection.setRequestProperty("Referer", requestUrl);
                in = urlConnection.getInputStream();
                return in;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }
}