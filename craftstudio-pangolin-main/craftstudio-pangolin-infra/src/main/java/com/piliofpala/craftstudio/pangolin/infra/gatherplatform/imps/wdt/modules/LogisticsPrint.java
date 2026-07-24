package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules;


import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsLabelPrintException;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsCloudPrintData;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsLabel;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Consignee;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class LogisticsPrint {
    private static String token;
    private static Date tokenLastCreateTime;

    private boolean needFlushLoginToken(){
        if(tokenLastCreateTime == null || token==null) {
            return true;
        }
        long diffMs = new Date().getTime() - tokenLastCreateTime.getTime();
        return diffMs > 1000*60*60*1.5;//超过1个半小时
    }

    public LogisticsLabel print(WdtClient wdtClient, String tradeNo) {
        if(needFlushLoginToken()) {
            createLoginToken();
        }
        String tradeId = new TradeQuery().queryTradeIdByTradeNo(
                wdtClient,tradeNo
        );
        var batchId = ordinary(tradeId);
        return getPrintData(batchId);
    }

    private void createLoginToken() {
        try {
            // 1. 创建 URL 对象
            URL url = new URL("https://login.huice.com/open/tm/unified-login/v5/login/password");

            // 2. 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 3. 设置请求方法为 POST
            connection.setRequestMethod("POST");

            // 4. 设置请求头
            connection.setRequestProperty("Content-Type", "application/json");

            // 5. 允许输入输出流
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // 6. 构建 JSON 请求体
            String jsonBody = "{"
                    + "\"mobileAccount\": true,"
                    + "\"account\": \"13349138430\","
                    + "\"password\": \"1qaz@WSX\","
                    + "\"clientType\": \"WEB\","
                    + "\"vid\": \"1784382890440_1a013886be5c175e9bf729b530791575\""
                    + "}";

            // 7. 写入请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 8. 获取响应码
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // 9. 读取响应内容
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();

            System.out.println("Response Body: " + response);

            // 10. 断开连接
            connection.disconnect();

            var respObj = JSONObject.parseObject(response.toString());
            var data = respObj.getJSONObject("data");
            if(data == null){
                throw new LogisticsLabelPrintException("快递打印异常：无法模拟登录");
            }
            var newToken = data.getString("token");
            if(newToken == null || newToken.isBlank()){
                throw new LogisticsLabelPrintException("快递打印异常：无法模拟登录");
            }
            token = newToken;
            tokenLastCreateTime = new Date();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String ordinary(String tradeId) {
        try {
            // 1. 目标 URL
            URL url = new URL("https://erp.huice.com/api/main/oms/logistics/print/async/ordinary");

            // 2. 打开连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 3. 设置请求方法
            conn.setRequestMethod("POST");

            // 4. 设置请求头
            conn.setRequestProperty("Content-Type", "application/json");

            // 5. 设置 Cookie（对应 curl 的 -b 参数）
            String cookie = "X-HC-TOKEN="+token;
            conn.setRequestProperty("Cookie", cookie);

            // 6. 允许输入输出
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // 7. 构建 JSON 请求体
            String jsonBody = "{"
                    + "\"groupId\": \"\","
                    + "\"invoicePrintFlag\": false,"
                    + "\"invoiceTemplateId\": \"\","
                    + "\"pickingListTemplateId\": \"\","
                    + "\"logisticsPrintFlag\": true,"
                    + "\"excludedDataCacheId\": \"\","
                    + "\"mappings\": [{\"configKey\":\"54\",\"printer\":\"Pantum M6200W series\"}],"
                    + "\"pickingOrderNum\": 0,"
                    + "\"pickingPrintFlag\": false,"
                    + "\"printPickingUseLogisticsPrinter\": false,"
                    + "\"sendFlag\": false,"
                    + "\"source\": \"ordinary\","
                    + "\"tradeIds\": [\""+tradeId+"\"],"
                    + "\"separatorPage\": false,"
                    + "\"forbidMultiWarehouse\": true"
                    + "}";

            // 8. 写入请求体
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 9. 获取响应码
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // 10. 读取响应内容
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();

            System.out.println("Response Body: " + response);
            var respObj = JSONObject.parseObject(response.toString());
            // 11. 断开连接
            conn.disconnect();
            return respObj.getJSONObject("data").getString("batchId");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private LogisticsLabel getPrintData(String batchId) throws LogisticsLabelPrintException{
        try {
            // 1. 创建 URL 对象
            URL url = new URL("https://erp.huice.com/api/main/oms/logistics/print/async/getPrintData");

            // 2. 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 3. 设置请求方法为 POST
            connection.setRequestMethod("POST");

            // 4. 设置请求头
            connection.setRequestProperty("Content-Type", "application/json");
            String cookie = "X-HC-TOKEN="+token;
            connection.setRequestProperty("Cookie", cookie);

            // 5. 允许输入输出流
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // 6. 构建 JSON 请求体
            String jsonBody = "{"
                    + "\"batchId\": \""+batchId+"\","
                    + "\"type\": \"ordinary\""
                    + "}";

            // 7. 写入请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 8. 获取响应码
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // 9. 读取响应内容
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();

            System.out.println("Response Body: " + response);

            // 10. 断开连接
            connection.disconnect();

            var respObj = JSONObject.parseObject(response.toString());
            var dataObj = respObj.getJSONObject("data");
            if(dataObj == null){
                throw new LogisticsLabelPrintException("快递打印异常：旺店通服务暂不可用");
            }
            var successResults = dataObj.getJSONArray("successResults");
            if(successResults == null || successResults.size()!=1) {
                throw new LogisticsLabelPrintException(errorReason(batchId));
            }
            var successResult = successResults.getJSONObject(0);
            var logisticsOrderId = successResult.getString("logisticsNo");
            if(logisticsOrderId == null || logisticsOrderId.isBlank()){
                throw new LogisticsLabelPrintException(errorReason(batchId));
            }
            LogisticsLabel logisticsLabel = new LogisticsLabel();
            logisticsLabel.setLogisticsOrderId(logisticsOrderId);
            var customData = successResult.getJSONObject("customData");
            if(customData!=null) {
                Consignee consignee = new Consignee();
                logisticsLabel.setConsignee(consignee);
                consignee.setName(customData.getString("receiver_name"));
                consignee.setPhone(customData.getString("receiver_mobile"));
                Address address = new Address();
                address.setDetailAddress(customData.getString("receiver_address"));
                consignee.setAddress(address);
            }
            var printData = successResult.getString("printData");
            if(printData!=null){
                LogisticsCloudPrintData logisticsCloudPrintData = new LogisticsCloudPrintData();
                logisticsCloudPrintData.setPrintData(printData);
                logisticsLabel.setLogisticsCloudPrintData(logisticsCloudPrintData);
            }
            return logisticsLabel;

        } catch (Exception e) {
            e.printStackTrace();
            throw new LogisticsLabelPrintException(errorReason(batchId));
        }
    }

    //获取错误类型
    private String errorReason(String batchId) {
        try {
            // 1. 创建 URL 对象
            URL url = new URL("https://erp.huice.com/api/main/oms/logistics/print/batchDetail");

            // 2. 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 3. 设置请求方法为 POST
            connection.setRequestMethod("POST");

            // 4. 设置请求头
            connection.setRequestProperty("Content-Type", "application/json");
            String cookie = "X-HC-TOKEN="+token;
            connection.setRequestProperty("Cookie", cookie);

            // 5. 允许输入输出流
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // 6. 构建 JSON 请求体
            String jsonBody = "{"
                    + "\"batchId\": \""+batchId+"\","
                    + "\"type\": \"ordinary\""
                    + "}";

            // 7. 写入请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 8. 获取响应码
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // 9. 读取响应内容
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();

            System.out.println("Response Body: " + response);

            // 10. 断开连接
            connection.disconnect();

            var respObj = JSONObject.parseObject(response.toString());
            var dataObj = respObj.getJSONArray("data");
            if(dataObj == null || dataObj.isEmpty()){
                return "未知错误";
            }
            return dataObj.getJSONObject(0).getString("reason");

        } catch (Exception e) {
            e.printStackTrace();
            return "未知错误";
        }
    }
}
