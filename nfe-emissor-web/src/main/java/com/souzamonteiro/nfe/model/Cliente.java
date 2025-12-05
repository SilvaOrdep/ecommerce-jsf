/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.souzamonteiro.nfe.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author roberto
 */
@Entity
@Table(name = "cliente")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Cliente.findAll", query = "SELECT c FROM Cliente c"),
    @NamedQuery(name = "Cliente.findById", query = "SELECT c FROM Cliente c WHERE c.id = :id"),
    @NamedQuery(name = "Cliente.findByXnome", query = "SELECT c FROM Cliente c WHERE c.xnome = :xnome"),
    @NamedQuery(name = "Cliente.findByCpf", query = "SELECT c FROM Cliente c WHERE c.cpf = :cpf"),
    @NamedQuery(name = "Cliente.findByCnpj", query = "SELECT c FROM Cliente c WHERE c.cnpj = :cnpj"),
    @NamedQuery(name = "Cliente.findByIe", query = "SELECT c FROM Cliente c WHERE c.ie = :ie"),
    @NamedQuery(name = "Cliente.findByIndiedest", query = "SELECT c FROM Cliente c WHERE c.indiedest = :indiedest"),
    @NamedQuery(name = "Cliente.findByXlgr", query = "SELECT c FROM Cliente c WHERE c.xlgr = :xlgr"),
    @NamedQuery(name = "Cliente.findByNro", query = "SELECT c FROM Cliente c WHERE c.nro = :nro"),
    @NamedQuery(name = "Cliente.findByXcpl", query = "SELECT c FROM Cliente c WHERE c.xcpl = :xcpl"),
    @NamedQuery(name = "Cliente.findByXbairro", query = "SELECT c FROM Cliente c WHERE c.xbairro = :xbairro"),
    @NamedQuery(name = "Cliente.findByCmun", query = "SELECT c FROM Cliente c WHERE c.cmun = :cmun"),
    @NamedQuery(name = "Cliente.findByXmun", query = "SELECT c FROM Cliente c WHERE c.xmun = :xmun"),
    @NamedQuery(name = "Cliente.findByUf", query = "SELECT c FROM Cliente c WHERE c.uf = :uf"),
    @NamedQuery(name = "Cliente.findByCep", query = "SELECT c FROM Cliente c WHERE c.cep = :cep"),
    @NamedQuery(name = "Cliente.findByFone", query = "SELECT c FROM Cliente c WHERE c.fone = :fone"),
    @NamedQuery(name = "Cliente.findByEmail", query = "SELECT c FROM Cliente c WHERE c.email = :email"),
    @NamedQuery(name = "Cliente.findByAtivo", query = "SELECT c FROM Cliente c WHERE c.ativo = :ativo"),
    @NamedQuery(name = "Cliente.findByDataCriacao", query = "SELECT c FROM Cliente c WHERE c.dataCriacao = :dataCriacao"),
    @NamedQuery(name = "Cliente.findByDataAtualizacao", query = "SELECT c FROM Cliente c WHERE c.dataAtualizacao = :dataAtualizacao")})
