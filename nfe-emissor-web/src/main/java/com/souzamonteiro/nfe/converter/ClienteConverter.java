package com.souzamonteiro.nfe.converter;

import com.souzamonteiro.nfe.dao.ClienteDAO;
import com.souzamonteiro.nfe.model.Cliente;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import java.io.Serializable;

@FacesConverter(forClass = Cliente.class, value = "clienteConverter")
public class ClienteConverter implements Converter, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Use uma instância transient ou CDI se disponível
    private transient ClienteDAO clienteDAO;
    
    private ClienteDAO getClienteDAO() {
        if (clienteDAO == null) {
            clienteDAO = new ClienteDAO();
        }
        return clienteDAO;
    }
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        System.out.println("DEBUG ClienteConverter.getAsObject: value = '" + value + "'");
        
        if (value == null || value.trim().isEmpty() || "null".equals(value.trim())) {
            System.out.println("DEBUG: Valor nulo ou vazio, retornando null");
            return null;
        }
        
        try {
            Integer id = Integer.parseInt(value.trim());
            System.out.println("DEBUG: Buscando cliente com ID = " + id);
            
            Cliente cliente = getClienteDAO().findById(id);
            System.out.println("DEBUG: Cliente encontrado: " + 
                (cliente != null ? cliente.getXnome() + " (ID: " + cliente.getId() + ")" : "null"));
            
            return cliente;
        } catch (NumberFormatException e) {
            System.err.println("ERROR ClienteConverter.getAsObject: Não é um número válido: '" + value + "'");
            return null;
        } catch (Exception e) {
            System.err.println("ERROR ClienteConverter.getAsObject: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        System.out.println("DEBUG ClienteConverter.getAsString: value = " + 
            (value != null ? value.getClass().getName() + ": " + value.toString() : "null"));
        
        if (value == null) {
            return "";
        }
        
        if (value instanceof Cliente) {
            Cliente cliente = (Cliente) value;
            String idStr = (cliente.getId() != null) ? cliente.getId().toString() : "";
            System.out.println("DEBUG: Convertendo Cliente para string: ID = " + idStr);
            return idStr;
        } else if (value instanceof String) {
            // Já é uma string, retorna como está
            return (String) value;
        } else {
            System.err.println("ERROR: Tipo não suportado: " + value.getClass().getName());
            return "";
        }
    }
}