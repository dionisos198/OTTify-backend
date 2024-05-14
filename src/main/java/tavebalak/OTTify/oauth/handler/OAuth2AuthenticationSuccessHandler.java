package tavebalak.OTTify.oauth.handler;

import static tavebalak.OTTify.oauth.cookie.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tavebalak.OTTify.common.constant.Role;
import tavebalak.OTTify.oauth.CustomOAuth2User;
import tavebalak.OTTify.oauth.cookie.CookieUtils;
import tavebalak.OTTify.oauth.cookie.HttpCookieOAuth2AuthorizationRequestRepository;
import tavebalak.OTTify.oauth.jwt.JwtService;
import tavebalak.OTTify.oauth.redis.RefreshTokenRepository;
import tavebalak.OTTify.user.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final RefreshTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login 성공!");
        try {
            CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

            // User의 Role이 GUEST일 경우 처음 요청한 회원이므로 회원가입 페이지로 리다이렉트
            if (oAuth2User.getRole() == Role.GUEST) {
                String accessToken = jwtService.createAccessToken(oAuth2User.getEmail());
                String refreshToken = jwtService.createRefreshToken();
                Long userId = oAuth2User.getUserId();

                response.addHeader(jwtService.getAccessHeader(), "Bearer " + accessToken);
                response.addHeader(jwtService.getRefreshHeader(), "Bearer " + refreshToken);

                String url = generateUrl(accessToken, refreshToken, userId, request, response);
//                response.sendRedirect("oauth2/sign-up"); // 프론트의 회원가입 추가 정보 입력 폼으로 리다이렉트

                jwtService.sendAccessAndRefreshToken(response, oAuth2User.getEmail(), accessToken,
                        refreshToken);

                response.sendRedirect(url);

                log.info(tokenRepository.findByRefreshToken(refreshToken).get().getEmail());
//                User findUser = userRepository.findByEmail(oAuth2User.getEmail())
//                                .orElseThrow(() -> new IllegalArgumentException("이메일에 해당하는 유저가 없습니다."));
//                findUser.authorizeUser();
            } else {
                log.info("===user login===");
                loginSuccess(request, response, oAuth2User); // 로그인에 성공한 경우 access, refresh 토큰 생성
            }
        } catch (Exception e) {
            throw e;
        }

    }

    private void loginSuccess(HttpServletRequest request, HttpServletResponse response,
            CustomOAuth2User oAuth2User)
            throws IOException {
        String accessToken = jwtService.createAccessToken(oAuth2User.getEmail());
        String refreshToken = jwtService.createRefreshToken();
        Long userId = oAuth2User.getUserId();

        response.addHeader(jwtService.getAccessHeader(), "Bearer " + accessToken);
        response.addHeader(jwtService.getRefreshHeader(), "Bearer " + refreshToken);

        String url = generateUrl(accessToken, refreshToken, userId, request, response);
        log.info("===redirect===");

        jwtService.sendAccessAndRefreshToken(response, oAuth2User.getEmail(), accessToken,
                refreshToken);
        jwtService.updateRefreshToken(oAuth2User.getEmail(), refreshToken);

        response.sendRedirect(url);

    }

    private String generateUrl(final String accessToken, final String refreshToken,
            final Long userId, HttpServletRequest request, HttpServletResponse response) {
        StringBuilder sb = new StringBuilder();
        StringBuilder url = sb.append(generateBaseUrl(request, response) + "login/oauth2/code")
                .append("?")
                .append("accessToken=")
                .append(accessToken)
                .append("&")
                .append("refreshToken=")
                .append(refreshToken)
                .append("&")
                .append("userId=")
                .append(userId);
        return url.toString();
    }

    private String generateBaseUrl(HttpServletRequest request, HttpServletResponse response) {

        String targetUrl = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue).orElse("");
        clearAuthenticationAttributes(request, response);

        return targetUrl;

    }

    private void clearAuthenticationAttributes(HttpServletRequest request,
            HttpServletResponse response) {

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request,
                response);
    }


}