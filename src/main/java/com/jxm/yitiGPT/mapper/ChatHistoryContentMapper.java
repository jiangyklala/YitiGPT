package com.jxm.yitiGPT.mapper;

import com.jxm.yitiGPT.domain.ChatHistoryContent;
import com.jxm.yitiGPT.domain.ChatHistoryContentExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChatHistoryContentMapper {
    long countByExample(ChatHistoryContentExample example);

    int deleteByExample(ChatHistoryContentExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ChatHistoryContent record);

    int insertSelective(ChatHistoryContent record);

    List<ChatHistoryContent> selectByExampleWithBLOBs(ChatHistoryContentExample example);

    List<ChatHistoryContent> selectByExample(ChatHistoryContentExample example);

    ChatHistoryContent selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ChatHistoryContent record, @Param("example") ChatHistoryContentExample example);

    int updateByExampleWithBLOBs(@Param("record") ChatHistoryContent record, @Param("example") ChatHistoryContentExample example);

    int updateByExample(@Param("record") ChatHistoryContent record, @Param("example") ChatHistoryContentExample example);

    int updateByPrimaryKeySelective(ChatHistoryContent record);

    int updateByPrimaryKeyWithBLOBs(ChatHistoryContent record);
}