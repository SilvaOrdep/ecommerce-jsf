package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.ClienteDAO;
import com.souzamonteiro.nfe.dao.UsuarioDAO;
import com.souzamonteiro.nfe.model.Cliente;
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
    private String confirmarSenha = "";

    private Cliente cliente;
    private transient ClienteDAO clienteDAO = new ClienteDAO();

    @PostConstruct
    public void init() {
        carregarUsuarios();
        cliente = new Cliente();
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

            if (validarUsuario()) {
                usuarioDAO.save(usuario);
                carregarUsuarios();

                FacesContext.getCurrentInstance().addMessage("form:growl",
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Sucesso", "Usuário salvo com sucesso."));
            }

        } catch (Exception e) {
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
                Usuario usuarioLogado = (Usuario) context.getExternalContext().getSessionMap().get("usuarioLogado");

                if (usuarioSelecionado.getLogin().equals(usuarioLogado.getLogin())) {
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

    public void registrarClienteUsuario() {
        try {
            if (validarUsuario() && validarCliente()) {
                usuario.setPerfil("USER");

                String cpfCnpj = cliente.getCpf() != null ? cliente.getCpf() : cliente.getCnpj();
                String emailCliente = usuario.getEmail();
                String nomeCliente = usuario.getNome();

                usuarioDAO.save(usuario);
                System.out.println("Salvou usuario");

                Cliente novoCliente = new Cliente();
                novoCliente.setAtivo(true);
                novoCliente.setEmail(emailCliente);
                novoCliente.setXnome(nomeCliente);
                novoCliente.setCpf(cliente.getCpf());
                novoCliente.setCnpj(cliente.getCnpj());
                
                novoCliente.setXlgr("S/N");
                novoCliente.setXbairro("S/N");
                novoCliente.setNro("S/N");
                novoCliente.setXbairro("S/N");
                novoCliente.setCmun("S/N");
                novoCliente.setXmun("S/N");
                novoCliente.setUf("SN");
                novoCliente.setCep("S/N");

                clienteDAO.save(novoCliente);
                System.out.println("Salvou cliente " + novoCliente.toString());

                cliente = new Cliente();
                usuario = new Usuario();
                confirmarSenha = "";
                editando = false;
                carregarUsuarios();

                FacesContext.getCurrentInstance().addMessage("form:growl",
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Sucesso", "Usuário salvo com sucesso."));
            }

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage("form:growl",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erro", "Erro ao salvar usuário: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public void cancelarEdicao() {
        carregarUsuarios();
    }

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

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    private boolean validarUsuario() {
        try {
            // Validar senha
            if (usuario.getId() == null && (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty())) {
                FacesContext.getCurrentInstance().addMessage("form:growl",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Erro", "Senha é obrigatória para novo usuário."));
                return false;
            }

            if (usuarioSelecionado == null) {
                if (!usuario.getSenha().equals(confirmarSenha)) {
                    FacesContext.getCurrentInstance().addMessage("form:growl",
                            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                    "Erro", "Senha e confirmação não conferem."));
                    return false;
                }
            } else {
                if (!confirmarSenha.equals("")) {
                    if (!usuario.getSenha().equals(confirmarSenha)) {
                        FacesContext.getCurrentInstance().addMessage("form:growl",
                                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                        "Erro", "Senha e confirmação não conferem."));
                        return false;
                    }
                }
            }

            // Verificar se login já existe
            Usuario existenteLogin = usuarioDAO.findByLogin(usuario.getLogin());
            if (existenteLogin != null) {
                // Se for novo usuário (id é null) OU se for um usuário diferente
                if (usuario.getId() == null || !usuario.getId().equals(existenteLogin.getId())) {
                    FacesContext.getCurrentInstance().addMessage("form:growl",
                            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                    "Erro", "Já existe um usuário com este login."));
                    return false;
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
                    return false;
                }
            }

            if (usuario.getAtivo() == null)
                usuario.setAtivo(true);

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage("form:growl",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erro", "Erro ao salvar usuário: " + e.getMessage()));
        }
        System.out.println("validou usuario");
        return true;
    }

    private boolean validarCliente() {
        try {
            if ((cliente.getCpf() == null || cliente.getCpf().trim().isEmpty()) &&
                    (cliente.getCnpj() == null || cliente.getCnpj().trim().isEmpty())) {
                System.out.println("CPF: " + cliente.getCpf());
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Erro", "Informe CPF ou CNPJ."));
                return false;
            }

            // Verificar se documento já existe
            String documento = cliente.getCpf() != null ? cliente.getCpf() : cliente.getCnpj();
            Cliente existente = clienteDAO.findByDocumento(documento);
            if (existente != null && !existente.getId().equals(cliente.getId())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Erro", "Já existe um cliente com este documento."));
                return false;
            }

            return true;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage("form:growl",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erro", "Erro ao salvar usuário: " + e.getMessage()));
        }
        return false;
    }
}