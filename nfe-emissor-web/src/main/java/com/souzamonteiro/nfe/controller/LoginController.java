package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.UsuarioDAO;
import com.souzamonteiro.nfe.model.Usuario;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.annotation.PostConstruct;
import java.io.Serializable;

@ManagedBean(name = "loginController")
@SessionScoped
public class LoginController implements Serializable {
    
    private String login;
    private String senha;
    private Usuario usuarioLogado;
    private transient UsuarioDAO usuarioDAO = new UsuarioDAO();
    
    public LoginController() {
        System.out.println("=== LoginController INSTANCIADO ===");
    }
    
    @PostConstruct
    public void init() {
        System.out.println("=== LoginController @PostConstruct ===");
        System.out.println("loginController hashcode: " + this.hashCode());
    }
    
    public String login() {
        System.out.println("Tentando login com: " + login);
        try {
            // Buscar usuário no banco
            usuarioLogado = usuarioDAO.findByLogin(login);
            System.out.println("Usuário por login: " + usuarioLogado);
            if (usuarioLogado == null) {
                usuarioLogado = usuarioDAO.findByEmail(login);
                System.out.println("Usuário por email: " + usuarioLogado);
            }
            
            // Verificar se usuário existe, está ativo e a senha está correta
            if (usuarioLogado != null && 
                usuarioLogado.getAtivo() != null && 
                usuarioLogado.getAtivo() &&
                usuarioLogado.getSenha() != null && 
                usuarioLogado.getSenha().equals(senha)) {
                
                System.out.println("Login bem sucedido para: " + usuarioLogado.getNome());
                
                // Salvar usuário na sessão
                FacesContext.getCurrentInstance().getExternalContext()
                    .getSessionMap().put("usuarioLogado", usuarioLogado);
                
                // Limpar senha por segurança
                senha = null;
                
                // Login bem sucedido - REDIRECIONAMENTO CORRETO
                return "/index.xhtml?faces-redirect=true";
            } else {
                System.out.println("Login falhou - usuário não encontrado ou senha incorreta");
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Login ou senha inválidos."));
                return null;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao realizar login: " + e.getMessage()));
            return null;
        }
    }
    
    public String logout() {
        System.out.println("Fazendo logout...");
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        usuarioLogado = null;
        login = null;
        senha = null;
        return "/login.xhtml?faces-redirect=true";
    }
    
    // Métodos para o template.xhtml
    public boolean isAdmin() {
        boolean admin = usuarioLogado != null && "ADMIN".equals(usuarioLogado.getPerfil());
        System.out.println("isAdmin() chamado: " + admin);
        return admin;
    }
    
    public boolean isLoggedIn() {
        boolean loggedIn = usuarioLogado != null;
        System.out.println("isLoggedIn() chamado: " + loggedIn);
        return loggedIn;
    }
    
    // Getters para EL expressions (JSF usa getters, não métodos booleanos diretamente)
    public boolean getAdmin() {
        return isAdmin();
    }
    
    public boolean getLoggedIn() {
        return isLoggedIn();
    }
    
    // Getters e Setters
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    
    public Usuario getUsuarioLogado() { return usuarioLogado; }
    public void setUsuarioLogado(Usuario usuarioLogado) { this.usuarioLogado = usuarioLogado; }
}