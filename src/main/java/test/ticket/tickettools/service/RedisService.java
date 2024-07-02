package test.ticket.tickettools.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RedisService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public void saveList(String key, List<String> list) {
        // 清空列表再保存，以避免重复数据
        redisTemplate.delete(key);
        redisTemplate.opsForList().rightPushAll(key, list);
    }

    public boolean deleteKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public void setData(String key,String value){
        redisTemplate.opsForSet().add(key,value);
    }

    public List<String> getList(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public void removeFromList(String key, String value) {
        // 第二个参数 count 为 1 表示移除第一个匹配的元素
        redisTemplate.opsForList().remove(key, 1, value);
    }
}
