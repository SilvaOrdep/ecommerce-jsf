package com.souzamonteiro.nfe.controller;

import com.souzamonteiro.nfe.dao.ConfiguracaoDAO;
import com.souzamonteiro.nfe.model.Configuracao;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.annotation.PostConstruct;
import java.io.Serializable;

@ManagedBean
@ViewScoped
public class ConfiguracaoController implements Serializable {
    
    private transient ConfiguracaoDAO configuracaoDAO = new ConfiguracaoDAO();
    private Configuracao configuracao;
    
    public ConfiguracaoController() {
        // Construtor vazio para permitir @PostConstruct
    }
    
    @PostConstruct
    public void init() {
        carregarConfiguracao();
    }
    
    public void carregarConfiguracao() {
        configuracao = configuracaoDAO.getConfiguracao();
        if (configuracao == null) {
            configuracao = new Configuracao();
        }
    }
    
    public void salvarConfiguracao() {
        try {
            configuracaoDAO.save(configuracao);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Configurações salvas com sucesso."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao salvar configurações: " + e.getMessage()));
        }
    }
    
    // Getters e Setters
    public Configuracao getConfiguracao() { return configuracao; }
    public void setConfiguracao(Configuracao configuracao) { this.configuracao = configuracao; }
}