package com.souzamonteiro.nfe.dao;

import com.souzamonteiro.nfe.model.Empresa;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.Serializable;

public class EmpresaDAO extends GenericDAO<Empresa> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public EmpresaDAO() {
        super(Empresa.class);
    }
    
    public Empresa getEmpresa() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Empresa> query = em.createQuery(
                "SELECT e FROM Empresa e", 
                Empresa.class
            );
            return query.getResultList().stream().findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        } finally {
            em.close();
        }
    }
}