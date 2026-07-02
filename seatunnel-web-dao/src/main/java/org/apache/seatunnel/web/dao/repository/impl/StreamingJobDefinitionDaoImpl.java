package org.apache.seatunnel.web.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.NonNull;
import org.apache.seatunnel.web.common.enums.ReleaseState;
import org.apache.seatunnel.web.dao.entity.StreamingJobDefinitionEntity;
import org.apache.seatunnel.web.dao.mapper.StreamingJobDefinitionMapper;
import org.apache.seatunnel.web.dao.repository.BaseDao;
import org.apache.seatunnel.web.dao.repository.StreamingJobDefinitionDao;
import org.apache.seatunnel.web.spi.bean.dto.StreamingJobDefinitionQueryDTO;
import org.apache.seatunnel.web.spi.bean.vo.StreamingJobDefinitionVO;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class StreamingJobDefinitionDaoImpl
        extends BaseDao<StreamingJobDefinitionEntity, StreamingJobDefinitionMapper>
        implements StreamingJobDefinitionDao {

    @Resource
    private StreamingJobDefinitionMapper streamingJobDefinitionMapper;

    public StreamingJobDefinitionDaoImpl(@NonNull StreamingJobDefinitionMapper streamingJobDefinitionMapper) {
        super(streamingJobDefinitionMapper);
    }

    @Override
    public StreamingJobDefinitionEntity queryById(Long id) {
        if (id == null) {
            return null;
        }
        return streamingJobDefinitionMapper.selectById(id);
    }

    @Override
    public void saveOrUpdate(StreamingJobDefinitionEntity entity) {
        if (entity == null) {
            return;
        }

        StreamingJobDefinitionEntity existing = entity.getId() == null
                ? null
                : streamingJobDefinitionMapper.selectById(entity.getId());

        if (existing == null) {
            streamingJobDefinitionMapper.insert(entity);
        } else {
            streamingJobDefinitionMapper.updateById(entity);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) {
            return false;
        }
        return streamingJobDefinitionMapper.deleteById(id) > 0;
    }

    @Override
    public boolean updateReleaseState(Long id, ReleaseState releaseState) {
        if (id == null || releaseState == null) {
            return false;
        }

        LambdaUpdateWrapper<StreamingJobDefinitionEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(StreamingJobDefinitionEntity::getId, id)
                .set(StreamingJobDefinitionEntity::getReleaseState, releaseState);

        return streamingJobDefinitionMapper.update(null, wrapper) > 0;
    }

    @Override
    public List<StreamingJobDefinitionVO> selectPage(
            StreamingJobDefinitionQueryDTO dto,
            int offset,
            int pageSize) {
        if (offset < 0) {
            offset = 0;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }

        List<StreamingJobDefinitionVO> records =
                streamingJobDefinitionMapper.selectPageWithLatestInstance(dto, offset, pageSize);

        return records == null ? Collections.emptyList() : records;
    }

    @Override
    public Long count(StreamingJobDefinitionQueryDTO dto) {
        Long count = streamingJobDefinitionMapper.countPage(dto);
        return count == null ? 0L : count;
    }

    @Override
    public boolean existsByDatasourceId(Long datasourceId) {
        if (datasourceId == null || datasourceId <= 0) {
            return false;
        }

        LambdaQueryWrapper<StreamingJobDefinitionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(StreamingJobDefinitionEntity::getId)
                .and(w -> w
                        .eq(StreamingJobDefinitionEntity::getSourceDatasourceId, datasourceId)
                        .or()
                        .eq(StreamingJobDefinitionEntity::getSinkDatasourceId, datasourceId)
                )
                .last("LIMIT 1");

        return streamingJobDefinitionMapper.selectOne(wrapper) != null;
    }

    @Override
    public List<Long> selectReferencedDatasourceIds(List<Long> datasourceIds) {
        if (datasourceIds == null || datasourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> validIds = datasourceIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        if (validIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<StreamingJobDefinitionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                StreamingJobDefinitionEntity::getSourceDatasourceId,
                StreamingJobDefinitionEntity::getSinkDatasourceId
        )
                .and(w -> w
                        .in(StreamingJobDefinitionEntity::getSourceDatasourceId, validIds)
                        .or()
                        .in(StreamingJobDefinitionEntity::getSinkDatasourceId, validIds)
                );

        List<StreamingJobDefinitionEntity> records = streamingJobDefinitionMapper.selectList(wrapper);
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        return records.stream()
                .flatMap(record -> java.util.stream.Stream.of(
                        record.getSourceDatasourceId(),
                        record.getSinkDatasourceId()
                ))
                .filter(id -> id != null && validIds.contains(id))
                .distinct()
                .collect(Collectors.toList());
    }
}