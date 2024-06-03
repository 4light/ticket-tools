package test.ticket.tickettools.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.dao.UserDao;
import test.ticket.tickettools.domain.entity.UserEntity;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MyUserDetailsService implements UserDetailsService {
    private final ConcurrentHashMap<String, UserDetails> usersCache = new ConcurrentHashMap<>();

    @Resource
    UserDao userDao;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 先从缓存中查找
        UserDetails cachedUser = usersCache.get(username);
        if (cachedUser != null) {
            return cachedUser;
        }
        UserEntity byUsername = userDao.findByUsername(username);// 如果缓存中没有，则从数据库加载（这里模拟数据库查询）
        if (!ObjectUtils.isEmpty(byUsername)) {
            UserDetails user = new User(byUsername.getUserName(), byUsername.getPwd(), new ArrayList<>());
            usersCache.put(username, user); // 加载后放入缓存
            return user;
        } else {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }
}

