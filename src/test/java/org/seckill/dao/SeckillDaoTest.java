package org.seckill.dao;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.seckill.entity.Seckill;
import org.seckill.util.UnitTestBase;

public class SeckillDaoTest extends UnitTestBase {

	public SeckillDaoTest() {
		super("classpath:spring/spring-dao.xml");
	}

	@Test
	public void testQueryById() {
		long id = 1000L;
		SeckillDao seckillDao = super.getBean("seckillDao");
		Seckill seckill = seckillDao.queryById(id);
		System.out.println(seckill.getName());
		System.out.println(seckill);
	}

	@Test
	public void testQueryAll() {
		SeckillDao seckillDao = super.getBean("seckillDao");
		List<Seckill> seckills = seckillDao.queryAll(0, 100);
		for (Seckill seckill : seckills) {
			System.out.println(seckill);
		}
	}

	@Test
	public void testReduceNumber() {
		SeckillDao seckillDao = super.getBean("seckillDao");
		Date now = new Date();
		int updateCount = seckillDao.reduceNumber(1000L, now);
		System.out.println("updateCount = " + updateCount);
	}
}
