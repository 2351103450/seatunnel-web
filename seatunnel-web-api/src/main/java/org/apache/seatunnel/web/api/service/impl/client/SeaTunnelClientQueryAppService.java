package org.apache.seatunnel.web.api.service.impl.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.common.enums.SeaTunnelClientHealthStatusEnum;
import org.apache.seatunnel.web.common.enums.SeaTunnelClientNodeRole;
import org.apache.seatunnel.web.core.exceptions.ServiceException;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClient;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClientNode;
import org.apache.seatunnel.web.dao.repository.SeaTunnelClientDao;
import org.apache.seatunnel.web.dao.repository.SeaTunnelClientNodeDao;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientEndpointDTO;
import org.apache.seatunnel.web.spi.bean.dto.SeaTunnelClientPageDTO;
import org.apache.seatunnel.web.spi.bean.vo.OptionVO;
import org.apache.seatunnel.web.spi.enums.Status;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeaTunnelClientQueryAppService {

    @Resource
    private SeaTunnelClientDao seaTunnelClientDao;

    @Resource
    private SeaTunnelClientNodeDao seaTunnelClientNodeDao;

    @Resource
    private SeaTunnelClientAssembler assembler;

    public List<OptionVO> option() {
        LambdaQueryWrapper<SeaTunnelClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(
                SeaTunnelClient::getHealthStatus,
                SeaTunnelClientHealthStatusEnum.LIVE.getCode()
        );
        wrapper.orderByDesc(SeaTunnelClient::getCreateTime);

        List<SeaTunnelClient> entities = seaTunnelClientDao.selectList(wrapper);

        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(assembler::toOptionVO)
                .collect(Collectors.toList());
    }

    public IPage<SeaTunnelClient> page(SeaTunnelClientPageDTO dto) {
        int pageNo = dto == null || dto.getPageNo() == null || dto.getPageNo() <= 0
                ? 1
                : dto.getPageNo();

        int pageSize = dto == null || dto.getPageSize() == null || dto.getPageSize() <= 0
                ? 10
                : dto.getPageSize();

        LambdaQueryWrapper<SeaTunnelClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SeaTunnelClient::getCreateTime);

        IPage<SeaTunnelClient> page =
                seaTunnelClientDao.selectPage(new Page<>(pageNo, pageSize), wrapper);

        fillClientNodes(page.getRecords());

        return page;
    }

    public List<SeaTunnelClientEndpointDTO> nodes(Long clientId) {
        getEntity(clientId);

        List<SeaTunnelClientNode> nodes =
                seaTunnelClientNodeDao.selectByClientId(clientId);

        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        return nodes.stream()
                .map(assembler::toEndpointDTO)
                .collect(Collectors.toList());
    }

    private void fillClientNodes(List<SeaTunnelClient> clients) {
        if (clients == null || clients.isEmpty()) {
            return;
        }

        for (SeaTunnelClient client : clients) {
            List<SeaTunnelClientNode> nodes =
                    seaTunnelClientNodeDao.selectByClientId(client.getId());

            if (nodes == null || nodes.isEmpty()) {
                client.setMasterEndpoints(Collections.emptyList());
                client.setWorkerEndpoints(Collections.emptyList());
                continue;
            }

            List<SeaTunnelClientEndpointDTO> masters = nodes.stream()
                    .filter(node -> StringUtils.equalsIgnoreCase(
                            node.getNodeRole(),
                            SeaTunnelClientNodeRole.MASTER
                    ))
                    .map(assembler::toEndpointDTO)
                    .collect(Collectors.toList());

            List<SeaTunnelClientEndpointDTO> workers = nodes.stream()
                    .filter(node -> StringUtils.equalsIgnoreCase(
                            node.getNodeRole(),
                            SeaTunnelClientNodeRole.WORKER
                    ))
                    .map(assembler::toEndpointDTO)
                    .collect(Collectors.toList());

            client.setMasterEndpoints(masters);
            client.setWorkerEndpoints(workers);
        }
    }

    private SeaTunnelClient getEntity(Long id) {
        if (id == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端 ID 不能为空"
            );
        }

        SeaTunnelClient entity = seaTunnelClientDao.queryById(id);

        if (entity == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端不存在, id=" + id
            );
        }

        return entity;
    }
}