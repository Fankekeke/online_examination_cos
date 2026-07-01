package com.onlineexamsystem.backend.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.onlineexamsystem.backend.common.util.DateTimeUtils;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = DateTimeUtils.utcNow();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, DateTimeUtils.utcNow());
    }
}
