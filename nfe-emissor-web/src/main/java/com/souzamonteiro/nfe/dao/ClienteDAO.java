package com.souzamonteiro.nfe.dao;

import com.souzamonteiro.nfe.model.Cliente;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class ClienteDAO extends GenericDAO<Cliente, Long> {
    
    public ClienteDAO() {
        super(Cliente.class);
    }
    
    @Override
    protected boolean isNew(Cliente cliente) {
        return cliente.getId() == null;
    }
    
    public List<Cliente> findAtivos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Cliente> query = em.createQuery(
                "SELECT c FROM Cliente c WHERE c.ativo = true ORDER BY c.xnome", 
                Cliente.class
            );
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public Cliente findByDocumento(String documento) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Cliente> query = em.createQuery(
                "SELECT c FROM Cliente c WHERE c.cpf = :doc OR c.cnpj = :doc", 
                Cliente.class
            );
            query.setParameter("doc", documento);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        } finally {
            em.close();
        }
    }
}