package com.souzamonteiro.nfe.dao;

import com.souzamonteiro.nfe.model.Empresa;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

public class EmpresaDAO extends GenericDAO<Empresa, Long> {
    
    public EmpresaDAO() {
        super(Empresa.class);
    }
    
    @Override
    protected boolean isNew(Empresa empresa) {
        return empresa.getId() == null;
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