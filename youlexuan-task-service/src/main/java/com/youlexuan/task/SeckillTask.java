package com.youlexuan.task;

import com.youlexuan.CONSTANT;
import com.youlexuan.mapper.TbSeckillGoodsMapper;
import com.youlexuan.pojo.TbSeckillGoods;
import com.youlexuan.pojo.TbSeckillGoodsExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class SeckillTask {

    @Autowired
    private TbSeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Scheduled(cron = "* * * * * ?")
    public void refreshSeckillGoodsList(){
//        System.out.println("------定时刷新秒杀列表数据-------"+new Date());
        /**
         * 查询秒杀商品列表，要求，id不在redis中存在，并且符合秒杀的规则
         * 1、状态
         * 2、时间
         * 3、库存
         */
        TbSeckillGoodsExample exam = new TbSeckillGoodsExample();
        TbSeckillGoodsExample.Criteria criteria = exam.createCriteria();
        criteria.andStatusEqualTo("1");
        criteria.andStartTimeLessThanOrEqualTo(new Date());
        criteria.andEndTimeGreaterThanOrEqualTo(new Date());
        criteria.andStockCountGreaterThan(0);
        Set keys = redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).keys();
        List<Long> idList = new ArrayList<>(keys);
        criteria.andIdNotIn(idList);
        List<TbSeckillGoods> seckillGoodsList = seckillGoodsMapper.selectByExample(exam);
        for (TbSeckillGoods seckillGoods:seckillGoodsList){
            redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).put(seckillGoods.getId(),seckillGoods);
        }
        System.out.println("将"+seckillGoodsList.size()+"个秒杀商品存放到redis中");

    }
}
