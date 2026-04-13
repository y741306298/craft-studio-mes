package com.mes.domain.shared.utils;

import java.util.UUID;

/**
 * ID 生成器工具类
 */
public class IdGenerator {

    /**
     * 生成唯一的制造商元数据 ID
     * 格式：MM + 时间戳 + 6 位随机数
     * @return 唯一的 manufacturerMetaId
     */
    public static String generateManufacturerMetaId() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "MM" + timestamp + randomCode;
    }

    /**
     * 生成唯一的订单 ID
     * 格式：ORD + 日期 + 6 位随机数
     * @return 唯一的 orderId
     */
    public static String generateOrderId() {
        String date = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String randomCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "ORD" + date + randomCode;
    }

    /**
     * 生成唯一的订单项 ID
     * 格式：ITEM + 时间戳 + 6 位随机数
     * @return 唯一的 orderItemId
     */
    public static String generateOrderItemId() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "ITEM" + timestamp + randomCode;
    }

    /**
     * 生成通用的唯一 ID
     * 格式：前缀 + 时间戳 + 6 位随机数
     * @param prefix ID 前缀
     * @return 唯一的 ID
     */
    public static String generateId(String prefix) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix + timestamp + randomCode;
    }
}
