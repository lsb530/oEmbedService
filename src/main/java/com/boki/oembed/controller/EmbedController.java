package com.boki.oembed.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class EmbedController {

    private static List<String> list = null;

    @GetMapping
    public String home(Model model) throws IOException {
        initProviders();
        return "home";
    }

    @GetMapping("/oembedResponse")
    @ResponseBody
    public String oembedResponse(@RequestParam("userUrlData") String userUrlData) throws Exception {
        String result = "";
        String host = hostCheck(userUrlData);
        String encode = URLEncoder.encode(userUrlData, StandardCharsets.UTF_8);
        String oembedUrl = createAddr(host, encode);

        CloseableHttpClient hc = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(oembedUrl);

        httpGet.addHeader("Content-Type","application/json");
        CloseableHttpResponse httpResponse = hc.execute(httpGet);
        result = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
        return result;
    }

    private static void initProviders() throws IOException {
        list = new ArrayList<>();
        JSONParser jsonParser = new JSONParser();
        // AWS 빌드환경(jar)-> jar:file
        // ClassPathResource classPathResource = new ClassPathResource("dir/providers.json");
        ClassPathResource classPathResource = new ClassPathResource("providers.json");
        BufferedReader rd = new BufferedReader(
            new InputStreamReader(classPathResource.getInputStream()));
        try {
            Object obj = jsonParser.parse(rd);
            JSONArray jsonArr = (JSONArray) obj;
            for (int i = 0; i < jsonArr.size(); i++) {
                JSONObject provider_url = (JSONObject) jsonArr.get(i);
                String url = (String) provider_url.get("endpoints").toString();
                Object obj2 = jsonParser.parse(url);
                JSONArray jsonArray = new JSONArray();
                jsonArray = (JSONArray) obj2;
                JSONObject urlData = (JSONObject) jsonArray.get(0);
                String value = (String) urlData.get("url");
                list.add(value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String hostCheck(String str) throws MalformedURLException {
        URL url = new URL(str);
        String[] split = url.getHost().split("\\.");
        return (split.length == 2) ? split[0] : split[1];
    }

    private String createAddr(String host, String encode) {
        String oembedUrl = "";
        for (String str : list) {
            if (str.contains(host)) {
                if (str.contains("oembed.")) {
                    if (str.contains("{format}")) {
                        str = str.replace("{format}", "json");
                    }
                    oembedUrl = str + "?url=" + encode;
                } else if (str.contains("_oembed")) {
                    oembedUrl = "";
                } else {
                    oembedUrl = str + "?format=json&url=" + encode;
                }
                break;
            }
        }
        return oembedUrl;
    }

}