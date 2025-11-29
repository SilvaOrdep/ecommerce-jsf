package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.UsuarioDAO;
import com.souzamonteiro.nfe.model.Usuario;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.Serializable;

@ManagedBean
@SessionScoped
public class LoginController implements Serializable {
    
    private String login;
    private String senha;
    private Usuario usuarioLogado;
    private UsuarioDAO usuarioDAO = new UsuarioDAO();
    
    public String login() {
        try {
            // Buscar usuário no banco
            usuarioLogado = usuarioDAO.findByLogin(login);
            if (usuarioLogado == null) {
                usuarioLogado = usuarioDAO.findByEmail(login);
            }
            
            if (usuarioLogado != null && usuarioLogado.getAtivo() 
                && usuarioLogado.getSenha().equals(senha)) {
                return "index?faces-redirect=true";
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Login ou senha inválidos."));
                return null;
            }
            
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao realizar login: " + e.getMessage()));
            return null;
        }
    }
    
    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        usuarioLogado = null;
        login = null;
        senha = null;
        return "/login?faces-redirect=true";
    }
    
    // Métodos para o template.xhtml
    public boolean isAdmin() {
        return usuarioLogado != null && "ADMIN".equals(usuarioLogado.getPerfil());
    }
    
    public boolean isLoggedIn() {
        return usuarioLogado != null;
    }
    
    // Getters para o template.xhtml (propriedades)
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