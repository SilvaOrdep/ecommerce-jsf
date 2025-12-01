package com.souzamonteiro.nfe.dao;

import com.souzamonteiro.nfe.model.Usuario;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;

public class UsuarioDAO extends GenericDAO<Usuario> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public UsuarioDAO() {
        super(Usuario.class);
    }
    
    public List<Usuario> findAtivos() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Usuario> query = em.createQuery(
                "SELECT u FROM Usuario u WHERE u.ativo = true ORDER BY u.nome", 
                Usuario.class
            );
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public Usuario findByLogin(String login) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Usuario> query = em.createQuery(
                "SELECT u FROM Usuario u WHERE u.login = :login", 
                Usuario.class
            );
            query.setParameter("login", login);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }
    
    public Usuario findByEmail(String email) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Usuario> query = em.createQuery(
                "SELECT u FROM Usuario u WHERE u.email = :email", 
                Usuario.class
            );
            query.setParameter("email", email);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }
}