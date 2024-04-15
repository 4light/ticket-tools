package test.ticket.tickettools.dao;

import org.springframework.stereotype.Repository;
import test.ticket.tickettools.domain.entity.UserInfoEntity;

@Repository
public interface UserInfoDao {
    Integer insertOrUpdate(UserInfoEntity userInfoEntity);
    UserInfoEntity select(UserInfoEntity userInfoEntity);
}
