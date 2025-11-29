package com.souzamonteiro.nfe.dao;

import com.souzamonteiro.nfe.model.Produto;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class ProdutoDAO extends GenericDAO<Produto, Long> {
    
    public ProdutoDAO() {
        super(Produto.class);
    }
    
    @Override
    protected boolean isNew(Produto produto) {
        return produto.getId() == null;
    }
    
    public List<Produto> findAtivos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Produto> query = em.createQuery(
                "SELECT p FROM Produto p WHERE p.ativo = true ORDER BY p.xprod", 
                Produto.class
            );
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public Produto findByCodigo(String codigo) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Produto> query = em.createQuery(
                "SELECT p FROM Produto p WHERE p.cprod = :codigo", 
                Produto.class
            );
            query.setParameter("codigo", codigo);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        } finally {
            em.close();
        }
    }
}