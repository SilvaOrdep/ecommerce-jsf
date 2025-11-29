package com.souzamonteiro.nfe.dao;

import com.souzamonteiro.nfe.model.Configuracao;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

public class ConfiguracaoDAO extends GenericDAO<Configuracao, Long> {
    
    public ConfiguracaoDAO() {
        super(Configuracao.class);
    }
    
    @Override
    protected boolean isNew(Configuracao configuracao) {
        return configuracao.getId() == null;
    }
    
    public Configuracao getConfiguracao() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Configuracao> query = em.createQuery(
                "SELECT c FROM Configuracao c", 
                Configuracao.class
            );
            return query.getResultList().stream().findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        } finally {
            em.close();
        }
    }
}