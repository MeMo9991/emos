package com.example.emos.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.example.emos.api.common.util.PageUtils;
import com.example.emos.api.common.util.R;
import com.example.emos.api.controller.form.*;
import com.example.emos.api.db.pojo.TbUser;
import com.example.emos.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

@RestController
@RequestMapping("/user")
@Tag(name = "UserController", description = "用户Web接口")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 生成登陆二维码的字符串
     */
    @GetMapping("/createQrCode")
    @Operation(summary = "生成二维码Base64格式的字符串")
    public R createQrCode() {
        HashMap map = userService.createQrCode();
        return R.ok(map);
    }

    /**
     * 检测登陆验证码
     *
     * @param form
     * @return
     */
    @PostMapping("/checkQrCode")
    @Operation(summary = "检测登陆验证码")
    public R checkQrCode(@Valid @RequestBody CheckQrCodeForm form) {
        boolean bool = userService.checkQrCode(form.getCode(), form.getUuid());
        return R.ok().put("result", bool);
    }

    @PostMapping("/wechatLogin")
    @Operation(summary = "微信小程序登陆")
    public R wechatLogin(@Valid @RequestBody WechatLoginForm form) {
        HashMap map = userService.wechatLogin(form.getUuid());
        boolean result = (boolean) map.get("result");
        if (result) {
            int userId = (int) map.get("userId");
            StpUtil.setLoginId(userId);
            Set<String> permissions = userService.searchUserPermissions(userId);
            map.remove("userId");
            map.put("permissions", permissions);
            String token=StpUtil.getTokenInfo().getTokenValue();
            map.put("token",token);
        }
        return R.ok(map);
    }

    /**
     * 登陆成功后加载用户的基本信息
     */
    @GetMapping("/loadUserInfo")
    @Operation(summary = "登陆成功后加载用户的基本信息")
    @SaCheckLogin
    public R loadUserInfo() {
        int userId = StpUtil.getLoginIdAsInt();
        HashMap summary = userService.searchUserSummary(userId);
        return R.ok(summary);
    }

    @PostMapping("/searchById")
    @Operation(summary = "根据ID查找用户")
    @SaCheckPermission(value = {"ROOT", "USER:SELECT"}, mode = SaMode.OR)
    public R searchById(@Valid @RequestBody SearchUserByIdForm form) {
        HashMap map = userService.searchById(form.getUserId());
        return R.ok(map);
    }

    @GetMapping("/searchAllUser")
    @Operation(summary = "查询所有用户")
    @SaCheckLogin
    public R searchAllUser() {
        ArrayList<HashMap> list = userService.searchAllUser();
        return R.ok().put("list", list);
    }

    @PostMapping("/login")
    @Operation(summary = "登陆系统")
    public R login(@Valid @RequestBody LoginForm form){
        //form类只用于web层，转换为hashmap对象传给业务层
        HashMap param= JSONUtil.parse(form).toBean(HashMap.class);
        Integer userId=userService.login(param);
        R r=R.ok().put("result",userId!=null?true:false);
        if(userId!=null){
            //登陆后sa-token生成令牌字符串，并返回给浏览器以cookie形式存储
            //当第二次请求浏览器携带cookie发给后端程序sa-token
            StpUtil.setLoginId(userId);
            //查询用户权限列表
            Set<String> permissions=userService.searchUserPermissions(userId);
            String token=StpUtil.getTokenInfo().getTokenValue();
            //放到r变量
            r.put("permissions",permissions).put("token",token);
        }
        return r;
    }

    @GetMapping("/logout")
    @Operation(summary = "退出系统")
    public R logout(){
        //sa-token退出系统，摸除redis中令牌信息，使浏览器保存的cookie信息过期
        StpUtil.logout();
        return R.ok();
    }

    @PostMapping("/updatePassword")
    @SaCheckLogin
    @Operation(summary = "修改密码")
    public R updatePassword(@Valid @RequestBody UpdatePasswordForm form){
        //把浏览器提交的cookie转化为userID
        int userId=StpUtil.getLoginIdAsInt();
        HashMap param=new HashMap(){{
            put("userId",userId);
            put("password",form.getPassword());
        }};
        int rows=userService.updatePassword(param);
        return R.ok().put("rows",rows);
    }

    //查询用户分页记录
    @PostMapping("/searchUserByPage")
    @Operation(summary = "查询用户分页记录")
    @SaCheckPermission(value = {"ROOT", "USER:SELECT"}, mode = SaMode.OR)
    public R searchUserByPage(@Valid @RequestBody SearchUserByPageForm form){
        int page=form.getPage();
        int length=form.getLength();
        int start=(page-1)*length;
        HashMap param=JSONUtil.parse(form).toBean(HashMap.class);
        param.put("start",start);
        PageUtils pageUtils=userService.searchUserByPage(param);
        return R.ok().put("page",pageUtils);
    }

    //添加用户
    @PostMapping("/insert")
    @SaCheckPermission(value = {"ROOT", "USER:INSERT"}, mode = SaMode.OR)
    @Operation(summary = "添加用户")
    public R insert(@Valid @RequestBody InsertUserForm form){
        //form转换为TbUser映射类，用于匹配xml文件的parameterType
        TbUser user=JSONUtil.parse(form).toBean(TbUser.class);
        //设置在职状态
        user.setStatus((byte)1);
        //form中role是Integer数组，数据库中是Json格式，转为Json
        user.setRole(JSONUtil.parseArray(form.getRole()).toString());
        //入职时间
        user.setCreateTime(new Date());
        int rows=userService.insert(user);
        //返回给前端
        return R.ok().put("rows",rows);
    }

    //修改用户信息
    @PostMapping("/update")
    @SaCheckPermission(value = {"ROOT", "USER:UPDATE"}, mode = SaMode.OR)
    @Operation(summary = "修改用户")
    public R update(@Valid @RequestBody UpdateUserForm form){
        HashMap param=JSONUtil.parse(form).toBean(HashMap.class);
        //form由integer数组转为json
        param.replace("role",JSONUtil.parseArray(form.getRole()).toString());
        int rows=userService.update(param);
        //用户修改信息成功，踢下线
        if(rows==1){
            StpUtil.logoutByLoginId(form.getUserId());
        }
        return R.ok().put("rows",rows);
    }

    //删除用户信息
    @PostMapping("/deleteUserByIds")
    @SaCheckPermission(value = {"ROOT", "USER:DELETE"}, mode = SaMode.OR)
    @Operation(summary = "删除用户")
    public R deleteUserByIds(@Valid @RequestBody DeleteUserByIdsForm form){
        //提取出浏览器的cookie中的userid
        Integer userId=StpUtil.getLoginIdAsInt();
        if(ArrayUtil.contains(form.getIds(),userId)){
            return R.error("您不能删除自己的帐户");
        }
        int rows=userService.deleteUserByIds(form.getIds());
        //被删除的用户踢下线
        if(rows>0){
            for (Integer id:form.getIds()){
                StpUtil.logoutByLoginId(id);
            }
        }
        return R.ok().put("rows",rows);
    }

    @PostMapping("/searchNameAndDept")
    @Operation(summary = "查找员工姓名和部门")
    @SaCheckLogin
    public R searchNameAndDept(@Valid @RequestBody SearchNameAndDeptForm form){
        HashMap map=userService.searchNameAndDept(form.getId());
        return R.ok(map);
    }

}
