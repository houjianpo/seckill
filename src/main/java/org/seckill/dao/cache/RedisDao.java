package org.seckill.dao.cache;

import javax.servlet.jsp.tagext.TryCatchFinally;

import org.seckill.entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * redis 缓存工具类
 * 
 * @author houjianpo
 *
 */
public class RedisDao {
	private final Logger logger = LoggerFactory.getLogger(RedisDao.class);

	// redis连接池
	private final JedisPool jedisPool;

	// 构造方法，创建jedis对象
	public RedisDao(String ip, int port) {
		jedisPool = new JedisPool(ip, port);
	}

	// 自定义一个Seckill的字节码文件
	private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);

	// 获取对象
	public Seckill getSeckill(long seckillId) {
		try {
			// 拿到jedis对象
			Jedis jedis = jedisPool.getResource();
			try {
				String key = "seckill:" + seckillId;
				// redis并没有实现内部序列化操作，需要我们来完成,里面保存的是二进制数组
				// get -> byte[] -> 反序列化 -> Object对象(Seckill)
				// 采用自定义序列化方式，protostuff : pojo
				byte[] bytes = jedis.get(key.getBytes());
				// 缓存重获取到
				if (null != bytes) {
					// 空对象
					Seckill seckill = schema.newMessage();
					ProtostuffIOUtil.mergeFrom(bytes, seckill, schema); // redis中的字节数组，要转换的对象，schema
					// seckill 被反序列化
					return seckill;
				}
			} finally {
				jedis.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	// 设置对象
	public String putSeckill(Seckill seckill) {
		// Object(Seckill)对象 -> 序列化 -> byte[] -> 发送到redis
		try {
			// 拿到jedis对象
			Jedis jedis = jedisPool.getResource();
			try {
				// 将对象序列化
				String key = "seckill:" + seckill.getSeckillId();
				byte[] bytes = ProtostuffIOUtil.toByteArray(seckill, schema,
						LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
				// 超时缓存
				int timeout = 60 * 60; // 一小时
				String result = jedis.setex(key.getBytes(), timeout, bytes);
				return result;
			} finally {
				jedis.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
}
