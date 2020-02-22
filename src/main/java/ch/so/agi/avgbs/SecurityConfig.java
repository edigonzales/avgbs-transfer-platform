package ch.so.agi.avgbs;


import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        
        // @formatter:off
        http.authorizeRequests().antMatchers("/", "/login**", "/error**").permitAll()
        .anyRequest().authenticated()
        .and().logout().invalidateHttpSession(true).deleteCookies("JSESSIONID", "cognito", "XSRF-TOKEN").clearAuthentication(true).logoutUrl("/logout").logoutSuccessUrl("/")
        .and().oauth2Login();
        // @formatter:on
    }
}
