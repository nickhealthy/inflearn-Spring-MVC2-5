package hello.login;

import hello.login.web.filter.LogFilter;
import hello.login.web.filter.LoginCheckFilter;
import hello.login.web.interceptor.LogInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Filter;

/**
 * 필터를 등록하는 곳
 * FilterRegistrationBean
 *   - 해당 클래스를 사용해서 빈으로 등록하면 스프링의 애플리케이션 컨텍스트에 필터가 추가되는 것이 아니라, 톰캣이 구동될 때 서블릿 컨텍스트에 필터를 추가하게 된다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LogInterceptor())
                .order(1)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/*.ico", "/error");
    }

//    @Bean
    public FilterRegistrationBean logFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new LogFilter());  // 등록한 필터를 지정
        filterRegistrationBean.setOrder(1);                 // 필터는 체인으로 동작해 순서가 중요하다. 낮을수록 먼저 동작한다.
        filterRegistrationBean.addUrlPatterns("/*");        // 필터를 적용할 URL 패턴을 지정한다. 한번에 여러 패턴을 지정할 수 있다.
        return filterRegistrationBean;
    }

    @Bean
    public FilterRegistrationBean loginCheckFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new LoginCheckFilter());  // 로그인 필터를 등록
        filterRegistrationBean.setOrder(2);                 // 로그 필터 다음에 로그인 필터가 적용된다.
        filterRegistrationBean.addUrlPatterns("/*");        // 모든 요청에 로그인 필터를 적용한다. whitelist로 로그인 체크를 제외해야 할 리스트를 담아서 제외시킨다.
        return filterRegistrationBean;
    }

}
