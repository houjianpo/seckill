//存放主要交互逻辑js代码
// javaScript模块化
// seckill.detail.init(params)方式调用
var seckill = {
		// 封装秒杀相关ajax的url
		URL : {
			now : function(){
				return '/seckill/seckill/time/now';
			},
			exposer : function(seckillId){
				return '/seckill/seckill/' + seckillId + '/exposer';
			},
			execution : function(seckillId, md5) {
				return '/seckill/seckill/' + seckillId + '/' + md5 + '/execution';
			}
		},
		//处理秒杀逻辑
		handleSeckillkill : function(seckillId, node) {
			//console.log('seckillId = ' + seckillId + ',node = ' + node);
			node.hide()
				.html('<button class="btn btn-primary btn-lg" id="killBtn">开始秒杀</button>');//添加一个秒杀按钮
			//调用接口，获取秒杀信息
			$.post(seckill.URL.exposer(seckillId) , {}, function(result) {
				//在回调逻辑中执行交互逻辑
				if(result && result['success']) {
					var exposer = result['data'];
					if(exposer['exposed']){
						// 开始秒杀
						// 获取秒杀地址
						var md5 = exposer['md5'];
						var killUrl = seckill.URL.execution(seckillId, md5);
						//console.log(killUrl);
						// 绑定一次点击事件，防止用户连续点击按钮，不要使用click
						$('#killBtn').one('click', function(){
							// 执行秒杀请求
							// 1:先禁用按钮
							$(this).addClass('disabled');
							// 2:发送秒杀请求，执行秒杀
							$.post(killUrl, {}, function(result){
								if(result && result['success']) {
									//console.log(JSON.stringify(result['data']));
									var killResult = result['data'];
									var state = killResult['state'];
									var stateInfo = killResult['stateInfo'];
									// 3:显示秒杀结果
									node.html('<span class="label label-success">' + stateInfo + '</span>');
								}
							});
						});
						node.show();
					} else {
						// 未开启秒杀,可能会存在客户端和服务器端的时间误差，在执行一次countDown就行了
						var now = exposer['now'];
						var start = exposer['start'];
						var end = exposer['end'];
						seckill.countDown(seckillId, now, start, end);
					}
				} else {
					console.log('result = ' + result);
				}
			});
		},
		//验证手机号
		validatePhone : function(phone){
			if(phone && phone.length == 11 && !isNaN(phone)){
				return true;
			} else {
				return false;
			}
		},
		//倒计时
		countDown : function(seckillId, nowTime, startTime, endTime){
			//console.log('seckillId = ' + seckillId + ',nowTime = ' + nowTime + ',startTime = ' + startTime + ',endTime = ' + endTime);
			var seckillBox = $('#seckill-box');
			// 时间判断
			if(nowTime > endTime){
				// 秒杀结束
				seckillBox.html('秒杀结束');
			} else if(nowTime < startTime){
				// 秒杀未开始,计时事件绑定
				var killTime = new Date(parseInt(startTime,10) + 1000); //增加1秒，防止时间偏移
				//console.log('killTime = ' + killTime);
				seckillBox.countdown(killTime,function(event){
					// 时间格式
					var format = event.strftime('秒杀倒计时: %D天 %H时 %M分 %S秒');
					seckillBox.html(format);
				}).on('finish.countdown',function(){
					//时间完成后回调事件
					//获取秒杀地址，控制实现秒杀逻辑，执行秒杀
					seckill.handleSeckillkill(seckillId, seckillBox);
				});
			} else{
				// 秒杀开始
				seckill.handleSeckillkill(seckillId, seckillBox);
			}
		},
		// 详情页秒杀逻辑
		detail : {
			// 详情页初始化
			init : function(params) {
				// 手机验证和登陆，计时交互
				// 规划一下我们的交互流程
				// 1:在cookie中查找手机号
				var killPhone = $.cookie('killPhone');
				
				// 验证手机号
				if(!seckill.validatePhone(killPhone)){
					// 绑定手机号
					// 获取弹出层
					var killPhoneModel = $('#killPhoneModel');
					// 显示弹出层
					killPhoneModel.modal({
						show : true, //显示弹出层
						backdrop : 'static', //禁止位置关闭
						keyboard : false //关闭键盘事件
					});
					$('#killPhoneBtn').click(function(){
						var inputPhone = $('#killPhoneKey').val();
						if(seckill.validatePhone(inputPhone)){
							// 将电话号码写入cookie
							$.cookie('killPhone', inputPhone, {expires : 7, path : '/seckill'});
							//刷新页面
							window.location.reload();
						} else {
							$('#killPhoneMessage').hide().html('<label class="label label-danger">手机号错误!</label>').show(300);
						}
					});
				}
				// 已经登录了
				// 计时交互
				var seckillId = params['seckillId'];
				var startTime = params['startTime'];
				var endTime = params['endTime'];
				$.get(seckill.URL.now(), {}, function(result){
					//console.log('result = ' + result);
					if(result && result['success']){
						var nowTime = result['data'];
						// 时间判断，根据传进来的时间来计算
						seckill.countDown(seckillId, nowTime, startTime, endTime);
					} else {
						console.log('result = ' + result);
					}
				});
			}
		}
}