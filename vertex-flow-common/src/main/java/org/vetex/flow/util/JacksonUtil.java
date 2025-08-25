package org.vetex.flow.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Json工具类
 */
public class JacksonUtil {

    private JacksonUtil() {

    }


    private static ObjectMapper mapper = new ObjectMapper();

    {
        // 忽略未知字段，反序列化时不会报错
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 忽略 null 值属性
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 日期格式化
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // 禁用将时间格式化为时间戳
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public static String toJsonOrEmptyString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            return "";
        }
    }

    /** JSON 字符串 → Java 对象 */
    @Nullable
    public static <T> T toObjectOrNull(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON 字符串 → 泛型对象（如 List、Map 等） */
    @Nullable
    public static <T> T toObjectOrNull(String json, TypeReference<T> typeReference) {
        try {
            return mapper.readValue(json, typeReference);
        } catch (IOException e) {
            return null;
        }
    }


}
