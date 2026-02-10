package com.souzamonteiro.nfe.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebFilter(filterName = "LoginFilter", urlPatterns = { "/*" })
public class LoginFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("=== LoginFilter inicializado ===");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        String loginURL = httpRequest.getContextPath() + "/login.xhtml";
        String requestURI = httpRequest.getRequestURI();

        System.out.println("=== LoginFilter: URI=" + requestURI);

        // Recursos que NÃO precisam de login
        boolean isLoginRequest = requestURI.contains("login.xhtml");
        boolean isRegistroRequest = requestURI.contains("registro.xhtml");
        boolean isResourceRequest = requestURI.contains("/javax.faces.resource/");
        boolean isErrorPage = requestURI.contains("error.xhtml");

        // Verificar se usuário está logado (na sessão)
        boolean loggedIn = false;
        if (session != null) {
            // Verifica tanto no atributo da sessão quanto no mapa da sessão
            if (session.getAttribute("usuarioLogado") != null) {
                loggedIn = true;
                System.out.println("=== LoginFilter: Usuário logado na sessão ===");
            }
        }

        System.out.println("=== LoginFilter: loggedIn=" + loggedIn +
                ", isLoginRequest=" + isLoginRequest +
                ", isResourceRequest=" + isResourceRequest);

        if (loggedIn || isLoginRequest || isRegistroRequest || isResourceRequest || isErrorPage) {
            System.out.println("=== LoginFilter: Permitindo acesso ===");
            chain.doFilter(request, response);
        } else {
            System.out.println("=== LoginFilter: Redirecionando para login ===");
            httpResponse.sendRedirect(loginURL);
        }
    }

    @Override
    public void destroy() {
        System.out.println("=== LoginFilter destruído ===");
    }
}