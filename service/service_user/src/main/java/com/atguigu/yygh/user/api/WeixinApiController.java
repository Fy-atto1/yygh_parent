package com.atguigu.yygh.user.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.helper.JwtHelper;
import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.user.utils.ConstantWxPropertiesUtils;
import com.atguigu.yygh.user.utils.HttpClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

// 微信操作的接口
@Controller
@RequestMapping("/api/ucenter/wx")
public class WeixinApiController {

    private final UserInfoService userInfoService;

    @Autowired
    public WeixinApiController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    // 微信扫描后回调的方法
    @GetMapping("callback")
    public String callback(String code, String state) {
        // 第一步 获取临时票据 code
        System.out.println("code:" + code);
        // 第二步 根据code和微信id和秘钥，请求微信固定地址，得到两个值
        // 使用code和appid以及appscrect换取access_token
        // %s 占位符
        StringBuffer baseAccessTokenUrl = new StringBuffer()
                .append("https://api.weixin.qq.com/sns/oauth2/access_token")
                .append("?appid=%s")
                .append("&secret=%s")
                .append("&code=%s")
                .append("&grant_type=authorization_code");
        String accessTokenUrl = String.format(baseAccessTokenUrl.toString(),
                ConstantWxPropertiesUtils.WX_OPEN_APP_ID,
                ConstantWxPropertiesUtils.WX_OPEN_APP_SECRET,
                code);
        try {
            // 使用httpclient请求地址
            String accessTokenInfo = HttpClientUtils.get(accessTokenUrl);
            System.out.println("accessTokenInfo:" + accessTokenInfo);
            // 从返回的字符串中获取access_token和openid
            JSONObject jsonObject = JSONObject.parseObject(accessTokenInfo);
            String access_token = jsonObject.getString("access_token");
            String openid = jsonObject.getString("openid");
            // 根据openid判断数据库中是否已经存在扫描人的信息
            UserInfo userInfo = userInfoService.selectWxInfoOpenId(openid);
            if (userInfo == null) {
                // 第三步 根据access_token和openid请求微信地址，得到扫描人信息
                String baseUserInfoUrl = "https://api.weixin.qq.com/sns/userinfo" +
                        "?access_token=%s" +
                        "&openid=%s";
                String userInfoUrl = String.format(baseUserInfoUrl, access_token, openid);
                String resultInfo = HttpClientUtils.get(userInfoUrl);
                System.out.println("resultInfo:" + resultInfo);
                JSONObject resultUserInfoJson = JSONObject.parseObject(resultInfo);
                // 解析用户信息
                // 用户昵称
                String nickname = resultUserInfoJson.getString("nickname");
                // 用户头像
                String headimgurl = resultUserInfoJson.getString("headimgurl");
                // 将扫描人的信息添加到数据库
                userInfo = new UserInfo();
                userInfo.setOpenid(openid);
                userInfo.setNickName(nickname);
                userInfo.setStatus(1);
                userInfoService.save(userInfo);
            }
            // 返回name和token字符串
            Map<String, String> map = new HashMap<>();
            String name = userInfo.getName();
            if (StringUtils.isEmpty(name)) {
                name = userInfo.getNickName();
            }
            if (StringUtils.isEmpty(name)) {
                name = userInfo.getPhone();
            }
            map.put("name", name);
            // 判断userInfo中是否有手机号，如果手机号为空，返回openid
            // 如果手机号不为空，返回的openid为字符串
            // 用于前端判断用户是否已经绑定手机号
            // 如果openid不为空，则需要绑定手机号
            // 如果openid为空，那么不需要绑定手机号
            if (StringUtils.isEmpty(userInfo.getPhone())) {
                map.put("openid", userInfo.getOpenid());
            } else {
                map.put("openid", "");
            }
            // 使用jwt生成token字符串
            String token = JwtHelper.createToken(userInfo.getId(), name);
            map.put("token", token);
            // 跳转到前端页面
            return "redirect:"
                    + ConstantWxPropertiesUtils.YYGH_BASE_URL
                    + "/weixin/callback?token=" + map.get("token")
                    + "&openid=" + map.get("openid")
                    + "&name=" + URLEncoder.encode(map.get("name"), "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 1 生成微信扫描二维码
    // 返回生成二维码需要的参数
    @GetMapping("getLoginParam")
    @ResponseBody
    public Result<Map<String, Object>> getQrConnect() {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("appid", ConstantWxPropertiesUtils.WX_OPEN_APP_ID);
            map.put("scope", "snsapi_login");
            String wxOpenRedirectUrl = ConstantWxPropertiesUtils.WX_OPEN_REDIRECT_URL;
            String encodeUrl = URLEncoder.encode(wxOpenRedirectUrl, "utf-8");
            map.put("redirect_uri", encodeUrl);
            map.put("state", System.currentTimeMillis() + "");
            return Result.ok(map);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 2 回调的方法，得到扫描人信息

}
