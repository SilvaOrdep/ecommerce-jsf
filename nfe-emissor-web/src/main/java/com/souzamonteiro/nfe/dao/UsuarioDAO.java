package com.souzamonteiro.nfe.dao;

import com.souzamonteiro.nfe.model.Usuario;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class UsuarioDAO extends GenericDAO<Usuario, Long> {
    
    public UsuarioDAO() {
        super(Usuario.class);
    }
    
    @Override
    protected boolean isNew(Usuario usuario) {
        return usuario.getId() == null;
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
                "SELECT u FROM Usuario u WHERE u.login = :login AND u.ativo = true", 
                Usuario.class
            );
            query.setParameter("login", login);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        } finally {
            em.close();
        }
    }
    
    public Usuario findByEmail(String email) {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Usuario> query = em.createQuery(
                "SELECT u FROM Usuario u WHERE u.email = :email AND u.ativo = true", 
                Usuario.class
            );
            query.setParameter("email", email);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        } finally {
            em.close();
        }
    }
}