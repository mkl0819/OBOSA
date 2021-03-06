package com.ssafy.obosa.service.common;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ssafy.obosa.enumeration.JwtExpireTermEnum;
import com.ssafy.obosa.enumeration.LoginState;
import com.ssafy.obosa.enumeration.ResponseMessage;
import com.ssafy.obosa.enumeration.StatusCode;
import com.ssafy.obosa.model.common.DefaultRes;
import com.ssafy.obosa.model.common.JwtToken;
import com.ssafy.obosa.model.common.LoginReq;
import com.ssafy.obosa.model.common.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Service
public class JwtService
{
    @Value("${JWT.ISSUER}")
    private String ISSUER;

    @Value("${JWT.SECRET}")
    private String SECRET;

    private final AuthService authService;

    public JwtService(final AuthService authService)
    {
        this.authService = authService;
    }

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public DefaultRes generateToken(LoginReq loginReq)
    {
        LoginState loginState = authService.loginCheck(loginReq);
        int state = loginState.getState();

        if(state == LoginState.SUCCESS.getState()){
            long nowTime = System.currentTimeMillis() / 1000;

            Token accessToken = createToken(loginReq.getEmail(), JwtExpireTermEnum.ACCESS, nowTime);
            Token refreshToken = createToken(loginReq.getEmail(), JwtExpireTermEnum.REFRESH, nowTime);
            JwtToken jwtToken = new JwtToken(accessToken, refreshToken);
            return DefaultRes.res(StatusCode.CREATED, ResponseMessage.LOGIN_SUCCESS, jwtToken);
        }
        else if(state == LoginState.BANNED.getState()){
            return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessage.BANNED_USER);
        }
        else if(state == LoginState.FAIL.getState()){
            return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessage.LOGIN_FAIL);
        }
        else{
            return DefaultRes.res(StatusCode.INTERNAL_SERVER_ERROR, ResponseMessage.INTERNAL_SERVER_ERROR);
        }
    }

    public DefaultRes renewAccessToken(String refreshToken)
    {
        long nowTime = System.currentTimeMillis() / 1000;

        try
        {
            String email = decode(refreshToken);

            if(email == null)
            {
                return DefaultRes.res(StatusCode.UNAUTHORIZED, ResponseMessage.AUTH_FAIL);
            }

            if(authService.emailCheck(email))
            {
                return DefaultRes.res(StatusCode.CREATED, ResponseMessage.AUTH_SUCCESS, createToken(email, JwtExpireTermEnum.ACCESS, nowTime));
            }
            else
            {
                return DefaultRes.res(StatusCode.UNAUTHORIZED, ResponseMessage.AUTH_FAIL);
            }
        }
        catch (Exception e)
        {
            return DefaultRes.res(StatusCode.UNAUTHORIZED, ResponseMessage.AUTH_FAIL);
        }
    }
    private Token createToken(String email, JwtExpireTermEnum type, long nowTime)
    {
        long expireTime = nowTime + type.getExpireTerm();

        Date expireDate = new Date(expireTime * 1000);

        String data = JWT.create()
                .withExpiresAt(expireDate)
                .withIssuer(ISSUER)
                .withClaim("email", email)
                .sign(Algorithm.HMAC256(SECRET));

        return new Token(data, nowTime, expireTime);
    }

    public String decode(String token)
    {
        try
        {
            final JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(SECRET)).withIssuer(ISSUER).build();
            DecodedJWT decodedJWT = jwtVerifier.verify(token);

            System.out.println(decodedJWT.getClaim("email").asString());
            return decodedJWT.getClaim("email").asString();
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
