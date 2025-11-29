package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.EmpresaDAO;
import com.souzamonteiro.nfe.model.Empresa;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import jakarta.annotation.PostConstruct;
import java.io.Serializable;

@ManagedBean
@ViewScoped
public class EmpresaController implements Serializable {
    
    private EmpresaDAO empresaDAO = new EmpresaDAO();
    private Empresa empresa;
    
    @PostConstruct
    public void init() {
        carregarEmpresa();
    }
    
    public void carregarEmpresa() {
        empresa = empresaDAO.getEmpresa();
        if (empresa == null) {
            empresa = new Empresa();
        }
    }
    
    public void salvarEmpresa() {
        try {
            empresaDAO.save(empresa);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Dados da empresa salvos com sucesso."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao salvar dados da empresa: " + e.getMessage()));
        }
    }
    
    // Getters e Setters
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
}