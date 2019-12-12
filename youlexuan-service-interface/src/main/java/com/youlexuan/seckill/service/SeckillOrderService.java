package com.youlexuan.seckill.service;
import com.youlexuan.entity.PageResult;
import com.youlexuan.pojo.TbSeckillOrder;

import java.util.List;
/**
 * 服务层接口
 * @author Administrator
 *
 */
public interface SeckillOrderService {

	/**
	 * 返回全部列表
	 * @return
	 */
	public List<TbSeckillOrder> findAll();
	
	
	/**
	 * 返回分页列表
	 * @return
	 */
	public PageResult findPage(int pageNum, int pageSize);
	
	
	/**
	 * 增加
	*/
	public void add(TbSeckillOrder seckill_order);
	
	
	/**
	 * 修改
	 */
	public void update(TbSeckillOrder seckill_order);
	

	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	public TbSeckillOrder findOne(Long id);
	
	
	/**
	 * 批量删除
	 * @param ids
	 */
	public void delete(Long[] ids);

	/**
	 * 分页
	 * @param pageNum 当前页 码
	 * @param pageSize 每页记录数
	 * @return
	 */
	public PageResult findPage(TbSeckillOrder seckill_order, int pageNum, int pageSize);

	/**
	 * 提交秒杀订单
	 * seckillId:秒杀的商品的ID
	 * userId：提交了订单，需要记录该订单是谁的订单
	 */
	public  void submitOrder(Long seckillId,String userId);

	TbSeckillOrder findSeckillOrder(String userId);

    void saveFromRedisToDb(String userId, String orderId, String trade_no);

	void deleteOrderFromRedis(String userId, String out_trade_no);
}
