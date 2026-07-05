package org.apache.seatunnel.web.core.client.policy;

import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.core.exceptions.ServiceException;
import org.apache.seatunnel.web.spi.enums.Status;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class SeaTunnelClientVersionPolicy {

    private final Set<String> supportedVersions =
            new HashSet<>(Collections.singletonList("2.3.13"));

    public void check(String version) {
        if (StringUtils.isBlank(version)) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "SeaTunnel 客户端连接成功，但未获取到版本信息"
            );
        }

        if (supportedVersions.contains(version.trim())) {
            return;
        }

        throw new ServiceException(
                Status.INTERNAL_SERVER_ERROR_ARGS,
                "当前 SeaTunnel 客户端版本为 " + version
                        + "，暂不支持。当前仅支持 "
                        + String.join("、", supportedVersions)
        );
    }
}