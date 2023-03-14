package com.example.emos.api.db.dao;

import com.example.emos.api.db.pojo.TbUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Mapper
public interface TbUserDao {
    //查询用户权限
    public Set<String> searchUserPermissions(int userId);

    public Integer searchIdByOpenId(String openId);

    public HashMap searchById(int userId);

    public HashMap searchUserSummary(int userId);

    public HashMap searchUserInfo(int userId);

    public Integer searchDeptManagerId(int id);

    public Integer searchGmId();

    public ArrayList<HashMap> searchAllUser();

    //登录，参数为hashmap传入登陆时的用户名
    public Integer login(HashMap param);

    //修改密码，参数为password和userid封装到HashMap，返回修改的数据的数量
    public int updatePassword(HashMap param);

    //查询用户分页信息
    public ArrayList<HashMap> searchUserByPage(HashMap param);

    //查询符合条件的分页记录总数
    public long searchUserCount(HashMap param);

    //添加用户，返回条数
    public int insert(TbUser user);

    //修改用户信息
    public int update(HashMap param);

    public int deleteUserByIds(Integer[] ids);

    public ArrayList<String> searchUserRoles(int userId);

    public HashMap searchNameAndDept(int userId);
}