/*
 * Copyright © 2017-2023 Knife4j(xiaoymin@foxmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.github.xiaoymin.knife4j.spring.gateway.filter.basic;

import com.github.xiaoymin.knife4j.spring.gateway.conf.GlobalConstants;
import com.github.xiaoymin.knife4j.spring.gateway.filter.BasicFilter;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Base64;

/**
 * @author ：ZhRunXin 2023/5/4 0:51
 * @email ：zhrunxin33@gmail.com
 * @description：Security basic auth for gateway
 */
@Data
public class WebFluxSecurityBasicAuthFilter extends BasicFilter implements WebFilter {
    
    /***
     * 是否开启basic验证,默认不开启
     */
    private boolean enableBasicAuth = false;
    
    private String userName;
    
    private String password;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (enableBasicAuth) {
            // 只拦截Knife4J资源
            if (match(exchange.getRequest().getURI().toString())) {
                return exchange.getSession().doOnNext(session -> {
                    Object attribute = session.getAttribute(GlobalConstants.KNIFE4J_BASIC_AUTH_SESSION);
                    if (attribute != null) {
                        return;
                    } else {
                        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                        ServerHttpResponse response = exchange.getResponse();
                        if (authorization == null) {
                            writeForbiddenCode(response);
                        } else {
                            String[] parts = authorization.split(" ");
                            if (parts.length != 2 || !parts[0].equals("Basic")) {
                                writeForbiddenCode(response);
                            } else {
                                String credentials = new String(Base64.getDecoder().decode(parts[1]));
                                String[] usernameAndPassword = credentials.split(":");
                                if (usernameAndPassword.length != 2 || !usernameAndPassword[0].equals(userName) || !usernameAndPassword[1].equals(password)) {
                                    writeForbiddenCode(response);
                                } else {
                                    exchange.getSession().doOnNext(session1 -> {
                                        session1.getAttributes().put(GlobalConstants.KNIFE4J_BASIC_AUTH_SESSION, userName);
                                    }).subscribe();
                                }
                            }
                        }
                    }
                }).then(chain.filter(exchange));
            }
        }
        return chain.filter(exchange);
    }
    
    private void writeForbiddenCode(ServerHttpResponse serverHttpResponse) {
        serverHttpResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
        serverHttpResponse.getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Restricted Area\"");
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    
}
