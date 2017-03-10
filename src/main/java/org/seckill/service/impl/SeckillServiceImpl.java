package org.seckill.service.impl;

import java.util.Date;
import java.util.List;
import org.seckill.dao.SeckillDao;
import org.seckill.dao.SuccessKilledDao;
import org.seckill.dao.cache.RedisDao;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.entity.SuccessKilled;
import org.seckill.enums.SeckillStatEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

// @Component 所有组件，当我们不知道这个是Dao，service还是controller的时候，使用这个
@Service
public class SeckillServiceImpl implements SeckillService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	// 注入Service依赖
	@Autowired // Autowired是spring提供的，@Resource,@Inject是j2ee规范的
	private SeckillDao seckillDao;

	@Autowired
	private SuccessKilledDao successKilledDao;
	
	// 注入redisdao
	@Autowired
	private RedisDao redisDao;

	// md5盐值字符串，用于混淆md5
	private final String slat = "fdhaklsdf235146df%!~s$#$daf9df2349";

	public List<Seckill> getSeckillList() {
		return seckillDao.queryAll(0, 100);
	}

	public Seckill getById(long seckillId) {
		return seckillDao.queryById(seckillId);
	}

	public Exposer exportSeckillUrl(long seckillId) {
		// 先拿到秒杀的Seckill
		// 优化:使用redis缓存方式，数据的一致性建立在超时的基础上
		/**
		 * get from cache
		 * if null
		 * 	get db
		 * 	put cache
		 * else
		 * 	return 
		 */
		// 1:访问缓存
		Seckill seckill = redisDao.getSeckill(seckillId);
		if (null == seckill) {// 2:缓存中没有找到，则去数据库中读取
			seckill = seckillDao.queryById(seckillId);
			// 如果为空,直接返回秒杀失败
			if (seckill == null) {
				return new Exposer(false, seckillId);
			} else {// 3:将结果放入缓存中
				redisDao.putSeckill(seckill);
			}
		}
		// 拿到秒杀开始时间
		Date startTime = seckill.getStartTime();
		// 拿到秒杀结束时间
		Date endTime = seckill.getEndTime();
		// 当前系统时间
		Date nowTime = new Date();
		// 判断系统时间和秒杀开始时间以及结束时间
		if (nowTime.getTime() < startTime.getTime() || nowTime.getTime() > endTime.getTime()) {
			return new Exposer(false, seckillId, nowTime.getTime(), startTime.getTime(), endTime.getTime());
		}
		// 系统当前时间在秒杀时间范围内，则返回秒杀对象
		String md5 = getMD5(seckillId); // 转化特定字符串过程，不可逆
		return new Exposer(true, md5, seckillId);
	}

	/**
	 * 使用注解控制事物方法的优点： 1.开发团队达成一致约定，明确标注事物方法的编程风格
	 * 2.保证事物方法的执行时间尽可能短，不要穿插其他网络操作，如PRC/HTTP请求等，若非要操作，就在该方法的上层在建一个方法
	 * 3.不是所有的方法都需要事物，如，只有一条修改操作，只读操作等，可能会造成命名误解
	 */
	@Transactional
	public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
			throws SeckillException, RepeatKillException, SeckillCloseException {
		if (md5 == null || !md5.equals(getMD5(seckillId))) {
			throw new SeckillException("seckill data rewrite");
		}
		// 执行秒杀逻辑： 减库存 + 记录购买行为
		Date nowTime = new Date();	//取秒杀时间，取当前系统时间
		try {
			// 减库存
			int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
			if (updateCount <= 0) {
				// 没有减到库存，秒杀结束
				throw new SeckillCloseException("seckill is closed");
			} else {
				// 记录购买行为
				int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
				// 唯一：seckilled , userPhone
				if (insertCount <= 0) {
					// 重复秒杀
					throw new RepeatKillException("seckill repeated");
				} else {
					// 秒杀成功
					SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
					return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
				}
			}
		} catch (SeckillCloseException e1) {	//秒杀关闭
			throw e1;
		} catch (RepeatKillException e2) {	//重复秒杀
			throw e2;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			// 所有编译期异常转化为运行期异常
			throw new SeckillException("seckill inner error:" + e.getMessage());
		}
	}

	// 生成md5串儿
	private String getMD5(long seckillId) {
		String base = seckillId + "/" + slat;
		String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
		return md5;
	}
}
