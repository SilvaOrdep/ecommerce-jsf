package com.souzamonteiro.nfe.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@FacesConverter("numeroConverter")
public class NumeroConverter implements Converter {
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        // Chamado quando o valor é enviado do formulário para o bean
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        // Remove todos os caracteres não numéricos
        String valorLimpo = value.replaceAll("\\D", "");
        System.out.println("Converter getAsObject: '" + value + "' -> '" + valorLimpo + "'");
        return valorLimpo;
    }
    
    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        // Chamado quando o valor é exibido do bean para o formulário
        if (value == null) {
            return "";
        }
        // Converte Object para String
        String stringValue = value.toString();
        if (stringValue.trim().isEmpty()) {
            return "";
        }
        // Pode adicionar formatação aqui se quiser
        return stringValue;
    }
}