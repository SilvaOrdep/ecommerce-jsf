package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.ProdutoDAO;
import com.souzamonteiro.nfe.model.Produto;
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
public class ProdutoController implements Serializable {
    
    private transient ProdutoDAO produtoDAO = new ProdutoDAO();
    private List<Produto> produtos;
    private Produto produto;
    private Produto produtoSelecionado;
    private boolean editando = false; // Inicialize como false
    
    @PostConstruct
    public void init() {
        carregarProdutos();
    }
    
    public void carregarProdutos() {
        produtos = produtoDAO.findAtivos();
        editando = false;
        produtoSelecionado = null;
        produto = null;
    }
    
    public void novoProduto() {
        produto = new Produto();
        produto.setAtivo(true);
        editando = true;
        produtoSelecionado = null;
    }
    
    public void editarProduto() {
        if (produtoSelecionado != null) {
            produto = produtoSelecionado;
            editando = true;
        } else {
            FacesContext.getCurrentInstance().addMessage("form:growl",
                new FacesMessage(FacesMessage.SEVERITY_WARN, 
                "Aviso", "Selecione um produto para editar."));
        }
    }
    
    public void salvarProduto() {
        try {
            // Verificar se código já existe
            Produto existente = produtoDAO.findByCodigo(produto.getCprod());
            if (existente != null && !existente.getId().equals(produto.getId())) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Já existe um produto com este código."));
                return;
            }
            
            produtoDAO.save(produto);
            carregarProdutos();
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Produto salvo com sucesso."));
                
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao salvar produto: " + e.getMessage()));
        }
    }
    
    public void excluirProduto() {
        if (produtoSelecionado != null) {
            try {
                produtoSelecionado.setAtivo(false);
                produtoDAO.save(produtoSelecionado);
                carregarProdutos();
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Sucesso", "Produto excluído com sucesso."));
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Erro ao excluir produto: " + e.getMessage()));
            }
        }
    }
    
    public void cancelarEdicao() {
        carregarProdutos();
    }
    
    // Métodos para manipular seleção no dataTable
    public void onRowSelect(SelectEvent<Produto> event) {
        produtoSelecionado = event.getObject();
        FacesContext.getCurrentInstance().addMessage("form:growl",
            new FacesMessage(FacesMessage.SEVERITY_INFO, 
            "Selecionado", "Usuário selecionado: " + produtoSelecionado.getXprod()));
    }
    
    public void onRowUnselect(UnselectEvent<Produto> event) {
        produtoSelecionado = null;
    }
    
    // Getters e Setters
    public List<Produto> getProdutos() { return produtos; }
    public void setProdutos(List<Produto> produtos) { this.produtos = produtos; }
    
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    
    public Produto getProdutoSelecionado() { return produtoSelecionado; }
    public void setProdutoSelecionado(Produto produtoSelecionado) { this.produtoSelecionado = produtoSelecionado; }
    
    public boolean isEditando() { return editando; }
    public void setEditando(boolean editando) { this.editando = editando; }
}