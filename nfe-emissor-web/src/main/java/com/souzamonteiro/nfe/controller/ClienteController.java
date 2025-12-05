package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.ClienteDAO;
import com.souzamonteiro.nfe.model.Cliente;
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
public class ClienteController implements Serializable {
    
    private transient ClienteDAO clienteDAO = new ClienteDAO();
    private List<Cliente> clientes;
    private Cliente cliente;
    private Cliente clienteSelecionado;
    private boolean editando = false; // Inicialize como false
    
    @PostConstruct
    public void init() {
        carregarClientes();
    }
    
    public void carregarClientes() {
        clientes = clienteDAO.findAtivos();
        editando = false;
        clienteSelecionado = null;
        cliente = null;
    }
    
    public void novoCliente() {
        cliente = new Cliente();
        cliente.setAtivo(true);
        editando = true;
        clienteSelecionado = null;
    }
    
    public void editarCliente() {
        if (clienteSelecionado != null) {
            cliente = clienteSelecionado;
            editando = true;
        } else {
            FacesContext.getCurrentInstance().addMessage("form:growl",
                new FacesMessage(FacesMessage.SEVERITY_WARN, 
                "Aviso", "Selecione um cliente para editar."));
        }
    }
    
    public void salvarCliente() {
        try {
            // Validar CPF/CNPJ
            if ((cliente.getCpf() == null || cliente.getCpf().trim().isEmpty()) && 
                (cliente.getCnpj() == null || cliente.getCnpj().trim().isEmpty())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Informe CPF ou CNPJ."));
                return;
            }
            
            // Verificar se documento já existe
            String documento = cliente.getCpf() != null ? cliente.getCpf() : cliente.getCnpj();
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
        } else {
            FacesContext.getCurrentInstance().addMessage("form:growl",
                new FacesMessage(FacesMessage.SEVERITY_WARN, 
                "Aviso", "Selecione um cliente para excluir."));
        }
    }
    
    public void cancelarEdicao() {
        carregarClientes();
    }
    
    // Métodos para manipular seleção no dataTable
    public void onRowSelect(SelectEvent<Cliente> event) {
        clienteSelecionado = event.getObject();
        FacesContext.getCurrentInstance().addMessage("form:growl",
            new FacesMessage(FacesMessage.SEVERITY_INFO, 
            "Selecionado", "Usuário selecionado: " + clienteSelecionado.getXnome()));
    }
    
    public void onRowUnselect(UnselectEvent<Cliente> event) {
        clienteSelecionado = null;
    }
    
    // Getters e Setters
    public List<Cliente> getClientes() { 
        return clientes; 
    }
    
    public void setClientes(List<Cliente> clientes) { 
        this.clientes = clientes; 
    }
    
    public Cliente getCliente() { 
        if (cliente == null) {
            cliente = new Cliente();
        }
        return cliente; 
    }
    
    public void setCliente(Cliente cliente) { 
        this.cliente = cliente; 
    }
    
    public Cliente getClienteSelecionado() { 
        return clienteSelecionado; 
    }
    
    public void setClienteSelecionado(Cliente clienteSelecionado) { 
        this.clienteSelecionado = clienteSelecionado; 
    }
    
    public boolean isEditando() { 
        return editando; 
    }
    
    public void setEditando(boolean editando) { 
        this.editando = editando; 
    }
}