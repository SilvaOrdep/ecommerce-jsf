package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.UsuarioDAO;
import com.souzamonteiro.nfe.model.Usuario;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.List;

@ManagedBean
@ViewScoped
public class UsuarioController implements Serializable {
    
    private transient UsuarioDAO usuarioDAO = new UsuarioDAO();
    private List<Usuario> usuarios;
    private Usuario usuario;
    private Usuario usuarioSelecionado;
    private boolean editando;
    private String confirmarSenha;
    
    public UsuarioController() {
        // Construtor vazio para permitir @PostConstruct
    }
    
    @PostConstruct
    public void init() {
        carregarUsuarios();
        novoUsuario();
    }
    
    public void carregarUsuarios() {
        usuarios = usuarioDAO.findAtivos();
        editando = false;
    }
    
    public void novoUsuario() {
        usuario = new Usuario();
        confirmarSenha = "";
        editando = true;
    }
    
    public void editarUsuario() {
        if (usuarioSelecionado != null) {
            usuario = usuarioSelecionado;
            confirmarSenha = usuario.getSenha();
            editando = true;
        }
    }
    
    public void salvarUsuario() {
        try {
            // Validar senha
            if (usuario.getId() == null && (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Senha é obrigatória para novo usuário."));
                return;
            }
            
            if (!usuario.getSenha().equals(confirmarSenha)) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Senha e confirmação não conferem."));
                return;
            }
            
            // Verificar se login já existe
            Usuario existenteLogin = usuarioDAO.findByLogin(usuario.getLogin());
            if (existenteLogin != null && !existenteLogin.getId().equals(usuario.getId())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Já existe um usuário com este login."));
                return;
            }
            
            // Verificar se email já existe
            Usuario existenteEmail = usuarioDAO.findByEmail(usuario.getEmail());
            if (existenteEmail != null && !existenteEmail.getId().equals(usuario.getId())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Já existe um usuário com este email."));
                return;
            }
            
            usuarioDAO.save(usuario);
            carregarUsuarios();
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Usuário salvo com sucesso."));
                
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao salvar usuário: " + e.getMessage()));
        }
    }
    
    public void excluirUsuario() {
        if (usuarioSelecionado != null) {
            try {
                usuarioSelecionado.setAtivo(false);
                usuarioDAO.save(usuarioSelecionado);
                carregarUsuarios();
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Sucesso", "Usuário excluído com sucesso."));
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Erro ao excluir usuário: " + e.getMessage()));
            }
        }
    }
    
    public void cancelarEdicao() {
        carregarUsuarios();
    }
    
    // Getters e Setters
    public List<Usuario> getUsuarios() { return usuarios; }
    public void setUsuarios(List<Usuario> usuarios) { this.usuarios = usuarios; }
    
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    
    public Usuario getUsuarioSelecionado() { return usuarioSelecionado; }
    public void setUsuarioSelecionado(Usuario usuarioSelecionado) { this.usuarioSelecionado = usuarioSelecionado; }
    
    public boolean isEditando() { return editando; }
    public void setEditando(boolean editando) { this.editando = editando; }
    
    public String getConfirmarSenha() { return confirmarSenha; }
    public void setConfirmarSenha(String confirmarSenha) { this.confirmarSenha = confirmarSenha; }
}