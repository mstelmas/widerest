package pl.touk.widerest.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import pl.touk.widerest.security.authentication.AnonymousUserDetailsService;
import pl.touk.widerest.security.authentication.AnonymousUserInterceptor;
import pl.touk.widerest.security.authentication.UserAuthenticationConverterImpl;

import javax.annotation.Resource;

@Configuration
@EnableAuthorizationServer
@EnableGlobalAuthentication
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Resource
    AnonymousUserDetailsService anonymousUserDetailsService;

    @Resource
    AuthenticationManager authenticationManager;

    @Resource
    private AnonymousUserInterceptor anonymousUserInterceptor;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        auth.parentAuthenticationManager(authenticationManager);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .addInterceptor(anonymousUserInterceptor)
                .tokenStore(tokenStore())
                .tokenEnhancer(jwtTokenEnhancer())
                .authenticationManager(authenticationManager);
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
                .withClient("backoffice").secret("secret").authorizedGrantTypes("password", "implicit", "authorization_code").scopes("site","backoffice").autoApprove(true);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security.allowFormAuthenticationForClients().realm("oauth/widerest");
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(jwtTokenEnhancer());
    }

    @Bean
    public JwtAccessTokenConverter jwtTokenEnhancer() {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        ((DefaultAccessTokenConverter)jwtAccessTokenConverter.getAccessTokenConverter()).setUserTokenConverter(new UserAuthenticationConverterImpl());
        return jwtAccessTokenConverter;
    }


}