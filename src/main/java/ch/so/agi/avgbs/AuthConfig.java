package ch.so.agi.avgbs;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class AuthConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri(URI.create("http://localhost:8080/test"));
        return oidcLogoutSuccessHandler;
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        LogoutSuccessHandler logoutHandler =
                new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);

        // @formatter:off
        http.csrf().disable().authorizeRequests()
                .antMatchers(HttpMethod.GET, "/login/**").permitAll()
                .antMatchers(HttpMethod.GET, "/test").permitAll()
                .anyRequest().authenticated()
                .and()
                .oauth2Login()
                .and()
                .logout()
                .clearAuthentication(true)
                .deleteCookies().invalidateHttpSession(true);
        // @formatter:on
        
    }
    

}
