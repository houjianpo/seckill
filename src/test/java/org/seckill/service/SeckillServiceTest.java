package org.seckill.service;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.util.UnitTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeckillServiceTest extends UnitTestBase {
	private final Logger logger = LoggerFactory.getLogger(SeckillServiceTest.class);

	public SeckillServiceTest() {
		super("classpath:spring/spring-dao.xml,spring/spring-service.xml");
	}

	@Test
	public void testGetSeckillList() {
		SeckillService seckillService = super.getBean("seckillServiceImpl");
		List<Seckill> list = seckillService.getSeckillList();
		logger.info("list={}", list);
	}

	@Test
	public void testGetById() {
		SeckillService seckillService = super.getBean("seckillServiceImpl");
		long id = 1000L;
		Seckill seckill = seckillService.getById(id);
		logger.info("seckill={}", seckill);
	}

	@Test
	public void testExportSeckillUrl() {
		SeckillService seckillService = super.getBean("seckillServiceImpl");
		long id = 1000L;
		Exposer exposer = seckillService.exportSeckillUrl(id);
		logger.info("exposer={}", exposer);
		/**
		 * exposer=Exposer [ exposed=true, md5=9c01c4aa68b981b03f37fc1b1ef0c075,
		 * seckillId=1000, now=0, start=0, end=0]
		 */
	}

	@Test
	public void testExecuteSeckill() {
		SeckillService seckillService = super.getBean("seckillServiceImpl");
		long id = 1000L;
		long phone = 15810103020L;
		String md5 = "9c01c4aa68b981b03f37fc1b1ef0c075";
		try {
			SeckillExecution seckillExecution = seckillService.executeSeckill(id, phone, md5);
			logger.info("seckillExecution={}", seckillExecution);
		} catch (RepeatKillException e) {
			e.printStackTrace();
		} catch (SeckillCloseException e) {
			e.printStackTrace();
		}
		/**
		 * seckillExecution=SeckillExecution [ seckillId=1000, state=1,
		 * stateInfo=秒杀成功, successKilled=SuccessKilled [seckillId=1000,
		 * userPhone=15810103020, state=0, createTime=Tue Mar 07 16:23:51 CST
		 * 2017, seckill=Seckill [seckillId=1000, name=1000元秒杀iphone6,
		 * number=97, startTime=Tue Mar 07 16:23:51 CST 2017, endTime=Wed Aug 30
		 * 00:00:00 CST 2017, createTime=Wed Mar 01 16:03:37 CST 2017]]]
		 */
	}
	
	//测试代码完整逻辑，可重复测试
	@Test
	public void testSeckillLogic() {
		SeckillService seckillService = super.getBean("seckillServiceImpl");
		long id = 1001L;
		Exposer exposer = seckillService.exportSeckillUrl(id);
		if (exposer.isExposed()) {
			logger.info("exposer={}", exposer);
			long phone = 15810103020L;
			String md5 = exposer.getMd5();
			try {
				SeckillExecution seckillExecution = seckillService.executeSeckill(id, phone, md5);
				logger.info("seckillExecution={}", seckillExecution);
			} catch (RepeatKillException e) {
				e.printStackTrace();
			} catch (SeckillCloseException e) {
				e.printStackTrace();
			}
		} else {
			// 秒杀未开启
			logger.warn("exposer={}", exposer);
		}
	}

}
