package test.ticket.tickettools.service;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
        redisTemplate.delete(key);
        redisTemplate.opsForValue().set(key,value);
    }

    public String getData(String key){
        return redisTemplate.opsForValue().get(key);
    }

    public List<String> searchKey(String keyWord){
        List<String> keys=new ArrayList<>();
        try {
            ScanOptions options = ScanOptions.scanOptions().match(keyWord).build();
            Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection().scan(options);
            while (cursor.hasNext()) {
                byte[] key = cursor.next();
                // 处理匹配的键
                String keyStr = new String(key, StandardCharsets.UTF_8);
                keys.add(keyStr);
            }
            cursor.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return keys;
    }

    public List<String> getList(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public void removeFromList(String key, String value) {
        // 第二个参数 count 为 1 表示移除第一个匹配的元素
        redisTemplate.opsForList().remove(key, 1, value);
    }
}
