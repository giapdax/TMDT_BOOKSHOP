package vn.fs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import vn.fs.security.PreLoginLockFilter;
import vn.fs.security.CustomAuthenticationFailureHandler;
import vn.fs.service.UserDetailService;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired private UserDetailService userDetailService;
    @Autowired private SuccessHandler successHandler;
    @Autowired private CustomAuthenticationFailureHandler failureHandler;
    @Autowired private PreLoginLockFilter preLoginLockFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();

        http.authorizeRequests()
                .antMatchers("/css/**","/js/**","/images/**","/vendor/**","/fonts/**",
                        "/assets/**","/uploads/**").permitAll()

                // Public pages
                .antMatchers("/","/login","/register",
                        "/products","/productDetail","/productByCategory","/productByPublisher",
                        "/searchProduct","/aboutUs","/contact","/loadImage").permitAll()

                // Forgot password flow
                .antMatchers("/forgotPassword","/confirmOtpForgotPassword",
                        "/resendOtpForgotPassword","/changePassword").permitAll()

                // Cart flow (YÊU CẦU LOGIN)
                .antMatchers("/addToCart","/remove/**","/cart/**",
                        "/shoppingCart_checkout","/checkout").authenticated()

                // Admin
                .antMatchers("/admin/reports","/admin/reportCategory","/admin/reportYear",
                        "/admin/reportMonth","/admin/reportQuarter","/admin/reportOrderCustomer")
                .hasRole("ADMIN")
                .antMatchers("/admin/**").hasAnyRole("ADMIN","STAFF")

                // Favorites/Profile
                .antMatchers("/favorite/**","/profile").authenticated()

                .anyRequest().permitAll()
                .and()
                .formLogin()
                .loginProcessingUrl("/doLogin")
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
                .and()
                .logout()
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/?logout_success")
                .permitAll()
                .and()
                .rememberMe()
                .rememberMeParameter("remember");

        // chặn trước khi auth nếu bị lock
        http.addFilterBefore(preLoginLockFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
