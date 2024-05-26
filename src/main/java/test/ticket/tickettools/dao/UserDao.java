package test.ticket.tickettools.dao;


import org.springframework.stereotype.Repository;
import test.ticket.tickettools.domain.entity.UserEntity;

@Repository
public interface UserDao {
    UserEntity selectById(Long id);
    Integer insert(UserEntity userEntity);
    Integer updateById(UserEntity userEntity);
    Integer deleteById(Long id);
}
