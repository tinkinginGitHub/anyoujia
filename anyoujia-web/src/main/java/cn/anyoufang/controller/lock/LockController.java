package cn.anyoufang.controller.lock;

import cn.anyoufang.controller.AbstractController;
import cn.anyoufang.entity.SpLockAdmin;
import cn.anyoufang.entity.SpMember;
import cn.anyoufang.entity.selfdefined.AnyoujiaResult;
import cn.anyoufang.entity.selfdefined.Temppwd;
import cn.anyoufang.enumresource.HttpCodeEnum;
import cn.anyoufang.log.annotation.LockOperateLog;
import cn.anyoufang.service.LockMemberService;
import cn.anyoufang.service.LockService;
import cn.anyoufang.service.LoginService;
import cn.anyoufang.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author daiping
 */
@RestController
@RequestMapping("/lock")
public class LockController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockController.class);

    @Value("${lock.salt}")
    private String lockSalt;
    @Value("${pageSize}")
    private String pagesize;
    @Value("${lock.url}")
    private String url;

    @Autowired
    private LoginService loginService;

    @Autowired
    private LockService lockService;

    @Autowired
    private LockMemberService memberService;

    /**
     * 获取锁开锁记录和报警记录
     * @param locksn
     * @param isalarm 0表示开锁记录，1表示报警记录，99表示所有的
     * @param page
     * @return
     */
    @RequestMapping("/records")
    public AnyoujiaResult getLockRecords(@RequestParam String locksn,
                                         @RequestParam int isalarm ,
                                         @RequestParam int page) {
        if(StringUtil.isEmpty(locksn)) {
            return AnyoujiaResult.build(FOUR_H,"参数异常");
        }
        return lockService.getLockRecords(locksn,isalarm,page);
    }

    /**
     * 添加指纹/IC卡用户信息
     * @param fingerid 指纹id
     * @param locksn
     * @param endtime
     * @param usertype 2:指纹 3：IC卡
     * @param fingerdesc 指纹描述
     * @return
     */
    @RequestMapping("/setuser")
    @LockOperateLog(operateTypeDesc="添加指纹")
    public AnyoujiaResult setLockUserFingerPassword(@RequestParam String fingerid,
                                                    @RequestParam String locksn,
                                                    @RequestParam int endtime,
                                                    @RequestParam int usertype,
                                                    @RequestParam(required = false) String fingerdesc,
                                                    HttpServletRequest request,@RequestParam(required = false,defaultValue = "0") int relationid) {
        if(StringUtil.isEmpty(locksn) || StringUtil.isEmpty(fingerid)) {
            return AnyoujiaResult.build(FOUR_H,"参数异常");
        }
        SpMember user =  getUser(request,loginService);
        if(user == null) {
            AnyoujiaResult.build(FOUR_H_1,"登录超时");
        }
        String nickname;
        int memberid = user.getUid();
        String phone = user.getPhone();
        boolean isAdmin = false;
        List<SpLockAdmin> lockAdmin = loginService.getLockAdmin(user.getUid(),locksn);
        if(lockAdmin.isEmpty()) {
            String relationUsername = loginService.getMemberRelation(locksn,phone);
            if(relationUsername != null) {
                nickname = relationUsername;
            }else {
                nickname = user.getBname();
            }
        }else {
            nickname = "我";
            isAdmin = true;
        }

        return lockService.setLockUserFingerPassword(memberid,locksn,endtime,usertype,nickname,phone,fingerdesc,fingerid,isAdmin,relationid);
    }


    /**
     * 添加锁密码用户
     * @param ptype 密码类型 1：永久 2：一次 3：临时
     * @param
     * @param locksn
     * @param endtime
     * @param motive 临时密码的目的
     * @param request
     * @param pwds
     * @param relationid
     * @return
     */
    @RequestMapping("/setpwd")
    @LockOperateLog(operateTypeDesc="添加锁密码")
    public AnyoujiaResult setLockPwd(@RequestParam int ptype,
                             @RequestParam String locksn,
                             @RequestParam(required = false,defaultValue = "0") int endtime,
                             @RequestParam(required = false) String motive,
                             HttpServletRequest request,
                             @RequestParam String pwds,@RequestParam(required = false,defaultValue = "0") int relationid) {

        if(StringUtil.isEmpty(locksn) ||StringUtil.isEmpty(pwds)) {
            return AnyoujiaResult.build(FOUR_H,"参数异常");
        }
        SpMember user = getUser(request,loginService);
        if(user == null) {
            AnyoujiaResult.build(FOUR_H_1,"登录超时");
        }
        String nickname;
        int memberid = user.getUid();
        String phone = user.getPhone();
        boolean isAdmin = false;
         List<SpLockAdmin> lockAdmin = loginService.getLockAdmin(user.getUid(),locksn);
         if(lockAdmin.isEmpty()) {
             String relation = loginService.getMemberRelation(locksn,phone);
             if(relation != null) {
                 nickname = relation;
             }else {
                 nickname = user.getBname();
             }
         }else {
            nickname = "我";
            isAdmin = true;
         }

        Map<String,String> res =  lockService.setLockPwd(ptype,memberid,locksn,endtime,pwds,nickname,user.getPhone(),isAdmin,motive,relationid);
        if(res == null) {
            return AnyoujiaResult.build(FIVE_H,"系统异常");
        }
        return AnyoujiaResult.build(Integer.valueOf(res.get("code")),res.get("msg"));
    }

    /**
     * 获取APP用户注册锁列表
     *
     * @return
     */
    @RequestMapping("/list")
    public AnyoujiaResult getLockList(HttpServletRequest request) {
       SpMember user =  getUser(request,loginService);
       if(user == null) {
          return AnyoujiaResult.build(FOUR_H_1,"登录超时");
       }

        return lockService.getAllLockList(user);
    }

    /**
     * 获取单个门锁详情
     * @param locksn
     * @return
     */
    @RequestMapping("/info")
    public AnyoujiaResult getLockInfo(@RequestParam String locksn) {

        if (StringUtil.isEmpty(locksn)) {
            return AnyoujiaResult.build(FOUR_H, "参数异常");
        }
       return lockService.getLockInfo(locksn);
    }

    /**
     * 注册锁信息(添加锁管理员)
     * @param locksn
     * @return
     */
    @RequestMapping("/register")
    @LockOperateLog(operateTypeDesc="注册锁")
    public AnyoujiaResult registerLockInfo(@RequestParam String locksn,HttpServletRequest request) {
        if (StringUtil.isEmpty(locksn)) {
            return AnyoujiaResult.build(FOUR_H, "参数异常");
        }
        SpMember user = getUser(request, loginService);
        if (user == null) {
            return AnyoujiaResult.build(FOUR_H, "登录超时");
        }
        return lockService.registerLockInfo(locksn, user.getUid());
    }

    /**
     * 移除锁上用户的密码信息
     * @param locksn
     * @param relationid
     * @return
     */
    @RequestMapping("/delpwd")
    @LockOperateLog(operateTypeDesc = "删除密码")
    public AnyoujiaResult delLockPwd(@RequestParam String locksn,
                                     @RequestParam(required = false,defaultValue = "-1") int relationid,
                                     HttpServletRequest request) {

        if(StringUtil.isEmpty(locksn)) {
            return AnyoujiaResult.build(FOUR_H,"参数异常");
        }
        SpMember member = getUser(request,loginService);

        if(member == null) {
             return AnyoujiaResult.build(FOUR_H_1,"登录超时");
        }

        if(memberService.deletePermentPwd(member.getUid(),locksn,member.getPhone(),relationid)) {
            return AnyoujiaResult.ok();
        }
        return AnyoujiaResult.build(FOUR_H,"删除超时,请重试");
    }

    /**
     * pwdtype 99,获取全部
     * usertype 用户类型 99:全部 0: APP 1:密码 2: 指纹 3:IC卡
     * @param locksn
     * @return
     */
    @RequestMapping("/templocklist")
    public AnyoujiaResult tempLockPwdList(@RequestParam String locksn,
                                          @RequestParam int begintime,
                                          @RequestParam int page,HttpServletRequest request) {

        SpMember member = getUser(request,loginService);

        if(member == null) {
            return AnyoujiaResult.build(FOUR_H_1,HttpCodeEnum.FOUR_HUNDRED1.getValue());
        }
        //pwdtype 99,获取全部
        int pwdtype =  99;
        // 1表示密码
        int usertype = 1;
        int memberid = member.getUid();
        List<Temppwd> res =  lockService.getLockTempPwdList(locksn,pwdtype,usertype,memberid,begintime,page);

        if( res==null || res.isEmpty()) {
           return AnyoujiaResult.build(TWO_H1, HttpCodeEnum.TWO_HUNDRED1.getValue());
        }

        return AnyoujiaResult.ok(res);
    }

    /**
     * 用于根据锁SN查询锁激活状态信息
     * @param locksn
     * @return
     */
    @RequestMapping("/activeinfo")
    public AnyoujiaResult getActiveInfo(@RequestParam String locksn) {
        if(StringUtil.isEmpty(locksn)) {
            return AnyoujiaResult.build(FOUR_H,"锁序列号不能为空");
        }
        return lockService.getLockActiveAndAddress(locksn);
    }
}
