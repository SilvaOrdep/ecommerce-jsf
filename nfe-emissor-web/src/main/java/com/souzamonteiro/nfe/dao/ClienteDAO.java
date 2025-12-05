package com.souzamonteiro.nfe.dao;

import com.souzamonteiro.nfe.model.Cliente;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;

public class ClienteDAO extends GenericDAO<Cliente> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public ClienteDAO() {
        super(Cliente.class);
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
        // Se documento for null ou vazio, retorna null IMEDIATAMENTE
        if (documento == null || documento.trim().isEmpty()) {
            System.out.println("DEBUG findByDocumento: documento nulo ou vazio, retornando null");
            return null;
        }

        EntityManager em = getEntityManager();
        try {
            System.out.println("DEBUG findByDocumento: buscando documento = '" + documento + "'");

            // Query que IGNORA registros onde cpf/cnpj são NULL ou vazios
            TypedQuery<Cliente> query = em.createQuery(
                "SELECT c FROM Cliente c WHERE " +
                "c.ativo = true AND " +  // Só clientes ativos
                "((c.cpf IS NOT NULL AND c.cpf = :doc) OR " +
                "(c.cnpj IS NOT NULL AND c.cnpj = :doc))", 
                Cliente.class
            );
            query.setParameter("doc", documento);

            List<Cliente> resultados = query.getResultList();
            System.out.println("DEBUG findByDocumento: encontrados " + resultados.size() + " resultados");

            if (resultados.isEmpty()) {
                return null;
            } else if (resultados.size() > 1) {
                // Log detalhado do problema
                System.err.println("ERRO: Múltiplos clientes ativos com documento '" + documento + "':");
                for (Cliente c : resultados) {
                    System.err.println("  - ID: " + c.getId() + 
                                     ", Nome: " + c.getXnome() + 
                                     ", CPF: '" + c.getCpf() + 
                                     "', CNPJ: '" + c.getCnpj() + "'");
                }
                // Para não quebrar, retorna o primeiro
                return resultados.get(0);
            } else {
                return resultados.get(0);
            }

        } catch (Exception e) {
            System.err.println("ERRO em findByDocumento: " + e.getMessage());
            return null;
        } finally {
            em.close();
        }
    }
}