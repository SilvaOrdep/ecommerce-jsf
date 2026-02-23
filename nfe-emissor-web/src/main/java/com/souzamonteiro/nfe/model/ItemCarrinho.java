package com.souzamonteiro.nfe.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class ItemCarrinho implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Produto produto;
    private BigDecimal quantidade;
    private BigDecimal subtotal;
    
    public ItemCarrinho() {
    }
    
    public ItemCarrinho(Produto produto, BigDecimal quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
        calcularSubtotal();
    }
    
    public void calcularSubtotal() {
        if (produto != null && produto.getVuncom() != null) {
            this.subtotal = produto.getVuncom().multiply(quantidade);
        }
    }
    
    // Getters e Setters
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { 
        this.produto = produto;
        calcularSubtotal();
    }
    
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { 
        this.quantidade = quantidade;
        calcularSubtotal();
    }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}