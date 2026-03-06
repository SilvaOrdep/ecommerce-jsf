package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.SincronizacaoERPDAO;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;

@ManagedBean
@ViewScoped
public class SincronizacaoERPController implements Serializable {
    
    private transient SincronizacaoERPDAO sincronizacaoDAO = new SincronizacaoERPDAO();
    
    public void sincronizarProdutosDoERP() {
        try {
            if (sincronizacaoDAO.sincronizarProdutos()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Sucesso", "Produtos sincronizados com sucesso!"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erro", "Falha ao sincronizar produtos"));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erro", "Erro: " + e.getMessage()));
        }
    }
}