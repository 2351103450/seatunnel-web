package org.apache.seatunnel.admin.utils;

import com.typesafe.config.*;

import java.util.*;
import java.util.regex.Pattern;

public final class HoconSensitiveMaskUtil {

    private static final String MASK = "******";

    /**
     * 敏感字段，可按需扩展
     */
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password",
            "passwd",
            "pwd",
            "secret",
            "accessKeySecret",
            "token"
    ));

    /**
     * SeaTunnel HOCON 顶层固定顺序
     */
    private static final List<String> ROOT_ORDER = Arrays.asList(
            "env",
            "source",
            "transform",
            "sink"
    );

    private static final ConfigRenderOptions RENDER_OPTIONS = ConfigRenderOptions.defaults()
            .setComments(false)
            .setFormatted(true)
            .setJson(false)
            .setOriginComments(false);

    private HoconSensitiveMaskUtil() {
    }

    /**
     * 脱敏 HOCON 字符串中的敏感字段，并按固定顶层顺序输出
     */
    public static String maskSensitiveInfo(String hoconText) {
        if (hoconText == null || hoconText.trim().isEmpty()) {
            return hoconText;
        }

        try {
            Config config = ConfigFactory.parseString(hoconText);
            ConfigValue maskedRoot = maskValue(config.root());

            if (maskedRoot != null && maskedRoot.valueType() == ConfigValueType.OBJECT) {
                return renderRootInOrder((ConfigObject) maskedRoot);
            }

            return renderValue(maskedRoot);
        } catch (Exception e) {
            // 解析失败兜底，至少保证 password 不泄露
            return maskByRegex(hoconText);
        }
    }

    /**
     * 递归脱敏
     */
    private static ConfigValue maskValue(ConfigValue value) {
        if (value == null) {
            return null;
        }

        switch (value.valueType()) {
            case OBJECT:
                ConfigObject obj = (ConfigObject) value;
                ConfigObject result = obj;

                for (Map.Entry<String, ConfigValue> entry : obj.entrySet()) {
                    String key = entry.getKey();
                    ConfigValue childValue = entry.getValue();

                    if (isSensitiveKey(key)) {
                        result = result.withValue(key, ConfigValueFactory.fromAnyRef(MASK));
                    } else {
                        ConfigValue maskedChild = maskValue(childValue);
                        if (maskedChild != childValue) {
                            result = result.withValue(key, maskedChild);
                        }
                    }
                }
                return result;

            case LIST:
                ConfigList list = (ConfigList) value;
                List<Object> newList = new ArrayList<>(list.size());
                boolean changed = false;

                for (ConfigValue item : list) {
                    ConfigValue maskedItem = maskValue(item);
                    newList.add(maskedItem == null ? null : maskedItem.unwrapped());
                    if (maskedItem != item) {
                        changed = true;
                    }
                }

                return changed ? ConfigValueFactory.fromIterable(newList) : value;

            default:
                return value;
        }
    }

    /**
     * 顶层按固定顺序渲染，避免 root.render() 打乱 env/source/transform/sink 顺序
     */
    private static String renderRootInOrder(ConfigObject root) {
        StringBuilder sb = new StringBuilder();
        Set<String> renderedKeys = new HashSet<>();

        // 先输出固定顺序
        for (String key : ROOT_ORDER) {
            if (root.containsKey(key)) {
                appendKeyValue(sb, key, root.get(key));
                renderedKeys.add(key);
            }
        }

        // 再输出其余顶层字段
        for (Map.Entry<String, ConfigValue> entry : root.entrySet()) {
            String key = entry.getKey();
            if (!renderedKeys.contains(key)) {
                appendKeyValue(sb, key, entry.getValue());
            }
        }

        return sb.toString().trim();
    }

    /**
     * 追加单个顶层 key-value
     */
    private static void appendKeyValue(StringBuilder sb, String key, ConfigValue value) {
        if (sb.length() > 0) {
            sb.append("\n");
        }
        sb.append(key).append(" ");
        sb.append(renderValue(value));
        sb.append("\n");
    }

    /**
     * 渲染单个 ConfigValue
     */
    private static String renderValue(ConfigValue value) {
        return value == null ? "null" : value.render(RENDER_OPTIONS);
    }

    private static boolean isSensitiveKey(String key) {
        return key != null && SENSITIVE_KEYS.contains(key.trim());
    }

    /**
     * HOCON 解析失败时的兜底方案
     * 兼容：
     * password="xxx"
     * password = "xxx"
     * "password"=xxx
     * password=xxx
     */
    private static String maskByRegex(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        for (String key : SENSITIVE_KEYS) {
            result = result.replaceAll(
                    "(?im)(\"?" + Pattern.quote(key) + "\"?\\s*[=:]\\s*)(\"[^\"]*\"|[^\\s\\r\\n}]+)",
                    "$1\"******\""
            );
        }
        return result;
    }
}