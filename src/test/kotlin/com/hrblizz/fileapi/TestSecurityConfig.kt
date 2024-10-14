import com.hrblizz.fileapi.security.ApiAuthenticationEntryPoint
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServletResponse


@TestConfiguration
@EnableWebSecurity
@Profile("test")
internal class TestSecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeRequests().anyRequest().permitAll() // Permit all requests
            .and()
            .csrf().disable()
        return http.build()
    }
}


@TestConfiguration
@EnableWebSecurity
@Profile("auth-test") // Specify that this config is for the auth-test profile
internal class AuthTestSecurityConfig (
    private val apiAuthenticationEntryPoint: ApiAuthenticationEntryPoint,
    ){

    private val logger = LoggerFactory.getLogger(AuthTestSecurityConfig::class.java)

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        println("auth-test security filter chain")
        http
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .httpBasic().authenticationEntryPoint(apiAuthenticationEntryPoint)
            .and()
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/docs", "/docs/*").permitAll()
            .antMatchers("/status", "/webjars/**", "/favicon.ico").permitAll()
            .anyRequest().fullyAuthenticated()
            .and().cors()
            .and().exceptionHandling().authenticationEntryPoint { request, response, authException ->
                logger.error("Unauthorized request: ${request.requestURI}")
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.message)
            }

        return http.build()
    }

    @Bean
    fun inMemoryUserDetailsManager(): UserDetailsService {
        val user = User.withUsername("USER")
            .password("{noop}PASSWORD1") // No password encoder for simplicity
            .roles("USER")
            .build()

        return InMemoryUserDetailsManager(user)
    }
}
