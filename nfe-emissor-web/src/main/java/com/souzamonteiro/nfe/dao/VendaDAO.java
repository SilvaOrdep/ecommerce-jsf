package com.souzamonteiro.nfe.dao;

import com.souzamonteiro.nfe.model.Venda;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class VendaDAO extends GenericDAO<Venda, Long> {
    
    public VendaDAO() {
        super(Venda.class);
    }
    
    @Override
    protected boolean isNew(Venda venda) {
        return venda.getId() == null;
    }
    
    public List<Venda> findPendentes() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Venda> query = em.createQuery(
                "SELECT v FROM Venda v WHERE v.status = 'PENDENTE' ORDER BY v.dataVenda", 
                Venda.class
            );
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<Venda> findEmitidas() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Venda> query = em.createQuery(
                "SELECT v FROM Venda v WHERE v.status = 'EMITIDA' ORDER BY v.dataVenda DESC", 
                Venda.class
            );
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public Integer getProximoNumeroNFe() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Integer> query = em.createQuery(
                "SELECT COALESCE(MAX(v.numeroNFe), 0) + 1 FROM Venda v WHERE v.numeroNFe IS NOT NULL", 
                Integer.class
            );
            return query.getSingleResult();
        } catch (Exception e) {
            return 1;
        } finally {
            em.close();
        }
    }
}