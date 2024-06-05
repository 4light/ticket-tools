package test.ticket.tickettools.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import test.ticket.tickettools.dao.UserDao;
import test.ticket.tickettools.domain.bo.LoginRequest;
import test.ticket.tickettools.domain.bo.LoginResponse;
import test.ticket.tickettools.domain.entity.UserEntity;
import test.ticket.tickettools.service.UserService;
import test.ticket.tickettools.utils.JwtUtil;

import javax.annotation.Resource;
import java.util.Date;

@RestController
@RequestMapping("/ticket/auth")
public class AuthController{

    @Resource
    UserService userServiceImpl;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    loginRequest.getUserName(), loginRequest.getPwd());

            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            userServiceImpl.selectByUserName(loginRequest.getUserName());
            String jwt = jwtUtil.generateToken(authentication.getName());
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + jwt);
            UserEntity userEntity = userServiceImpl.selectByUserName(loginRequest.getUserName());
            LoginResponse loginResponse=new LoginResponse();
            loginResponse.setId(userEntity.getId());
            loginResponse.setNickName(userEntity.getNickName());
            loginResponse.setUserName(userEntity.getUserName());
            loginResponse.setRole(userEntity.getRole());
            loginResponse.setCreateDate(userEntity.getCreateDate());
            return ResponseEntity.ok().headers(headers).body(loginResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody LoginRequest registrationRequest) {
        // 检查用户名是否已经存在
        if (!ObjectUtils.isEmpty(userServiceImpl.selectByUserName(registrationRequest.getUserName()))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username already exists");
        }

        // 创建用户实体并设置用户名和加密后的密码
        UserEntity user = new UserEntity();
        user.setNickName(registrationRequest.getNickName());
        user.setUserName(registrationRequest.getUserName());
        user.setPwd(registrationRequest.getPwd());
        user.setRole(registrationRequest.getRole());
        user.setCreateDate(new Date());
        // 在这里你可能还需要设置用户的其他属性，例如角色等

        // 保存用户到数据库
        userServiceImpl.insert(user);
        return ResponseEntity.ok("User registered successfully");
    }

}

