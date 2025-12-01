package com.souzamonteiro.nfe.dao;

import com.souzamonteiro.nfe.model.Configuracao;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.Serializable;

public class ConfiguracaoDAO extends GenericDAO<Configuracao> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public ConfiguracaoDAO() {
        super(Configuracao.class);
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