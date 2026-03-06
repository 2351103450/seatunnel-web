INSERT INTO seatunnel_web.t_seatunnel_datasource_plugin_config
    (id, plugin_type, config_schema, create_time, update_time)
VALUES ('1', 'MYSQL',
        '{"fields":[{"key":"host","label":"主机地址IP","type":"INPUT","required":true,"placeholder":"请输入主机地址IP","defaultValue":"localhost","rules":[{"required":true,"message":"主机地址IP不能为空"}]},{"key":"port","label":"端口号","type":"NUMBER","required":true,"defaultValue":3306,"rules":[{"required":true,"message":"端口号不能为空"}]},{"key":"database","label":"数据库名","type":"INPUT","required":true,"placeholder":"请输入数据库名称","rules":[{"required":true,"message":"数据库名不能为空"}]},{"key":"userPO","label":"用户名","type":"INPUT","required":true,"placeholder":"请输入用户名","rules":[{"required":true,"message":"用户名不能为空"}]},{"key":"password","label":"密码","type":"PASSWORD","required":true,"placeholder":"请输入密码","rules":[{"required":true,"message":"密码不能为空"}]},{"key":"driverLocation","label":"驱动Jar包","type":"INPUT","required":true,"defaultValue":"mysql-connector-java-8.0.29.jar","placeholder":"请输入驱动Jar包","rules":[{"required":true,"message":"的jar包不能为空"}]},{"key":"other","label":"版本","type":"CUSTOM_SELECT","required":false,"defaultValue":[{"key":"useSSL","value":"false"},{"key":"allowPublicKeyRetrieval","value":"true"}]}]}',
        '2025-10-24 17:53:37', '2025-10-24 17:53:37');

INSERT INTO seatunnel_web.t_seatunnel_user
(id, user_name, user_password, user_type, email, phone, create_time, update_time, state)
VALUES(1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 0, '295227940@qq.com', '15002344940', NULL, NULL, 1);