public class Cliente implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @Column(name = "xnome")
    private String xnome;
    @Column(name = "cpf")
    private String cpf;
    @Column(name = "cnpj")
    private String cnpj;
    @Column(name = "ie")
    private String ie;
    @Column(name = "indiedest")
    private String indiedest;
    @Basic(optional = false)
    @Column(name = "xlgr")
    private String xlgr;
    @Basic(optional = false)
    @Column(name = "nro")
    private String nro;
    @Column(name = "xcpl")
    private String xcpl;
    @Basic(optional = false)
    @Column(name = "xbairro")
    private String xbairro;
    @Basic(optional = false)
    @Column(name = "cmun")
    private String cmun;
    @Basic(optional = false)
    @Column(name = "xmun")
    private String xmun;
    @Basic(optional = false)
    @Column(name = "uf")
    private String uf;
    @Basic(optional = false)
    @Column(name = "cep")
    private String cep;
    @Column(name = "fone")
    private String fone;
    @Column(name = "email")
    private String email;
    @Column(name = "ativo")
    private Boolean ativo;
    @Column(name = "data_criacao")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCriacao;
    @Column(name = "data_atualizacao")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataAtualizacao;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "clienteId")
    private Collection<Venda> vendaCollection;

    public Cliente() {
    }

    public Cliente(Integer id) {
        this.id = id;
    }

    public Cliente(Integer id, String xnome, String xlgr, String nro, String xbairro, String cmun, String xmun, String uf, String cep) {
        this.id = id;
        this.xnome = xnome;
        this.xlgr = xlgr;
        this.nro = nro;
        this.xbairro = xbairro;
        this.cmun = cmun;
        this.xmun = xmun;
        this.uf = uf;
        this.cep = cep;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getXnome() {
        return xnome;
    }

    public void setXnome(String xnome) {
        this.xnome = xnome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getIe() {
        return ie;
    }

    public void setIe(String ie) {
        this.ie = ie;
    }

    public String getIndiedest() {
        return indiedest;
    }

    public void setIndiedest(String indiedest) {
        this.indiedest = indiedest;
    }

    public String getXlgr() {
        return xlgr;
    }

    public void setXlgr(String xlgr) {
        this.xlgr = xlgr;
    }

    public String getNro() {
        return nro;
    }

    public void setNro(String nro) {
        this.nro = nro;
    }

    public String getXcpl() {
        return xcpl;
    }

    public void setXcpl(String xcpl) {
        this.xcpl = xcpl;
    }

    public String getXbairro() {
        return xbairro;
    }

    public void setXbairro(String xbairro) {
        this.xbairro = xbairro;
    }

    public String getCmun() {
        return cmun;
    }

    public void setCmun(String cmun) {
        this.cmun = cmun;
    }

    public String getXmun() {
        return xmun;
    }

    public void setXmun(String xmun) {
        this.xmun = xmun;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getFone() {
        return fone;
    }

    public void setFone(String fone) {
        this.fone = fone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Date getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Date getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(Date dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    @XmlTransient
    public Collection<Venda> getVendaCollection() {
        return vendaCollection;
    }

    public void setVendaCollection(Collection<Venda> vendaCollection) {
        this.vendaCollection = vendaCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Cliente)) {
            return false;
        }
        Cliente other = (Cliente) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.souzamonteiro.nfe.model.Cliente[ id=" + id + " ]";
    }
    
    public String getDocumento() {
        if (cpf != null && !cpf.trim().isEmpty()) {
            return cpf;
        } else if (cnpj != null && !cnpj.trim().isEmpty()) {
            return cnpj;
        }
        return null;
    }
    
    public String getCpfFormatado() {
        if (cpf == null || cpf.trim().isEmpty()) {
            return "";
        }
        String cpfLimpo = cpf.replaceAll("\\D", "");
        if (cpfLimpo.length() == 11) {
            return cpfLimpo.substring(0, 3) + "." + 
                   cpfLimpo.substring(3, 6) + "." + 
                   cpfLimpo.substring(6, 9) + "-" + 
                   cpfLimpo.substring(9);
        }
        return cpf;
    }

    public String getCnpjFormatado() {
        if (cnpj == null || cnpj.trim().isEmpty()) {
            return "";
        }
        String cnpjLimpo = cnpj.replaceAll("\\D", "");
        if (cnpjLimpo.length() == 14) {
            return cnpjLimpo.substring(0, 2) + "." + 
                   cnpjLimpo.substring(2, 5) + "." + 
                   cnpjLimpo.substring(5, 8) + "/" + 
                   cnpjLimpo.substring(8, 12) + "-" + 
                   cnpjLimpo.substring(12);
        }
        return cnpj;
    }

    public String getCepFormatado() {
        if (cep == null || cep.trim().isEmpty()) {
            return "";
        }
        String cepLimpo = cep.replaceAll("\\D", "");
        if (cepLimpo.length() == 8) {
            return cepLimpo.substring(0, 5) + "-" + cepLimpo.substring(5);
        }
        return cep;
    }

    public String getTelefoneFormatado() {
        if (fone == null || fone.trim().isEmpty()) {
            return "";
        }
        String foneLimpo = fone.replaceAll("\\D", "");
        if (foneLimpo.length() == 10) {
            // Formato: (00) 0000-0000
            return "(" + foneLimpo.substring(0, 2) + ") " + 
                   foneLimpo.substring(2, 6) + "-" + 
                   foneLimpo.substring(6);
        } else if (foneLimpo.length() == 11) {
            // Formato: (00) 00000-0000
            return "(" + foneLimpo.substring(0, 2) + ") " + 
                   foneLimpo.substring(2, 7) + "-" + 
                   foneLimpo.substring(7);
        }
        return fone;
    }

    // Método para exibir documento formatado (CPF ou CNPJ)
    public String getDocumentoFormatado() {
        if (cpf != null && !cpf.trim().isEmpty()) {
            return getCpfFormatado();
        } else if (cnpj != null && !cnpj.trim().isEmpty()) {
            return getCnpjFormatado();
        }
        return "";
    }

    // Método para identificar tipo de documento
    public String getTipoDocumento() {
        if (cpf != null && !cpf.trim().isEmpty()) {
            return "F"; // Física
        } else if (cnpj != null && !cnpj.trim().isEmpty()) {
            return "J"; // Jurídica
        }
        return "";
    }
    
}
