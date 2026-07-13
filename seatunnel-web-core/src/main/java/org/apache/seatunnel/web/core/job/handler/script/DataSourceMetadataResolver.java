package org.apache.seatunnel.web.core.job.handler.script;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.web.dao.entity.DataSource;
import org.apache.seatunnel.web.dao.repository.DataSourceDao;
import org.apache.seatunnel.web.spi.enums.DbType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSourceMetadataResolver {

    private final DataSourceDao dataSourceDao;

    public DbType resolveDbType(Long datasourceId) {
        if (datasourceId == null || datasourceId <= 0) {
            return null;
        }

        try {
            DataSource dataSource = dataSourceDao.queryById(datasourceId);
            if (dataSource == null) {
                log.warn("Can not find datasource by id, datasourceId={}", datasourceId);
                return null;
            }

            return dataSource.getDbType();
        } catch (Exception e) {
            log.warn("Resolve datasource dbType failed, datasourceId={}", datasourceId, e);
            return null;
        }
    }
}