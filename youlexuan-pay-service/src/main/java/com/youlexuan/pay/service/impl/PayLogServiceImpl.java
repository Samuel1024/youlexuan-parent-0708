package com.youlexuan.pay.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.youlexuan.CONSTANT;
import com.youlexuan.mapper.TbOrderMapper;
import com.youlexuan.mapper.TbPayLogMapper;
import com.youlexuan.pay.service.PayLogService;
import com.youlexuan.pojo.TbOrder;
import com.youlexuan.pojo.TbPayLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;

@Service(timeout = 50000)
public class PayLogServiceImpl implements PayLogService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TbPayLogMapper payLogMapper;

    @Autowired
    private TbOrderMapper orderMapper;

    @Override
    public TbPayLog searchPayLogByUserId(String userID) {
        return (TbPayLog) redisTemplate.boundHashOps(CONSTANT.PAY_LOG_KEY).get(userID);
    }

    /**
     *
     * @param out_trade_no 支付日志的主键
     * @param transaction_id 支付宝的流水号
     *  1、修改支付日志的内容
     *  2、根据支付日志得到orderlist，修改order的状态
     */
    @Override
    public void updateOrderStatus(String out_trade_no, String transaction_id) {

        //1\
        TbPayLog tbPayLog = payLogMapper.selectByPrimaryKey(out_trade_no);
        tbPayLog.setPayTime(new Date());
        tbPayLog.setTradeState("1");
        tbPayLog.setTransactionId(transaction_id);
        payLogMapper.updateByPrimaryKey(tbPayLog);

        //2\
        String[] orderList = tbPayLog.getOrderList().split(",");
        for(String orderIdStr:orderList){
            TbOrder tbOrder = orderMapper.selectByPrimaryKey(Long.parseLong(orderIdStr));
            tbOrder.setStatus("2");
            tbOrder.setUpdateTime(new Date());
            orderMapper.updateByPrimaryKey(tbOrder);
        }

    }
}
