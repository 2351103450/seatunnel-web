package org.apache.seatunnel.plugin.datasource.api.form;

import com.alibaba.fastjson.JSON;
import org.apache.seatunnel.communal.KeyValuePair;
import org.apache.seatunnel.communal.form.FormField;
import org.apache.seatunnel.communal.form.FormFieldConfig;
import org.apache.seatunnel.communal.form.Rule;
import org.apache.seatunnel.communal.utils.JSONUtils;

import java.lang.reflect.Field;
import java.util.*;

public class ReflectionFormGenerator {


    public static List<FormFieldConfig> generate(Class<?> paramClass) {

        List<FormFieldConfig> fields = new ArrayList<>();

        Map<String, Field> fieldMap = new LinkedHashMap<>();

        Class<?> current = paramClass;

        while (current != null && current != Object.class) {

            for (Field field : current.getDeclaredFields()) {

                fieldMap.putIfAbsent(field.getName(), field);
            }

            current = current.getSuperclass();
        }

        for (Field field : fieldMap.values()) {

            FormField formField = field.getAnnotation(FormField.class);

            if (formField == null) {
                continue;
            }

            FormFieldConfig config = new FormFieldConfig();

            config.setKey(field.getName());
            config.setLabel(formField.label());
            config.setPlaceholder(formField.placeholder());
            config.setType(formField.type());

            if (formField.required()) {
                Rule rule = new Rule();
                rule.setRequired(true);
                rule.setMessage(formField.label() + " cannot be empty");
                config.setRules(List.of(rule));
            }

            String defaultValue = formField.defaultValue();
            if (!defaultValue.isEmpty()) {

                if (field.getType() == List.class) {
                    config.setDefaultValue(
                            JSON.parseArray(defaultValue, KeyValuePair.class)
                    );
                } else {
                    config.setDefaultValue(defaultValue);
                }
            }

            config.setOrder(formField.order());

            fields.add(config);
        }

        fields.sort(Comparator.comparing(FormFieldConfig::getOrder));

        return fields;
    }
}