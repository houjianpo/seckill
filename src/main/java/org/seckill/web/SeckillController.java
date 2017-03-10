package org.seckill.web;

import java.util.Date;
import java.util.List;

import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.dto.SeckillResult;
import org.seckill.entity.Seckill;
import org.seckill.enums.SeckillStatEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/seckill") // url:/模块/资源/{id}/细分 如：/seckill/list
public class SeckillController {
	private final Logger logger = LoggerFactory.getLogger(SeckillController.class);

	@Autowired
	private SeckillService seckillService;

	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public String list(Model model) {
		// 获取列表页
		List<Seckill> list = seckillService.getSeckillList();
		model.addAttribute("list", list);
		// list.jsp + model = ModelAndView
		return "list";// /WEB-INF/jsp/list.jsp
	}

	@RequestMapping(value = "/{seckillId}/detail", method = RequestMethod.GET)
	public String detail(@PathVariable("seckillId") Long seckillId, Model model) {
		if (null == seckillId) {// 若是seckillId不存在，则跳转回列表页
			return "redirect:/seckill/list";
		}
		Seckill seckill = seckillService.getById(seckillId);
		if (null == seckill) {// seckillId不合法，未找到该对象，则跳转回列表页
			return "forward:/seckill/list";
		}
		// 找到该对象，返回到页面中展示
		model.addAttribute("seckill", seckill);
		return "detail";// /WEB-INF/jsp/detail.jsp
	}

	// 秒杀地址请求 返回json
	@RequestMapping(value = "/{seckillId}/exposer", method = RequestMethod.POST, produces = {
			"application/json;charset=UTF-8" })
	@ResponseBody
	public SeckillResult<Exposer> exposer(@PathVariable("seckillId") Long seckillId) {
		SeckillResult<Exposer> result;
		try {
			Exposer exposer = seckillService.exportSeckillUrl(seckillId);
			result = new SeckillResult<Exposer>(true, exposer);
		} catch (Exception e) {
			e.printStackTrace();
			result = new SeckillResult<Exposer>(false, e.getMessage());
		}
		return result;
	}

	// 秒杀接口 返回json
	@RequestMapping(value = "/{seckillId}/{md5}/execution", method = RequestMethod.POST, produces = {
			"application/json;charset=UTF-8" })
	@ResponseBody
	public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") Long seckillId,
			@PathVariable("md5") String md5, @CookieValue(value = "killPhone", required = false) Long phone) {
		// 使用springMVC的valid也可以
		if (null == phone) {
			return new SeckillResult<SeckillExecution>(false, "未登录");
		}
		SeckillResult<SeckillExecution> result;
		try {
			SeckillExecution execution = seckillService.executeSeckill(seckillId, phone, md5);
			logger.info("秒杀成功 = " + execution.toString());
			result = new SeckillResult<SeckillExecution>(true, execution);
			return result;
		} catch (RepeatKillException e) {
			SeckillExecution execution = new SeckillExecution(seckillId, SeckillStatEnum.REPEAT_KILL);
			logger.info("重复秒杀 = " + execution.toString());
			result = new SeckillResult<SeckillExecution>(true, execution);
			return result;
		} catch (SeckillCloseException e) {
			SeckillExecution execution = new SeckillExecution(seckillId, SeckillStatEnum.END);
			logger.info("秒杀结束 = " + execution.toString());
			result = new SeckillResult<SeckillExecution>(true, execution);
			return result;
		} catch (Exception e) {
			SeckillExecution execution = new SeckillExecution(seckillId, SeckillStatEnum.INNER_ERROR);
			result = new SeckillResult<SeckillExecution>(true, execution);
			return result;
		}
	}

	// 获取系统时间，校对
	@RequestMapping(value = "/time/now", method = RequestMethod.GET)
	@ResponseBody
	public SeckillResult<Long> time() {
		Date now = new Date();
		return new SeckillResult<Long>(true, now.getTime());
	}
}
