package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.ClienteDAO;
import com.souzamonteiro.nfe.model.Cliente;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import jakarta.annotation.PostConstruct;
import java.io.Serializable;
import java.util.List;

@ManagedBean
@ViewScoped
public class ClienteController implements Serializable {
    
    private ClienteDAO clienteDAO = new ClienteDAO();
    private List<Cliente> clientes;
    private Cliente cliente;
    private Cliente clienteSelecionado;
    private boolean editando;
    
    @PostConstruct
    public void init() {
        carregarClientes();
        novoCliente();
    }
    
    public void carregarClientes() {
        clientes = clienteDAO.findAtivos();
        editando = false;
    }
    
    public void novoCliente() {
        cliente = new Cliente();
        editando = true;
    }
    
    public void editarCliente() {
        if (clienteSelecionado != null) {
            cliente = clienteSelecionado;
            editando = true;
        }
    }
    
    public void salvarCliente() {
        try {
            // Validar CPF/CNPJ
            if (cliente.getCpf() == null && cliente.getCnpj() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Informe CPF ou CNPJ."));
                return;
            }
            
            // Verificar se documento já existe
            String documento = cliente.getDocumento();
            Cliente existente = clienteDAO.findByDocumento(documento);
            if (existente != null && !existente.getId().equals(cliente.getId())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Já existe um cliente com este documento."));
                return;
            }
            
            clienteDAO.save(cliente);
            carregarClientes();
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Cliente salvo com sucesso."));
                
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao salvar cliente: " + e.getMessage()));
        }
    }
    
    public void excluirCliente() {
        if (clienteSelecionado != null) {
            try {
                clienteSelecionado.setAtivo(false);
                clienteDAO.save(clienteSelecionado);
                carregarClientes();
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Sucesso", "Cliente excluído com sucesso."));
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Erro ao excluir cliente: " + e.getMessage()));
            }
        }
    }
    
    public void cancelarEdicao() {
        carregarClientes();
    }
    
    // Getters e Setters
    public List<Cliente> getClientes() { return clientes; }
    public void setClientes(List<Cliente> clientes) { this.clientes = clientes; }
    
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    
    public Cliente getClienteSelecionado() { return clienteSelecionado; }
    public void setClienteSelecionado(Cliente clienteSelecionado) { this.clienteSelecionado = clienteSelecionado; }
    
    public boolean isEditando() { return editando; }
    public void setEditando(boolean editando) { this.editando = editando; }
}