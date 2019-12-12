package com.youlexuan.order.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.youlexuan.CONSTANT;
import com.youlexuan.entity.PageResult;
import com.youlexuan.mapper.TbOrderItemMapper;
import com.youlexuan.mapper.TbOrderMapper;
import com.youlexuan.mapper.TbPayLogMapper;
import com.youlexuan.order.service.OrderService;
import com.youlexuan.pojo.TbOrder;
import com.youlexuan.pojo.TbOrderExample;
import com.youlexuan.pojo.TbOrderExample.Criteria;
import com.youlexuan.pojo.TbOrderItem;
import com.youlexuan.pojo.TbPayLog;
import com.youlexuan.pojogroup.Cart;
import com.youlexuan.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private TbOrderMapper orderMapper;

	@Autowired
	private TbOrderItemMapper orderItemMapper;

	@Autowired
	private TbPayLogMapper payLogMapper;

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private IdWorker idWorker;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbOrder> findAll() {
		return orderMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbOrder> page=   (Page<TbOrder>) orderMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加 提交订单时调用
	 * 1、将购物车基本信息入库到TBOrder表中
	 * 2、将购物车中购物条目信息入库到tbOrderitem表中
	 *
	 * 3、将redis中购物车删除
	 *
	 * 4、生成了订单，如果是在线支付的话，那么需要生成一条支付日志入库，并将支付日志入redis中
	 * 		userid
	 *
	 * @param  order 是前台传递过来的参数order，携带是共性的数据
	 */
	@Override
	public void add(TbOrder order) {
		List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps(CONSTANT.CART_LIST_REDIS_KEY).get(order.getUserId());
		List orderIdList = new ArrayList(cartList.size());
		double totalMoney = 0.0;
		for (Cart cart:cartList){
			TbOrder tborder = new TbOrder();
			Long orderId = idWorker.nextId();
			tborder.setOrderId(orderId); //使用分布式ID生成器生成的
			orderIdList.add(orderId);

			tborder.setUserId(order.getUserId());//用户名
			tborder.setPaymentType(order.getPaymentType());//支付类型
			tborder.setStatus("1");//状态：未付款
			tborder.setCreateTime(new Date());//订单创建日期
			tborder.setUpdateTime(new Date());//订单更新日期
			tborder.setReceiverAreaName(order.getReceiverAreaName());//地址
			tborder.setReceiverMobile(order.getReceiverMobile());//手机号
			tborder.setReceiver(order.getReceiver());//收货人
			tborder.setSourceType(order.getSourceType());//订单来源
			tborder.setSellerId(cart.getSellerId());//商家ID

			//加工订单项信息
			List<TbOrderItem> orderItemList = cart.getOrderItemList();
			double money=0.0;
			for(TbOrderItem orderItem:orderItemList){
				Long id = idWorker.nextId();
				orderItem.setId(id);
				orderItem.setOrderId(orderId);
				orderItem.setSellerId(cart.getSellerId());
				//加工订单项的总金额
				money += orderItem.getTotalFee().doubleValue();
				orderItem.setTotalFee(new BigDecimal(money));

				orderItemMapper.insertSelective(orderItem);

			}

			totalMoney+=money;

			orderMapper.insertSelective(tborder);



		}
		//如果是在线支付，那么需要入库 入redis中一条支付日志
		if("1".equals(order.getPaymentType())){
			TbPayLog tbPayLog = new TbPayLog();
			String ids = orderIdList.toString().replace("[","").replace("]","").replace(" ","");
			tbPayLog.setOrderList(ids);//该支付，涉及到了那几个订单
			Long toltalFee = (long)(totalMoney*100);
			tbPayLog.setTotalFee(toltalFee);//该支付日志的总金额，总金额分
			String outTradeNo=  idWorker.nextId()+"";//支付订单号
			tbPayLog.setOutTradeNo(outTradeNo);//支付订单号
			tbPayLog.setCreateTime(new Date());//创建时间
			//订单号列表，逗号分隔
			tbPayLog.setPayType("1");//支付类型
			tbPayLog.setTradeState("0");//支付状态
			tbPayLog.setUserId(order.getUserId());//用户ID
			payLogMapper.insert(tbPayLog);//插入到支付日志表

			redisTemplate.boundHashOps(CONSTANT.PAY_LOG_KEY).put(order.getUserId(),tbPayLog);
		}

		redisTemplate.boundHashOps(CONSTANT.CART_LIST_REDIS_KEY).delete(order.getUserId());
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbOrder order){
		orderMapper.updateByPrimaryKey(order);
	}	
	
	/**
	 * 根据ID获取实体
	 * @return
	 */
	@Override
	public TbOrder findOne(Long orderId){
		return orderMapper.selectByPrimaryKey(orderId);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] orderIds) {
		for(Long orderId:orderIds){
			orderMapper.deleteByPrimaryKey(orderId);
		}		
	}
	
	
		@Override
	public PageResult findPage(TbOrder order, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbOrderExample example=new TbOrderExample();
		Criteria criteria = example.createCriteria();
		
		if(order!=null){			
						if(order.getPaymentType()!=null && order.getPaymentType().length()>0){
				criteria.andPaymentTypeLike("%"+order.getPaymentType()+"%");
			}			if(order.getPostFee()!=null && order.getPostFee().length()>0){
				criteria.andPostFeeLike("%"+order.getPostFee()+"%");
			}			if(order.getStatus()!=null && order.getStatus().length()>0){
				criteria.andStatusLike("%"+order.getStatus()+"%");
			}			if(order.getShippingName()!=null && order.getShippingName().length()>0){
				criteria.andShippingNameLike("%"+order.getShippingName()+"%");
			}			if(order.getShippingCode()!=null && order.getShippingCode().length()>0){
				criteria.andShippingCodeLike("%"+order.getShippingCode()+"%");
			}			if(order.getUserId()!=null && order.getUserId().length()>0){
				criteria.andUserIdLike("%"+order.getUserId()+"%");
			}			if(order.getBuyerMessage()!=null && order.getBuyerMessage().length()>0){
				criteria.andBuyerMessageLike("%"+order.getBuyerMessage()+"%");
			}			if(order.getBuyerNick()!=null && order.getBuyerNick().length()>0){
				criteria.andBuyerNickLike("%"+order.getBuyerNick()+"%");
			}			if(order.getBuyerRate()!=null && order.getBuyerRate().length()>0){
				criteria.andBuyerRateLike("%"+order.getBuyerRate()+"%");
			}			if(order.getReceiverAreaName()!=null && order.getReceiverAreaName().length()>0){
				criteria.andReceiverAreaNameLike("%"+order.getReceiverAreaName()+"%");
			}			if(order.getReceiverMobile()!=null && order.getReceiverMobile().length()>0){
				criteria.andReceiverMobileLike("%"+order.getReceiverMobile()+"%");
			}			if(order.getReceiverZipCode()!=null && order.getReceiverZipCode().length()>0){
				criteria.andReceiverZipCodeLike("%"+order.getReceiverZipCode()+"%");
			}			if(order.getReceiver()!=null && order.getReceiver().length()>0){
				criteria.andReceiverLike("%"+order.getReceiver()+"%");
			}			if(order.getInvoiceType()!=null && order.getInvoiceType().length()>0){
				criteria.andInvoiceTypeLike("%"+order.getInvoiceType()+"%");
			}			if(order.getSourceType()!=null && order.getSourceType().length()>0){
				criteria.andSourceTypeLike("%"+order.getSourceType()+"%");
			}			if(order.getSellerId()!=null && order.getSellerId().length()>0){
				criteria.andSellerIdLike("%"+order.getSellerId()+"%");
			}	
		}
		
		Page<TbOrder> page= (Page<TbOrder>)orderMapper.selectByExample(example);		
		return new PageResult(page.getTotal(), page.getResult());
	}
	
}
