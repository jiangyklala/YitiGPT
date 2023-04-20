package com.jxm.yitiGPT.mapper.cust;

import org.apache.ibatis.annotations.Param;

public interface UserMapperCust {

    public int balanceGetAndDecrNum(@Param("id") Long id, @Param("count") Long count);

    public int payWithCount(@Param("userEmail") String email, @Param("count") Long count);
}
