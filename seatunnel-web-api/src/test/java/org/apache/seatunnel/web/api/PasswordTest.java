package org.apache.seatunnel.web.api;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordTest {

    public static void main(String[] args) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        String rawPassword = "123456";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        System.out.println("密文：" + encodedPassword);
        System.out.println("验证：" + passwordEncoder.matches(rawPassword, encodedPassword));
    }
}