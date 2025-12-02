package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.UsuarioDAO;
import com.souzamonteiro.nfe.model.Usuario;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.annotation.PostConstruct;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import java.io.Serializable;
import java.util.List;

@ManagedBean
@ViewScoped
public class UsuarioController implements Serializable {
    
    private transient UsuarioDAO usuarioDAO = new UsuarioDAO();
    private List<Usuario> usuarios;
    private Usuario usuario;
    private Usuario usuarioSelecionado;
    private boolean editando = false;
    private String confirmarSenha;
    
    @PostConstruct
    public void init() {
        carregarUsuarios();
    }
    
    public void carregarUsuarios() {
        usuarios = usuarioDAO.findAtivos();
        editando = false;
        usuarioSelecionado = null; // Limpar seleção
    }
    
    public void novoUsuario() {
        usuario = new Usuario();
        usuario.setAtivo(true);
        usuario.setPerfil("USER");
        confirmarSenha = "";
        editando = true;
        usuarioSelecionado = null; // Limpar seleção
    }
    
    public void editarUsuario() {
        if (usuarioSelecionado != null) {
            usuario = usuarioSelecionado;
            confirmarSenha = usuario.getSenha();
            editando = true;
        } else {
            FacesContext.getCurrentInstance().addMessage("form:growl",
                new FacesMessage(FacesMessage.SEVERITY_WARN, 
                "Aviso", "Selecione um usuário para editar."));
        }
    }
    
    public void salvarUsuario() {
        try {
            // Validar senha
            if (usuario.getId() == null && (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty())) {
                FacesContext.getCurrentInstance().addMessage("form:growl",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Senha é obrigatória para novo usuário."));
                return;
            }
            
            if (!usuario.getSenha().equals(confirmarSenha)) {
                FacesContext.getCurrentInstance().addMessage("form:growl",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Senha e confirmação não conferem."));
                return;
            }
            
            // Verificar se login já existe
            Usuario existenteLogin = usuarioDAO.findByLogin(usuario.getLogin());
            if (existenteLogin != null) {
                // Se for novo usuário (id é null) OU se for um usuário diferente
                if (usuario.getId() == null || !usuario.getId().equals(existenteLogin.getId())) {
                    FacesContext.getCurrentInstance().addMessage("form:growl",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Erro", "Já existe um usuário com este login."));
                    return;
                }
            }
            
            // Verificar se email já existe
            Usuario existenteEmail = usuarioDAO.findByEmail(usuario.getEmail());
            if (existenteEmail != null) {
                // Se for novo usuário (id é null) OU se for um usuário diferente
                if (usuario.getId() == null || !usuario.getId().equals(existenteEmail.getId())) {
                    FacesContext.getCurrentInstance().addMessage("form:growl",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Erro", "Já existe um usuário com este email."));
                    return;
                }
            }
            
            usuarioDAO.save(usuario);
            carregarUsuarios();
            
            FacesContext.getCurrentInstance().addMessage("form:growl",
                new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Usuário salvo com sucesso."));
                
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage("form:growl",
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao salvar usuário: " + e.getMessage()));
        }
    }
    
    public void excluirUsuario() {
        if (usuarioSelecionado != null) {
            try {
                // Não permitir excluir a si mesmo (opcional)
                FacesContext context = FacesContext.getCurrentInstance();
                String usuarioLogado = (String) context.getExternalContext().getSessionMap().get("usuarioLogado");
                
                if (usuarioSelecionado.getLogin().equals(usuarioLogado)) {
                    FacesContext.getCurrentInstance().addMessage("form:growl",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Erro", "Você não pode excluir seu próprio usuário."));
                    return;
                }
                
                usuarioSelecionado.setAtivo(false);
                usuarioDAO.save(usuarioSelecionado);
                carregarUsuarios();
                
                FacesContext.getCurrentInstance().addMessage("form:growl",
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Sucesso", "Usuário excluído com sucesso."));
            } catch (Exception e) {
                e.printStackTrace();
                FacesContext.getCurrentInstance().addMessage("form:growl",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Erro ao excluir usuário: " + e.getMessage()));
            }
        } else {
            FacesContext.getCurrentInstance().addMessage("form:growl",
                new FacesMessage(FacesMessage.SEVERITY_WARN, 
                "Aviso", "Selecione um usuário para excluir."));
        }
    }
    
    public void cancelarEdicao() {
        carregarUsuarios();
    }
    
    // Métodos para manipular seleção no dataTable
    public void onRowSelect(SelectEvent<Usuario> event) {
        usuarioSelecionado = event.getObject();
        FacesContext.getCurrentInstance().addMessage("form:growl",
            new FacesMessage(FacesMessage.SEVERITY_INFO, 
            "Selecionado", "Usuário selecionado: " + usuarioSelecionado.getNome()));
    }
    
    public void onRowUnselect(UnselectEvent<Usuario> event) {
        usuarioSelecionado = null;
    }
    
    // Getters e Setters
    public List<Usuario> getUsuarios() { 
        return usuarios; 
    }
    
    public void setUsuarios(List<Usuario> usuarios) { 
        this.usuarios = usuarios; 
    }
    
    public Usuario getUsuario() { 
        if (usuario == null) {
            usuario = new Usuario();
        }
        return usuario; 
    }
    
    public void setUsuario(Usuario usuario) { 
        this.usuario = usuario; 
    }
    
    public Usuario getUsuarioSelecionado() { 
        return usuarioSelecionado; 
    }
    
    public void setUsuarioSelecionado(Usuario usuarioSelecionado) { 
        this.usuarioSelecionado = usuarioSelecionado; 
    }
    
    public boolean isEditando() { 
        return editando; 
    }
    
    public void setEditando(boolean editando) { 
        this.editando = editando; 
    }
    
    public String getConfirmarSenha() { 
        return confirmarSenha; 
    }
    
    public void setConfirmarSenha(String confirmarSenha) { 
        this.confirmarSenha = confirmarSenha; 
    }
}