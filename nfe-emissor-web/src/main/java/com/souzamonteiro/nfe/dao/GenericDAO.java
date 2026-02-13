package com.souzamonteiro.nfe.dao;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.io.Serializable;
import java.util.List;

import com.souzamonteiro.nfe.util.HibernateUtil;

public class GenericDAO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Class<T> clazz;

    public GenericDAO(Class<T> clazz) {
        this.clazz = clazz;
    }

    protected EntityManager getEntityManager() {
        return HibernateUtil.getEntityManager();
    }

    public void save(T entity) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = null;

        try {
            tx = em.getTransaction();
            tx.begin();

            // Verificar se é novo (não tem ID) ou se já existe
            Object id = null;
            try {
                id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
            } catch (Exception ignored) {
            }

            boolean isNew = (id == null);

            if (isNew) {
                em.persist(entity);
            } else {
                entity = em.merge(entity);
            }

            tx.commit();

            // IMPORTANTE: Após commit, fazer merge para reattach a entidade e obter ID
            // atualizado
            // Isso é necessário quando o banco gera o ID automaticamente (IDENTITY)
            if (isNew && em.isOpen()) {
                entity = em.merge(entity);
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    public T findById(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(clazz, id);
        } finally {
            em.close();
        }
    }

    public T findById(Object id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(clazz, id);
        } finally {
            em.close();
        }
    }

    // Ou específico para Integer:
    public T findById(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(clazz, id);
        } finally {
            em.close();
        }
    }

    public List<T> findAll() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("FROM " + clazz.getSimpleName(), clazz).getResultList();
        } finally {
            em.close();
        }
    }

    public void update(T entity) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = null;

        try {
            tx = em.getTransaction();
            tx.begin();
            em.merge(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(Long id) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = null;

        try {
            tx = em.getTransaction();
            tx.begin();
            T entity = em.find(clazz, id);
            if (entity != null) {
                em.remove(entity);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}