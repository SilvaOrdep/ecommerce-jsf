package com.souzamonteiro.nfe.service.integracaoerp;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author pedro
 */
public interface IntegravelComERP {
    
    JSONArray buscarProdutos();
    JSONObject registrarPedito(JSONObject pedido);
    
}
