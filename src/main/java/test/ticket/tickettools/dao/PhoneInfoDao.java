package test.ticket.tickettools.dao;

import org.springframework.stereotype.Repository;
import test.ticket.tickettools.domain.entity.PhoneInfoEntity;

@Repository
public interface PhoneInfoDao {
    Integer insert(PhoneInfoEntity phoneInfoEntity);
}
