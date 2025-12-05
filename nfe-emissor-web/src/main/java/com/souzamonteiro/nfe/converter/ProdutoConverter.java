package com.souzamonteiro.nfe.converter;

import com.souzamonteiro.nfe.dao.ProdutoDAO;
import com.souzamonteiro.nfe.model.Produto;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import java.io.Serializable;

@FacesConverter(forClass = Produto.class, value = "produtoConverter")
public class ProdutoConverter implements Converter, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private ProdutoDAO produtoDAO = new ProdutoDAO();
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            Integer id = Integer.parseInt(value);
            return produtoDAO.findById(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "";
        }
        
        if (value instanceof Produto) {
            Produto produto = (Produto) value;
            if (produto.getId() != null) {
                return produto.getId().toString();
            }
        }
        
        return "";
    }
}