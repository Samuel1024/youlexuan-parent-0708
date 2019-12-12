package com.youlexuan.seckill.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.youlexuan.CONSTANT;
import com.youlexuan.entity.PageResult;
import com.youlexuan.mapper.TbSeckillGoodsMapper;
import com.youlexuan.mapper.TbSeckillOrderMapper;
import com.youlexuan.pojo.TbSeckillGoods;
import com.youlexuan.pojo.TbSeckillOrder;
import com.youlexuan.pojo.TbSeckillOrderExample;
import com.youlexuan.pojo.TbSeckillOrderExample.Criteria;
import com.youlexuan.seckill.service.SeckillOrderService;
import com.youlexuan.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;
import java.util.List;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

	@Autowired
	private TbSeckillOrderMapper seckillOrderMapper;

	@Autowired
	private TbSeckillGoodsMapper seckillGoodsMapper;

	@Autowired
	private IdWorker idWorker;

	@Autowired
	private RedisTemplate redisTemplate;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbSeckillOrder> findAll() {
		return seckillOrderMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbSeckillOrder> page=   (Page<TbSeckillOrder>) seckillOrderMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbSeckillOrder seckillOrder) {
		seckillOrderMapper.insert(seckillOrder);		
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbSeckillOrder seckillOrder){
		seckillOrderMapper.updateByPrimaryKey(seckillOrder);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbSeckillOrder findOne(Long id){
		return seckillOrderMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			seckillOrderMapper.deleteByPrimaryKey(id);
		}		
	}
	
	
		@Override
	public PageResult findPage(TbSeckillOrder seckillOrder, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbSeckillOrderExample example=new TbSeckillOrderExample();
		Criteria criteria = example.createCriteria();
		
		if(seckillOrder!=null){			
						if(seckillOrder.getUserId()!=null && seckillOrder.getUserId().length()>0){
				criteria.andUserIdLike("%"+seckillOrder.getUserId()+"%");
			}			if(seckillOrder.getSellerId()!=null && seckillOrder.getSellerId().length()>0){
				criteria.andSellerIdLike("%"+seckillOrder.getSellerId()+"%");
			}			if(seckillOrder.getStatus()!=null && seckillOrder.getStatus().length()>0){
				criteria.andStatusLike("%"+seckillOrder.getStatus()+"%");
			}			if(seckillOrder.getReceiverAddress()!=null && seckillOrder.getReceiverAddress().length()>0){
				criteria.andReceiverAddressLike("%"+seckillOrder.getReceiverAddress()+"%");
			}			if(seckillOrder.getReceiverMobile()!=null && seckillOrder.getReceiverMobile().length()>0){
				criteria.andReceiverMobileLike("%"+seckillOrder.getReceiverMobile()+"%");
			}			if(seckillOrder.getReceiver()!=null && seckillOrder.getReceiver().length()>0){
				criteria.andReceiverLike("%"+seckillOrder.getReceiver()+"%");
			}			if(seckillOrder.getTransactionId()!=null && seckillOrder.getTransactionId().length()>0){
				criteria.andTransactionIdLike("%"+seckillOrder.getTransactionId()+"%");
			}	
		}
		
		Page<TbSeckillOrder> page= (Page<TbSeckillOrder>)seckillOrderMapper.selectByExample(example);		
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 *
	 * @param seckillId 得到秒杀的商品，加工订单的相关信息【id】
	 * @param userId ：将加工好的订单信息放到redis中
	 */
	@Override
	public void submitOrder(Long seckillId, String userId) {

		//--------------------------加工商品信息----------------------------
		//得到商品的信息
		TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).get(seckillId);
		if(seckillGoods==null){
			throw  new RuntimeException("该秒杀商品不存在");
		}
		if(seckillGoods.getStockCount()<=0){
			throw new RuntimeException("该秒杀商品库存不足");
		}

		//修改库存
		seckillGoods.setStockCount(seckillGoods.getStockCount()-1);
		//将计算后的库存数据放到redis中
		redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).put(seckillId,seckillGoods);

		//当某个用户秒杀到了最后一件商品时，那么将商品的库存信息入库，并将Redis中该商品删除
		if(seckillGoods.getStockCount()<=0){
			seckillGoodsMapper.updateByPrimaryKey(seckillGoods);
			redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).delete(seckillId);
		}

		//----------------------------加工订单信息--------------------------
		TbSeckillOrder seckillOrder = new TbSeckillOrder();
		seckillOrder.setId(idWorker.nextId());
		seckillOrder.setCreateTime(new Date());
		seckillOrder.setMoney(seckillGoods.getCostPrice());
		seckillOrder.setSeckillId(seckillId);
		seckillOrder.setStatus("0");
		seckillOrder.setUserId(userId);
		seckillOrder.setSellerId(seckillGoods.getSellerId());

		redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_LIST_KEY).put(userId,seckillOrder);




	}

	@Override
	public TbSeckillOrder findSeckillOrder(String userId) {
		TbSeckillOrder  seckillOrder = (TbSeckillOrder) redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_LIST_KEY).get(userId);
		if(seckillOrder==null){
			throw  new RuntimeException("秒杀订单不存在");
		}
		return seckillOrder;
	}

	/**
	 * 支付成功以后，将Redis中的数据落地到mysql中
	 * @param userId
	 * @param orderId
	 * @param trade_no
	 */
	@Override
	public void saveFromRedisToDb(String userId, String orderId, String trade_no) {
		TbSeckillOrder  seckillOrder = (TbSeckillOrder)redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_LIST_KEY).get(userId);
		if(seckillOrder == null){
			throw  new RuntimeException("秒杀订单不存在");
		}
		if(!orderId.equals(seckillOrder.getId()+"")){
			throw  new RuntimeException("订单不符合要求");
		}
		//补充订单信息
		seckillOrder.setPayTime(new Date());
		seckillOrder.setTransactionId(trade_no);
		seckillOrder.setStatus("1");
		seckillOrderMapper.insert(seckillOrder);
		redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_LIST_KEY).delete(userId);
	}

	@Override
	public void deleteOrderFromRedis(String userId, String orderId) {

		//--------------------删除订单-------------------------
		TbSeckillOrder  seckillOrder = (TbSeckillOrder)redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_LIST_KEY).get(userId);
		if(seckillOrder == null){
			throw  new RuntimeException("秒杀订单不存在");
		}
		if(!orderId.equals(seckillOrder.getId()+"")){
			throw  new RuntimeException("订单不符合要求");
		}
		redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_LIST_KEY).delete(userId);

		//---------------------更改库存--------------------
		TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).get(seckillOrder.getSeckillId());
		if(seckillGoods!=null){
			seckillGoods.setStockCount(seckillGoods.getStockCount()+1);
			redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).put(seckillOrder.getSeckillId(),seckillGoods);
		}
	}

}
