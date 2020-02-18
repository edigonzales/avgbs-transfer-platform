package ch.so.agi.avgbs;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class AuthConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http.authorizeRequests()
                .antMatchers(HttpMethod.GET, "/login/**").permitAll()
                .antMatchers(HttpMethod.GET, "/test").permitAll()
                .anyRequest().authenticated()
                .and()
                .oauth2Login();
        // @formatter:on
        
    }
}
