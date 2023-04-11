package com.jxm.yitiGPT.mapper;

import com.jxm.yitiGPT.domain.ChatHistory;
import com.jxm.yitiGPT.domain.ChatHistoryExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChatHistoryMapper {
    long countByExample(ChatHistoryExample example);

    int deleteByExample(ChatHistoryExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ChatHistory record);

    int insertSelective(ChatHistory record);

    List<ChatHistory> selectByExample(ChatHistoryExample example);

    ChatHistory selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ChatHistory record, @Param("example") ChatHistoryExample example);

    int updateByExample(@Param("record") ChatHistory record, @Param("example") ChatHistoryExample example);

    int updateByPrimaryKeySelective(ChatHistory record);

    int updateByPrimaryKey(ChatHistory record);
}