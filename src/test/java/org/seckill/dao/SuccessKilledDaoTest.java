package org.seckill.dao;

import static org.junit.Assert.*;

import org.junit.Test;
import org.seckill.entity.SuccessKilled;
import org.seckill.util.UnitTestBase;

public class SuccessKilledDaoTest extends UnitTestBase {

	public SuccessKilledDaoTest() {
		super("classpath:spring/spring-dao.xml");
	}

	@Test
	public void testInsertSuccessKilled() {
		/**
		 * 第一次:insertCount = 1 第二次:insertCount = 0
		 */
		long id = 1000L;
		long phone = 13501010202L;
		SuccessKilledDao successKilledDao = super.getBean("successKilledDao");
		int insertCount = successKilledDao.insertSuccessKilled(id, phone);
		System.out.println("insertCount = " + insertCount);
	}

	@Test
	public void testQueryByIdWithSeckill() {
		long id = 1000L;
		long phone = 13501010202L;
		SuccessKilledDao successKilledDao = super.getBean("successKilledDao");
		SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(id, phone);
		System.out.println(successKilled);
	}

}
