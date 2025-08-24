package com.Ecom.api_gateway_service.Filter;

import com.Ecom.api_gateway_service.utils.JwtUtil;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
    @Autowired
    private RoutValidator routeValidator;
    @Autowired
    private JwtUtil util;
public AuthenticationFilter()
{
    super(Config.class);
}
    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange,chain)->
        {
            System.out.println("Request is here");
            if(routeValidator.isSecured.test(exchange.getRequest()))
            {
                if(!(exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)))
                {
                    System.out.println("Request missing header here");
                    throw new RuntimeException("missing authorization header");
                }
                String authHeader=exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if(authHeader!=null &&authHeader.startsWith("Bearer "))
                {
                    authHeader=authHeader.substring(7);
                }
                System.out.println("Filter bypassed from this");
               if(!util.validateToken(authHeader)){
                   throw new RuntimeException("Cannot validate token");
               }
            }
            return chain.filter(exchange);
        }
                );
    }

    public static class Config{

    }

}
