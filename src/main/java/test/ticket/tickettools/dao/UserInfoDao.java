package test.ticket.tickettools.dao;

import org.springframework.stereotype.Repository;
import test.ticket.tickettools.domain.entity.UserInfoEntity;

import java.util.List;

@Repository
public interface UserInfoDao {
    Integer insertOrUpdate(UserInfoEntity userInfoEntity);
    List<UserInfoEntity> select(UserInfoEntity userInfoEntity);
    UserInfoEntity selectById(Long id);
}
